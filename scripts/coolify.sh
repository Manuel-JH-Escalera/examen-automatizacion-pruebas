#!/usr/bin/env bash
# =============================================================================
# Utilidades para desplegar en Coolify desde el pipeline (GitHub Actions).
#
# Coolify es la plataforma PaaS que corre en el VPS del ambiente de pruebas.
# La aplicación está configurada en Coolify como "Docker Image", por lo que
# desplegar una versión = cambiar el tag de la imagen + lanzar un deploy, y
# hacer rollback = volver a apuntar al tag anterior + lanzar un deploy.
#
# Variables de entorno requeridas:
#   COOLIFY_URL       ej: https://coolify.midominio.cl
#   COOLIFY_TOKEN     token de API (Coolify > Keys & Tokens > API tokens)
#   COOLIFY_APP_UUID  uuid de la aplicación en Coolify
#
# Uso:
#   scripts/coolify.sh current-tag                 -> imprime el tag desplegado
#   scripts/coolify.sh deploy <tag>                -> despliega el tag y espera
#   scripts/coolify.sh wait-version <url> <commit> -> espera a que /api/info reporte el commit
# =============================================================================
set -euo pipefail

API="${COOLIFY_URL%/}/api/v1"
AUTH=(-H "Authorization: Bearer ${COOLIFY_TOKEN}" -H "Accept: application/json")

# ---- helpers ---------------------------------------------------------------
api_get()   { curl -fsS "${AUTH[@]}" "${API}$1"; }
api_patch() { curl -fsS "${AUTH[@]}" -H "Content-Type: application/json" -X PATCH "${API}$1" -d "$2"; }

log() { echo "[coolify] $*"; }

# ---- comandos --------------------------------------------------------------

# Tag de imagen actualmente configurado en la aplicación (versión desplegada).
current_tag() {
  api_get "/applications/${COOLIFY_APP_UUID}" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("docker_registry_image_tag",""))'
}

# Cambia el tag de la imagen, dispara el despliegue y espera a que termine.
deploy() {
  local tag="$1"
  log "Configurando la aplicación ${COOLIFY_APP_UUID} con el tag '${tag}'"
  api_patch "/applications/${COOLIFY_APP_UUID}" "{\"docker_registry_image_tag\":\"${tag}\"}" > /dev/null

  log "Lanzando despliegue..."
  local resp deployment_uuid
  resp=$(api_get "/deploy?uuid=${COOLIFY_APP_UUID}&force=true")
  deployment_uuid=$(echo "$resp" | python3 -c 'import sys,json; d=json.load(sys.stdin); print(d["deployments"][0]["deployment_uuid"])')
  log "Deployment uuid: ${deployment_uuid}"

  # Espera hasta 10 minutos a que Coolify termine el despliegue
  local status="" i
  for i in $(seq 1 120); do
    status=$(api_get "/deployments/${deployment_uuid}" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("status",""))')
    log "Estado del despliegue: ${status}"
    case "$status" in
      finished) log "Despliegue completado"; return 0 ;;
      failed|cancelled*) log "El despliegue falló"; return 1 ;;
    esac
    sleep 5
  done
  log "Tiempo de espera agotado"
  return 1
}

# Espera a que la aplicación publicada responda con el commit esperado en /api/info.
wait_version() {
  local url="${1%/}" commit="$2" i actual
  for i in $(seq 1 60); do
    actual=$(curl -fsS "${url}/api/info" 2>/dev/null | python3 -c 'import sys,json; print(json.load(sys.stdin).get("commit",""))' 2>/dev/null || true)
    log "Commit activo en ${url}: '${actual:-sin respuesta}' (esperado '${commit}')"
    if [ "$actual" = "$commit" ]; then
      log "La versión esperada está activa"
      return 0
    fi
    sleep 5
  done
  log "La aplicación no llegó a la versión esperada"
  return 1
}

case "${1:-}" in
  current-tag)  current_tag ;;
  deploy)       deploy "$2" ;;
  wait-version) wait_version "$2" "$3" ;;
  *) echo "Uso: $0 {current-tag|deploy <tag>|wait-version <url> <commit>}"; exit 2 ;;
esac
