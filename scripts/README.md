# Scripts de automatización — atg-app

## resolve_incidencias.sh

Procesa las incidencias en estado `NUEVA`, las asigna a Claude Code y abre PRs automáticamente.

### Prerequisitos

- Tener `scripts/.env` relleno (copia de `.env.example`)
- Tener instalado: `jq`, `curl`, `git`, `gh` (GitHub CLI), `claude` (Claude Code CLI)
- Estar autenticado en gh: `gh auth login`

### Ejecución

```bash
bash scripts/resolve_incidencias.sh
```

### Qué hace

1. Obtiene JWT de Keycloak con la cuenta `claude-bot`
2. Consulta incidencias en estado `NUEVA` ordenadas por prioridad (máx. 10)
3. Por cada incidencia:
   - Comprueba que no haya ya una PR abierta para ella
   - Marca la incidencia como `EN_REVISION`
   - Crea rama `feature/INC-{id}-titulo`
   - Ejecuta Claude Code con el prompt de `prompt_incidencia.txt`
   - Si Claude aplicó cambios → commit + push + abre PR en GitHub → marca `EN_DESARROLLO`
   - Si Claude no aplicó cambios → deja en `EN_REVISION` con nota de revisión manual
4. Escribe log en `/var/log/atg_incidencias.log`

### Variables de entorno requeridas (scripts/.env)

| Variable | Descripción |
|---|---|
| `KEYCLOAK_URL` | URL base de Keycloak (ej: `https://manuhd.duckdns.org/auth`) |
| `KEYCLOAK_REALM` | Realm de Keycloak (`atg`) |
| `KEYCLOAK_CLIENT_ID` | Client ID de la cuenta de servicio |
| `KEYCLOAK_CLIENT_SECRET` | Client secret |
| `SERVICE_ACCOUNT_USER` | Usuario Keycloak del bot (`claude-bot`) |
| `SERVICE_ACCOUNT_PASS` | Contraseña del bot |
| `API_BASE_URL` | URL base de la API (ej: `https://manuhd.duckdns.org`) |
| `ANTHROPIC_API_KEY` | API key de Anthropic para Claude Code |
