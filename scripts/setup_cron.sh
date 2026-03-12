#!/usr/bin/env bash
# ============================================================
# setup_cron.sh — Instala el cron de resolución de incidencias
# ============================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ENV_FILE="$SCRIPT_DIR/.env"
RESOLVE_SCRIPT="$SCRIPT_DIR/resolve_incidencias.sh"
LOG_FILE=/var/log/atg_incidencias.log

echo "=== Setup del cron de resolución de incidencias ATG ==="

# ─── 1. Verificar dependencias ────────────────────────────────
echo ""
echo "▶ Verificando dependencias..."
MISSING=0
declare -A INSTALL_HINTS=(
  [jq]="sudo apt-get install -y jq  OR  brew install jq"
  [curl]="sudo apt-get install -y curl  OR  brew install curl"
  [git]="sudo apt-get install -y git  OR  brew install git"
  [gh]="https://cli.github.com/manual/installation"
  [claude]="npm install -g @anthropic-ai/claude-code"
)

for dep in jq curl git gh claude; do
  if command -v "$dep" &>/dev/null; then
    echo "  ✅ $dep → $(command -v "$dep")"
  else
    echo "  ❌ Falta $dep. Instala con: ${INSTALL_HINTS[$dep]}"
    MISSING=1
  fi
done

if [[ $MISSING -eq 1 ]]; then
  echo ""
  echo "ERROR: Instala las dependencias faltantes y vuelve a ejecutar este script."
  exit 1
fi

# ─── 2. Verificar .env ───────────────────────────────────────
echo ""
echo "▶ Verificando $ENV_FILE..."
if [[ ! -f "$ENV_FILE" ]]; then
  echo "ERROR: No se encontró $ENV_FILE"
  echo "  Copia el ejemplo: cp $SCRIPT_DIR/.env.example $ENV_FILE"
  echo "  Luego rellena los valores reales."
  exit 1
fi

# shellcheck source=/dev/null
source "$ENV_FILE"

REQUIRED_VARS=(KEYCLOAK_URL KEYCLOAK_REALM KEYCLOAK_CLIENT_ID KEYCLOAK_CLIENT_SECRET
               SERVICE_ACCOUNT_USER SERVICE_ACCOUNT_PASS API_BASE_URL ANTHROPIC_API_KEY
               CRON_SCHEDULE)
EMPTY_VARS=0
for var in "${REQUIRED_VARS[@]}"; do
  if [[ -z "${!var:-}" ]]; then
    echo "  ❌ Variable vacía: $var"
    EMPTY_VARS=1
  else
    echo "  ✅ $var = ${!var}"
  fi
done

if [[ $EMPTY_VARS -eq 1 ]]; then
  echo ""
  echo "ERROR: Rellena todas las variables en $ENV_FILE antes de continuar."
  exit 1
fi

# ─── 3. Hacer ejecutable el script principal ─────────────────
chmod +x "$RESOLVE_SCRIPT"
echo ""
echo "▶ $RESOLVE_SCRIPT marcado como ejecutable."

# ─── 4. Eliminar entrada anterior del cron si existe ─────────
echo ""
echo "▶ Actualizando crontab..."
CURRENT_CRON=$(crontab -l 2>/dev/null || true)
CLEAN_CRON=$(echo "$CURRENT_CRON" | grep -v "resolve_incidencias" || true)

# ─── 5. Añadir nueva entrada ──────────────────────────────────
NEW_CRON_ENTRY="${CRON_SCHEDULE} ${RESOLVE_SCRIPT} >> ${LOG_FILE} 2>&1"
if [[ -n "$CLEAN_CRON" ]]; then
  NEW_CRONTAB="${CLEAN_CRON}
${NEW_CRON_ENTRY}"
else
  NEW_CRONTAB="$NEW_CRON_ENTRY"
fi
echo "$NEW_CRONTAB" | crontab -

# ─── 6. Confirmar ─────────────────────────────────────────────
echo ""
echo "▶ Verificando crontab instalado:"
crontab -l | grep "resolve_incidencias"

echo ""
echo "✅ Cron configurado correctamente."
echo "   Próximas ejecuciones según: $CRON_SCHEDULE"
echo "   Logs en: $LOG_FILE"
echo ""
echo "Para ejecutar manualmente: bash $RESOLVE_SCRIPT"
