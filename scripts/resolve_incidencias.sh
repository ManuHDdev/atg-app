#!/usr/bin/env bash
# ============================================================
# resolve_incidencias.sh — Orquestador de resolución automática
# de incidencias ATG con Claude Code
# ============================================================
set -euo pipefail

# ─── Lockfile ────────────────────────────────────────────────
LOCKFILE=/tmp/atg_incidencias.lock
exec 200>"$LOCKFILE"
flock -n 200 || { echo "[$(date -Is)] Ya hay una ejecución en curso. Abortando."; exit 1; }

# ─── Logging ─────────────────────────────────────────────────
LOG_FILE=/var/log/atg_incidencias.log
mkdir -p "$(dirname "$LOG_FILE")"
exec > >(tee -a "$LOG_FILE") 2>&1

log() { echo "[$(date -Is)] $*"; }
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
  "${API_BASE_URL}/api/incidencias?estado=NUEVA&size=10&sort=prioridad,desc" \
  -H "Authorization: Bearer ${JWT_TOKEN}")

TOTAL=$(echo "$INCIDENCIAS_JSON" | jq '.content | length')
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
  git clone git@github.com:ManuHDdev/atg-app.git "$WORK_DIR"
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
    INC_DESCRIPCION=$(echo "$incidencia" | jq -r '.descripcion')
    INC_TIPO=$(echo "$incidencia" | jq -r '.tipo')
    INC_PRIORIDAD=$(echo "$incidencia" | jq -r '.prioridad')

    log "--- Procesando INC-${INC_ID}: ${INC_TITULO} [${INC_TIPO}/${INC_PRIORIDAD}]"

    # a) Verificar si ya existe PR abierta
    EXISTING_PRS=$(gh pr list \
      --repo ManuHDdev/atg-app \
      --search "INC-${INC_ID}" \
      --state open \
      --json number | jq length)
    if [[ "$EXISTING_PRS" -gt 0 ]]; then
      log "INC-${INC_ID} ya tiene PR abierta. Saltando."
      exit 0
    fi

    # b) Cambiar estado a EN_REVISION
    curl -s -f -X PATCH \
      "${API_BASE_URL}/api/incidencias/${INC_ID}/estado" \
      -H "Authorization: Bearer ${JWT_TOKEN}" \
      -H "Content-Type: application/json" \
      -d '{"estado":"EN_REVISION"}' > /dev/null
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
      curl -s -f -X PATCH \
        "${API_BASE_URL}/api/incidencias/${INC_ID}/estado" \
        -H "Authorization: Bearer ${JWT_TOKEN}" \
        -H "Content-Type: application/json" \
        -d "{\"estado\":\"EN_REVISION\",\"notasDeveloper\":\"Claude Code: sin cambios aplicables automáticamente. Requiere revisión manual.\"}" \
        > /dev/null
      git -C "$WORK_DIR" checkout main
      git -C "$WORK_DIR" branch -D "$BRANCH_NAME"
      exit 0
    fi

    # g) Commit y push
    git -C "$WORK_DIR" add -A
    git -C "$WORK_DIR" commit -m "fix(INC-${INC_ID}): ${INC_TITULO} [automated]"
    git -C "$WORK_DIR" push origin "$BRANCH_NAME"

    # h) Crear PR
    PR_URL=$(gh pr create \
      --repo ManuHDdev/atg-app \
      --title "fix(INC-${INC_ID}): ${INC_TITULO}" \
      --body "## Incidencia #${INC_ID}
**Tipo:** ${INC_TIPO} | **Prioridad:** ${INC_PRIORIDAD}

**Descripción:**
${INC_DESCRIPCION}

---
*PR generada automáticamente por Claude Code*
Ver log completo: /tmp/atg_claude_inc_${INC_ID}.log" \
      --base main \
      --head "$BRANCH_NAME")
    log "INC-${INC_ID}: PR creada → $PR_URL"

    # i) Actualizar estado a EN_DESARROLLO con URL de la PR
    curl -s -f -X PATCH \
      "${API_BASE_URL}/api/incidencias/${INC_ID}/estado" \
      -H "Authorization: Bearer ${JWT_TOKEN}" \
      -H "Content-Type: application/json" \
      -d "{\"estado\":\"EN_DESARROLLO\",\"notasDeveloper\":\"PR automática: ${PR_URL}\"}" \
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

done < <(echo "$INCIDENCIAS_JSON" | jq -c '.content[]')

# ─────────────────────────────────────────────────────────────
# PASO 3.5 — Resumen final
# ─────────────────────────────────────────────────────────────
log "=== Resumen ==="
log "  OK (PR creada):      $COUNT_OK"
log "  Sin cambios:         $COUNT_NO_CHANGES"
log "  Saltadas (PR exist): $COUNT_SKIP"
log "  Errores:             $COUNT_ERROR"
log "=== Fin de ejecución ==="
