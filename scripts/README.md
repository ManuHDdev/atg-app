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

---

## audit_seguridad.sh

Auditoría de seguridad en 4 fases: análisis estático automático → auditoría profunda
con Claude Code → commit + PR de las correcciones → resumen en terminal.

### Prerequisitos

- Mismas dependencias que `resolve_incidencias.sh`
- `pip3` disponible (opcional, para instalar Semgrep si no está)
- `npm` disponible (para npm audit del frontend)

### Ejecución

```bash
bash scripts/audit_seguridad.sh
```

### Qué hace

**Fase 1 — Análisis estático automático**
- Instala Semgrep si no está disponible e inspecciona todos los microservicios
  con los rulesets `p/java`, `p/spring-boot` y `p/owasp-top-ten`
- Ejecuta `npm audit` sobre el frontend y captura vulnerabilidades
- Busca patrones peligrosos con grep: secretos hardcodeados, SQL concatenado,
  controllers sin `@PreAuthorize`, `permitAll()`, métodos de escritura sin
  `@Transactional`

**Fase 2 — Auditoría profunda con Claude Code**
- Consolida los resultados del análisis estático
- Lanza Claude Code con acceso a todo el código para revisar configuraciones
  de seguridad, controllers, servicios, repositorios y environments
- Corrige vulnerabilidades críticas/altas/medias directamente en el código
- Genera informe en `/tmp/atg_audit_resultado.md`

**Fase 3 — Commit y PR**
- Si Claude aplicó correcciones: crea rama `audit/security-YYYYMMDD-HHMM`,
  commit, push y PR en GitHub con el informe como cuerpo
- Si no hay correcciones: muestra el informe en terminal

**Fase 4 — Resumen**
- Muestra rutas de logs, URL de PR (si se creó) y lista de pendientes
  que requieren atención manual

### Salidas

| Fichero | Contenido |
|---|---|
| `/tmp/atg_audit_resultado.md` | Informe completo de la auditoría |
| `/tmp/atg_audit_semgrep.json` | Raw output de Semgrep |
| `/tmp/atg_audit_npm.json` | Raw output de npm audit |
| `/tmp/atg_audit_grep.txt` | Resultados de búsquedas grep |
| `/var/log/atg_audit.log` | Log de ejecución del script |
| `/tmp/atg_claude_audit.log` | Log completo de Claude Code |
