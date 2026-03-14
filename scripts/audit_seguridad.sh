#!/usr/bin/env bash
# ============================================================
# audit_seguridad.sh — Auditoría de seguridad automatizada
# Fase 1: análisis estático (Semgrep, npm audit, grep)
# Fase 2: auditoría profunda con Claude Code
# Fase 3: commit + PR si hay correcciones
# Fase 4: resumen en terminal
# ============================================================
set -uo pipefail

# ─── Lockfile (solo en Linux donde flock está disponible) ────
LOCKFILE=/tmp/atg_audit.lock
if command -v flock &>/dev/null; then
  exec 201>"$LOCKFILE"
  flock -n 201 || { echo "[$(date -Is)] Ya hay una auditoría en curso. Abortando."; exit 1; }
fi

# ─── Rutas ───────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
LOG_FILE=/var/log/atg_audit.log
SEMGREP_OUT=/tmp/atg_audit_semgrep.json
NPM_OUT=/tmp/atg_audit_npm.json
GREP_OUT=/tmp/atg_audit_grep.txt
PROMPT_FILE=/tmp/atg_audit_prompt.txt
RESULTADO_OUT=/tmp/atg_audit_resultado.md

# ─── Logging ─────────────────────────────────────────────────
mkdir -p "$(dirname "$LOG_FILE")"
exec > >(tee -a "$LOG_FILE") 2>&1

log() { echo "[$(date -Is)] $*"; }
log "=== Inicio de auditoría de seguridad ==="
log "Repo: $REPO_DIR"

# ─── Cargar .env ─────────────────────────────────────────────
ENV_FILE="$SCRIPT_DIR/.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck source=/dev/null
  source "$ENV_FILE"
  log ".env cargado."
else
  log "AVISO: No se encontró $ENV_FILE. Continuando sin vars de entorno."
fi

# ─── Dependencias obligatorias ────────────────────────────────
for dep in jq curl git gh claude; do
  if ! command -v "$dep" &>/dev/null; then
    log "ERROR: Falta dependencia obligatoria: $dep"
    exit 1
  fi
done

# ─── Inicializar ficheros de salida ───────────────────────────
echo '{"results":[]}' > "$SEMGREP_OUT"
echo '{}'             > "$NPM_OUT"
> "$GREP_OUT"
> "$RESULTADO_OUT"

PR_URL=""
FASE_ERROR=0

# ════════════════════════════════════════════════════════════════
# FASE 1 — Análisis estático automático
# ════════════════════════════════════════════════════════════════
log "FASE 1 — Análisis estático automático"

# ─── 1.1 Semgrep ─────────────────────────────────────────────
log "FASE 1.1 — Semgrep..."
{
  if ! command -v semgrep &>/dev/null; then
    log "Semgrep no encontrado. Intentando instalar con pip3..."
    if command -v pip3 &>/dev/null; then
      pip3 install semgrep -q && log "Semgrep instalado." || {
        log "AVISO: No se pudo instalar Semgrep. Se omite este análisis."
        FASE_ERROR=$((FASE_ERROR + 1))
      }
    else
      log "AVISO: pip3 no disponible. Se omite análisis Semgrep."
      FASE_ERROR=$((FASE_ERROR + 1))
    fi
  fi

  if command -v semgrep &>/dev/null; then
    log "Detectando microservicios (pom.xml)..."
    mapfile -t SVC_DIRS < <(
      find "$REPO_DIR" -name "pom.xml" \
        -not -path "*/target/*" \
        -not -path "*/node_modules/*" \
        | xargs -I{} dirname {} \
        | grep -v "^${REPO_DIR}$"
    )

    if [[ ${#SVC_DIRS[@]} -gt 0 ]]; then
      log "Ejecutando Semgrep sobre ${#SVC_DIRS[@]} módulos..."
      semgrep \
        --config "p/java" \
        --config "p/spring-boot" \
        --config "p/owasp-top-ten" \
        --json \
        --output "$SEMGREP_OUT" \
        "${SVC_DIRS[@]}" 2>/dev/null \
        && log "Semgrep completado." \
        || log "AVISO: Semgrep terminó con findings (normal)."
    else
      log "AVISO: No se encontraron directorios con pom.xml."
    fi
  fi
} || { log "AVISO: Fase 1.1 (Semgrep) falló. Continuando."; FASE_ERROR=$((FASE_ERROR + 1)); }

# ─── 1.2 npm audit ───────────────────────────────────────────
log "FASE 1.2 — npm audit..."
{
  if [[ -d "$REPO_DIR/frontend" ]]; then
    (cd "$REPO_DIR/frontend" && npm audit --json > "$NPM_OUT" 2>&1) \
      && log "npm audit completado sin vulnerabilidades." \
      || log "AVISO: npm audit encontró vulnerabilidades (capturadas en $NPM_OUT)."
  else
    log "AVISO: Directorio frontend/ no encontrado. Se omite npm audit."
  fi
} || { log "AVISO: Fase 1.2 (npm audit) falló. Continuando."; FASE_ERROR=$((FASE_ERROR + 1)); }

# ─── 1.3 Grep de patrones peligrosos ─────────────────────────
log "FASE 1.3 — Patrones peligrosos con grep..."
{
  {
    echo "=== Passwords hardcodeados ==="
    grep -rPn 'password\s*=\s*['"'"'"][^\$\{]' \
      --include="*.java" --include="*.ts" --include="*.yml" \
      "$REPO_DIR" 2>/dev/null || echo "(ninguno)"

    echo ""
    echo "=== Tokens/secrets en código ==="
    grep -rPn 'secret\s*=\s*['"'"'"]' \
      --include="*.java" --include="*.ts" \
      "$REPO_DIR" 2>/dev/null || echo "(ninguno)"

    echo ""
    echo "=== TODO/FIXME de seguridad ==="
    grep -rni 'TODO.*security\|FIXME.*auth\|HACK' \
      --include="*.java" --include="*.ts" \
      "$REPO_DIR" 2>/dev/null || echo "(ninguno)"

    echo ""
    echo "=== @RestController sin @PreAuthorize ==="
    while IFS= read -r f; do
      if grep -q "@RestController" "$f" && ! grep -q "@PreAuthorize" "$f"; then
        echo "$f — sin @PreAuthorize en ningún método"
      fi
    done < <(find "$REPO_DIR" -name "*Controller.java" -not -path "*/target/*")
    echo "(fin controllers)"

    echo ""
    echo "=== permitAll() en configuración de seguridad ==="
    grep -rn "permitAll" \
      --include="*.java" \
      "$REPO_DIR" 2>/dev/null || echo "(ninguno)"

    echo ""
    echo "=== SQL concatenado (posible injection) ==="
    grep -rPn '"SELECT[^"]*"\s*\+|"INSERT[^"]*"\s*\+|"UPDATE[^"]*"\s*\+' \
      --include="*.java" \
      "$REPO_DIR" 2>/dev/null || echo "(ninguno)"

    echo ""
    echo "=== Métodos de escritura en ServiceImpl posiblemente sin @Transactional ==="
    while IFS= read -r f; do
      while IFS= read -r match; do
        lineno=$(echo "$match" | cut -d: -f1)
        startline=$((lineno > 4 ? lineno - 4 : 1))
        context=$(sed -n "${startline},${lineno}p" "$f" 2>/dev/null)
        if ! echo "$context" | grep -q "@Transactional"; then
          echo "$f:$lineno — posible ausencia de @Transactional: $(echo "$match" | cut -d: -f2-)"
        fi
      done < <(grep -nP 'public\s+\S+\s+(save|delete|update|crear|actualizar|eliminar)\b' "$f" 2>/dev/null)
    done < <(find "$REPO_DIR" -name "*ServiceImpl.java" -not -path "*/target/*")
    echo "(fin servicios)"

  } >> "$GREP_OUT" 2>/dev/null
  log "Grep completado. Resultados en $GREP_OUT"
} || { log "AVISO: Fase 1.3 (grep) falló parcialmente. Continuando."; FASE_ERROR=$((FASE_ERROR + 1)); }

# ════════════════════════════════════════════════════════════════
# FASE 2 — Auditoría profunda con Claude Code
# ════════════════════════════════════════════════════════════════
log "FASE 2 — Auditoría profunda con Claude Code..."

# Extraer findings Semgrep (máx 50 líneas)
SEMGREP_FINDINGS=$(jq -r '
  if (.results | length) > 0 then
    .results[] |
    "  [" + (.extra.severity // "?") + "] "
      + .path + ":"
      + ((.start.line // 0) | tostring)
      + " — " + (.extra.message // "sin mensaje")
  else "  (sin findings o Semgrep no ejecutado)"
  end
' "$SEMGREP_OUT" 2>/dev/null | head -50 \
  || echo "  (no disponible)")

# Extraer vulnerabilidades high/critical de npm
NPM_FINDINGS=$(jq -r '
  if .vulnerabilities then
    .vulnerabilities | to_entries[]
    | select(.value.severity == "high" or .value.severity == "critical")
    | "  [" + .value.severity + "] " + .key + " — "
      + (.value.via[0]
         | if type == "string" then .
           else (.title // "desconocido")
           end)
  else "  (sin vulnerabilidades high/critical o npm audit no ejecutado)"
  end
' "$NPM_OUT" 2>/dev/null | head -30 \
  || echo "  (no disponible)")

GREP_CONTENT=$(cat "$GREP_OUT" 2>/dev/null || echo "(no disponible)")

# Construir prompt en fichero temporal para evitar problemas de quoting
cat > "$PROMPT_FILE" << 'ENDOFPROMPT'
Eres un auditor de seguridad senior. Estás en el repositorio atg-app.
Lee CLAUDE.md primero para entender el stack y las convenciones.

Se han ejecutado herramientas de análisis estático con los resultados que aparecen
más abajo.

ÁREAS A AUDITAR MANUALMENTE (lee estos ficheros con Glob/Read):
1. Todos los ficheros en */config/ (seguridad, CORS, Keycloak)
2. Todos los *Controller.java (autorización por endpoint)
3. Todos los *ServiceImpl.java (transacciones, validaciones de negocio)
4. Todos los *Repository.java (queries personalizadas)
5. application.yml y application-dev.yml de cada microservicio
6. Ficheros de environment Angular (environment*.ts)
7. docker-compose.yml y infra/docker-compose.prod.yml (puertos, env vars)

PARA CADA VULNERABILIDAD QUE ENCUENTRES:

A) CRÍTICA o ALTA (inyección SQL, autenticación bypasseable, secretos en código,
   CORS abierto a *, endpoints sin autorización):
   - Corrígela directamente en el fichero afectado
   - Añade un test que verifique la corrección si es razonable
   - Documéntala en /tmp/atg_audit_resultado.md bajo ## CORREGIDAS

B) MEDIA (falta @Transactional en escrituras, validación débil, log de datos
   sensibles, falta de @PreAuthorize en endpoints no públicos):
   - Corrígela si el cambio es pequeño (1-10 líneas)
   - Si requiere refactoring mayor: documéntala en ## PENDIENTES con descripción
     detallada de cómo resolverla

C) BAJA o INFORMATIVA (mejoras de configuración, deprecaciones):
   - Solo documéntala en ## INFORMATIVAS

CHECKLIST OBLIGATORIO — revisa cada punto:
□ Todos los @RestController tienen @PreAuthorize o están protegidos en SecurityFilterChain
□ No hay secretos hardcodeados (passwords, tokens, API keys) en código fuente
□ Los endpoints de Actuator están protegidos o limitados
□ CORS no permite * en producción
□ Los DTOs tienen validaciones Jakarta (@NotNull, @Size, @NotBlank...)
□ Los servicios que escriben en BD tienen @Transactional
□ No hay queries SQL construidas por concatenación de strings
□ Los logs no imprimen datos sensibles (passwords, tokens, PII)
□ Las excepciones no exponen stack traces al cliente (GlobalExceptionHandler)
□ Los ficheros application-dev.yml no tienen credenciales reales
□ El frontend no tiene API keys ni tokens hardcodeados
□ Las imágenes Docker no corren como root

AL FINALIZAR, escribe el informe completo en /tmp/atg_audit_resultado.md con
este formato exacto:

# Informe de auditoría de seguridad — atg-app
Fecha: [fecha actual]

## Resumen ejecutivo
- Vulnerabilidades críticas/altas corregidas: N
- Vulnerabilidades medias corregidas: N
- Pendientes de revisión manual: N
- Informativas: N

## CORREGIDAS
### [CRITICIDAD] [Fichero] — [Descripción breve]
**Problema:** ...
**Corrección aplicada:** ...
**Test añadido:** sí/no

## PENDIENTES
### [CRITICIDAD] [Fichero] — [Descripción breve]
**Problema:** ...
**Cómo resolverlo:** ...

## INFORMATIVAS
- [lista]

NO hagas git commit (el script lo gestiona).
NO modifiques CLAUDE.md, docker-compose.yml ni ficheros de configuración de Keycloak.

━━━━━━━━━━━━━━━━━━━━━━━━━
RESULTADOS DEL ANÁLISIS ESTÁTICO
━━━━━━━━━━━━━━━━━━━━━━━━━

ENDOFPROMPT

# Añadir resultados dinámicos al prompt
{
  echo "SEMGREP FINDINGS:"
  echo "$SEMGREP_FINDINGS"
  echo ""
  echo "NPM AUDIT (high/critical):"
  echo "$NPM_FINDINGS"
  echo ""
  echo "PATRONES PELIGROSOS DETECTADOS:"
  echo "$GREP_CONTENT"
} >> "$PROMPT_FILE"

log "Ejecutando Claude Code para auditoría profunda..."
cd "$REPO_DIR"
claude -p "$(cat "$PROMPT_FILE")" \
  --allowedTools "Edit,Write,Read,Bash,Glob,Grep" \
  2>&1 | tee /tmp/atg_claude_audit.log

log "Claude Code finalizado."

# ════════════════════════════════════════════════════════════════
# FASE 3 — Commit y PR si hay cambios
# ════════════════════════════════════════════════════════════════
log "FASE 3 — Verificando cambios..."

cd "$REPO_DIR"
CHANGED_COUNT=$(git status --porcelain | grep -cv "^??" || true)

if [[ "$CHANGED_COUNT" -gt 0 ]]; then
  log "Cambios detectados ($CHANGED_COUNT ficheros). Creando rama y PR..."
  git status --short | grep -v "^??" | awk '{print "  - "$2}' | while read -r line; do log "$line"; done

  AUDIT_BRANCH="audit/security-$(date +%Y%m%d-%H%M)"
  git checkout -b "$AUDIT_BRANCH"
  git add -A
  git commit -m "security: auditoría automática de seguridad $(date +%Y-%m-%d)"
  git push origin "$AUDIT_BRANCH"

  PR_BODY="$(cat "$RESULTADO_OUT" 2>/dev/null || echo 'Ver /tmp/atg_audit_resultado.md para el informe completo.')"
  PR_URL=$(gh pr create \
    --repo ManuHDdev/atg-app \
    --title "security: auditoría automática $(date +%Y-%m-%d)" \
    --body "$PR_BODY" \
    --base main \
    --head "$AUDIT_BRANCH") \
    && log "PR creada: $PR_URL" \
    || log "AVISO: No se pudo crear la PR automáticamente."

  git checkout main
else
  log "Sin cambios aplicados automáticamente."
fi

# ════════════════════════════════════════════════════════════════
# FASE 4 — Resumen en terminal
# ════════════════════════════════════════════════════════════════
log "FASE 4 — Resumen final"

echo ""
echo "════════════════════════════════════════════════════════"
echo "  AUDITORÍA DE SEGURIDAD — RESUMEN"
echo "════════════════════════════════════════════════════════"
echo "  Informe completo : $RESULTADO_OUT"
echo "  Log completo     : $LOG_FILE"
echo "  Log Claude Code  : /tmp/atg_claude_audit.log"
[[ -n "$PR_URL" ]] && echo "  PR creada        : $PR_URL"
[[ "$FASE_ERROR" -gt 0 ]] && echo "  Fases con avisos : $FASE_ERROR (ver log)"
echo "────────────────────────────────────────────────────────"

if [[ -f "$RESULTADO_OUT" ]]; then
  if grep -q "^## PENDIENTES" "$RESULTADO_OUT" 2>/dev/null; then
    echo ""
    echo "  ⚠  PENDIENTES DE ATENCIÓN MANUAL:"
    awk '/^## PENDIENTES/{found=1; next} found && /^## [A-Z]/{exit} found && /^### /{print "    →", substr($0,5)}' "$RESULTADO_OUT"
  fi
  echo ""
  echo "════════════════════════════════════════════════════════"
  echo ""
  cat "$RESULTADO_OUT"
fi

log "=== Auditoría finalizada ==="
