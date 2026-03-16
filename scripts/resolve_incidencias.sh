#!/usr/bin/env bash
# ============================================================
# resolve_incidencias.sh — Orquestador de resolución automática
# de incidencias ATG con Claude Code
# ============================================================
set -euo pipefail

# ─── PATH — incluir herramientas en ubicaciones no estándar de Windows ───────
# jq (Anaconda)
[[ -d "/c/Users/Manu/anaconda3/Library/mingw-w64/bin" ]] && \
  export PATH="/c/Users/Manu/anaconda3/Library/mingw-w64/bin:$PATH"
# claude (extensión VS Code)
CLAUDE_BIN=$(find "/c/Users/Manu/.vscode/extensions" -name "claude.exe" 2>/dev/null | head -1)
[[ -n "$CLAUDE_BIN" ]] && export PATH="$(dirname "$CLAUDE_BIN"):$PATH"
# gh (GitHub CLI — copiado junto a jq en Anaconda mingw-w64)
[[ -f "/c/Users/Manu/anaconda3/Library/mingw-w64/bin/gh.exe" ]] && \
  export PATH="/c/Users/Manu/anaconda3/Library/mingw-w64/bin:$PATH"

# ─── Lockfile (compatible con Linux y Git Bash/Windows) ──────
LOCKFILE=/tmp/atg_incidencias.lock
if [[ -f "$LOCKFILE" ]]; then
  echo "Ya hay una ejecución en curso (lockfile existe). Abortando."
  exit 1
fi
touch "$LOCKFILE"
trap 'rm -f "$LOCKFILE"' EXIT

# ─── Logging ─────────────────────────────────────────────────
LOG_FILE=/tmp/atg_incidencias.log
exec > >(tee -a "$LOG_FILE") 2>&1

log() { echo "[$(date '+%Y-%m-%dT%H:%M:%S')] $*"; }
log "=== Inicio de ejecución ==="

# ─── Cargar .env ─────────────────────────────────────────────
ENV_FILE="$(dirname "$0")/.env"
if [[ ! -f "$ENV_FILE" ]]; then
  log "ERROR: No se encontró $ENV_FILE. Copia .env.example y rellénalo."
  exit 1
fi
# shellcheck source=/dev/null
source "$ENV_FILE"

# ─── Verificar dependencias ───────────────────────────────────
MISSING=0
for dep in jq curl git gh claude; do
  if ! command -v "$dep" &>/dev/null; then
    log "ERROR: Falta dependencia: $dep"
    MISSING=1
  fi
done
[[ $MISSING -eq 1 ]] && exit 1

# ─── Contadores ───────────────────────────────────────────────
COUNT_OK=0
COUNT_SKIP=0
COUNT_NO_CHANGES=0
COUNT_ERROR=0

# ─────────────────────────────────────────────────────────────
# PASO 3.1 — Obtener JWT de Keycloak
# ─────────────────────────────────────────────────────────────
log "Obteniendo JWT de Keycloak..."
TOKEN_RESPONSE=$(curl -s -f -X POST \
  "${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=${KEYCLOAK_CLIENT_ID}" \
  -d "client_secret=${KEYCLOAK_CLIENT_SECRET}" \
  -d "username=${SERVICE_ACCOUNT_USER}" \
  -d "password=${SERVICE_ACCOUNT_PASS}")

JWT_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
if [[ -z "$JWT_TOKEN" || "$JWT_TOKEN" == "null" ]]; then
  log "ERROR: No se pudo obtener JWT. Respuesta: $TOKEN_RESPONSE"
  exit 1
fi
log "JWT obtenido."

# ─────────────────────────────────────────────────────────────
# PASO 3.2 — Obtener incidencias NUEVAS
# ─────────────────────────────────────────────────────────────
log "Consultando incidencias con estado=NUEVA..."
INCIDENCIAS_JSON=$(curl -s -f \
  "${API_BASE_URL}/auth/api/incidencias?estado=NUEVA&size=10&sort=prioridad,desc" \
  -H "Authorization: Bearer ${JWT_TOKEN}")

TOTAL=$(echo "$INCIDENCIAS_JSON" | jq '. | length')
if [[ "$TOTAL" -eq 0 ]]; then
  log "Sin incidencias nuevas. Fin."
  exit 0
fi
log "Encontradas $TOTAL incidencias nuevas."

# ─────────────────────────────────────────────────────────────
# PASO 3.3 — Directorio de trabajo (clon del repo)
# ─────────────────────────────────────────────────────────────
WORK_DIR=/tmp/atg_work
if [[ ! -d "$WORK_DIR/.git" ]]; then
  log "Clonando repo en $WORK_DIR..."
  git clone https://${GITHUB_TOKEN}@github.com/ManuHDdev/atg-app.git "$WORK_DIR"
else
  log "Actualizando repo en $WORK_DIR..."
  git -C "$WORK_DIR" fetch origin
  git -C "$WORK_DIR" checkout main
  git -C "$WORK_DIR" pull
fi

PROMPT_TEMPLATE="$(dirname "$0")/prompt_incidencia.txt"
if [[ ! -f "$PROMPT_TEMPLATE" ]]; then
  log "ERROR: No se encontró $PROMPT_TEMPLATE"
  exit 1
fi

# ─────────────────────────────────────────────────────────────
# PASO 3.4 — Loop por incidencia
# ─────────────────────────────────────────────────────────────
while IFS= read -r incidencia; do
  (
    INC_ID=$(echo "$incidencia" | jq -r '.id')
    INC_TITULO=$(echo "$incidencia" | jq -r '.titulo')
    INC_TIPO=$(echo "$incidencia" | jq -r '.tipo')
    INC_PRIORIDAD=$(echo "$incidencia" | jq -r '.prioridad')
    # El listado usa ResumenDto sin descripcion — hay que pedir el detalle
    INC_DETAIL=$(curl -s "${API_BASE_URL}/auth/api/incidencias/${INC_ID}" \
      -H "Authorization: Bearer ${JWT_TOKEN}")
    INC_DESCRIPCION=$(echo "$INC_DETAIL" | jq -r '.descripcion // ""')

    log "--- Procesando INC-${INC_ID}: ${INC_TITULO} [${INC_TIPO}/${INC_PRIORIDAD}]"

    # a) Verificar si ya existe PR abierta (GitHub REST API)
    EXISTING_PRS=$(curl -s \
      "https://api.github.com/repos/ManuHDdev/atg-app/pulls?state=open&head=ManuHDdev:feature/INC-${INC_ID}" \
      -H "Authorization: Bearer ${GITHUB_TOKEN}" \
      -H "Accept: application/vnd.github+json" | jq 'length')
    if [[ "$EXISTING_PRS" -gt 0 ]]; then
      log "INC-${INC_ID} ya tiene PR abierta. Saltando."
      exit 0
    fi

    # b) Cambiar estado a EN_REVISION
    curl -s -f -X PUT \
      "${API_BASE_URL}/auth/api/incidencias/${INC_ID}/estado" \
      -H "Authorization: Bearer ${JWT_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{"nuevoEstado":"EN_REVISION"}' > /dev/null
    log "INC-${INC_ID} → EN_REVISION"

    # c) Crear rama
    BRANCH_NAME="feature/INC-${INC_ID}-$(echo "$INC_TITULO" \
      | tr '[:upper:]' '[:lower:]' \
      | tr ' ' '-' \
      | tr -cd '[:alnum:]-' \
      | cut -c1-40)"
    git -C "$WORK_DIR" checkout -b "$BRANCH_NAME"

    # d) Construir prompt
    export INC_ID INC_TITULO INC_DESCRIPCION INC_TIPO INC_PRIORIDAD
    PROMPT=$(envsubst < "$PROMPT_TEMPLATE")

    # e) Ejecutar Claude Code
    log "Ejecutando Claude Code para INC-${INC_ID}..."
    cd "$WORK_DIR"
    claude -p "$PROMPT" --allowedTools "Edit,Write,Read,Bash" \
      2>&1 | tee "/tmp/atg_claude_inc_${INC_ID}.log"

    # f) Verificar si hay cambios
    CHANGED_FILES=$(git -C "$WORK_DIR" diff --name-only HEAD)
    if [[ -z "$CHANGED_FILES" ]]; then
      log "INC-${INC_ID}: Claude Code no aplicó cambios. Requiere revisión manual."
      curl -s -f -X PUT \
        "${API_BASE_URL}/auth/api/incidencias/${INC_ID}/estado" \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{\"nuevoEstado\":\"EN_REVISION\",\"notasDeveloper\":\"Claude Code: sin cambios aplicables automáticamente. Requiere revisión manual.\"}" \
        > /dev/null
      git -C "$WORK_DIR" checkout main
      git -C "$WORK_DIR" branch -D "$BRANCH_NAME"
      exit 0
    fi

    # g) Commit y push
    git -C "$WORK_DIR" add -A
    git -C "$WORK_DIR" commit -m "fix(INC-${INC_ID}): ${INC_TITULO} [automated]"
    git -C "$WORK_DIR" push origin "$BRANCH_NAME"

    # h) Crear PR (GitHub REST API)
    PR_BODY="## Incidencia #${INC_ID}\n**Tipo:** ${INC_TIPO} | **Prioridad:** ${INC_PRIORIDAD}\n\n**Descripción:**\n${INC_DESCRIPCION}\n\n---\n*PR generada automáticamente por Claude Code*"
    PR_RESPONSE=$(curl -s -X POST \
      "https://api.github.com/repos/ManuHDdev/atg-app/pulls" \
      -H "Authorization: Bearer ${GITHUB_TOKEN}" \
      -H "Accept: application/vnd.github+json" \
      -H "Content-Type: application/json" \
      -d "{\"title\":\"fix(INC-${INC_ID}): ${INC_TITULO}\",\"body\":\"${PR_BODY}\",\"head\":\"${BRANCH_NAME}\",\"base\":\"main\"}")
    PR_URL=$(echo "$PR_RESPONSE" | jq -r '.html_url')
    log "INC-${INC_ID}: PR creada → $PR_URL"

    # i) Actualizar estado a EN_DESARROLLO con URL de la PR
    curl -s -f -X PUT \
      "${API_BASE_URL}/auth/api/incidencias/${INC_ID}/estado" \
      -H "Authorization: Bearer ${JWT_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{\"nuevoEstado\":\"EN_DESARROLLO\",\"notasDeveloper\":\"PR automática: ${PR_URL}\"}" \
      > /dev/null
    log "INC-${INC_ID} → EN_DESARROLLO"

    # j) Volver a main y limpiar rama local
    git -C "$WORK_DIR" checkout main
    git -C "$WORK_DIR" branch -D "$BRANCH_NAME"

  ) && COUNT_OK=$((COUNT_OK + 1)) || {
    log "ERROR procesando INC-${INC_ID}. Continuando con la siguiente."
    COUNT_ERROR=$((COUNT_ERROR + 1))
    git -C "$WORK_DIR" checkout main 2>/dev/null || true
  }

done < <(echo "$INCIDENCIAS_JSON" | jq -c '.[]')

# ─────────────────────────────────────────────────────────────
# PASO 3.5 — Resumen final
# ─────────────────────────────────────────────────────────────
log "=== Resumen ==="
log "  OK (PR creada):      $COUNT_OK"
log "  Sin cambios:         $COUNT_NO_CHANGES"
log "  Saltadas (PR exist): $COUNT_SKIP"
log "  Errores:             $COUNT_ERROR"
log "=== Fin de ejecución ==="
