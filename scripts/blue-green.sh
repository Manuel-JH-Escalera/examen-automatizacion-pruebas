#!/usr/bin/env bash
# =============================================================================
# Despliegue Blue-Green del ambiente de pruebas (Docker).
#
# Topología:
#   [router (nginx) :8080] --> app-blue:8080  (versión actual / estable)
#                          \-> app-green:8080 (versión nueva candidata)
#
# El router expone un único punto de entrada (http://localhost:8080). Cambiar
# de versión ("switch") solo reescribe el upstream de nginx y lo recarga, por lo
# que el cambio es instantáneo y reversible: el rollback es volver a "blue".
#
# Uso:
#   scripts/blue-green.sh up-router
#   scripts/blue-green.sh start <blue|green> <imagen> <commit>
#   scripts/blue-green.sh wait-healthy <blue|green>
#   scripts/blue-green.sh switch <blue|green>
#   scripts/blue-green.sh active            -> slot que recibe el tráfico
#   scripts/blue-green.sh info <blue|green> -> /api/info del contenedor directo
#   scripts/blue-green.sh down
# =============================================================================
set -euo pipefail

RED="pruebas"
ROUTER="router"
CONF_DIR="${BG_CONF_DIR:-$PWD/.blue-green}"
PUERTO_PUBLICO="${BG_PORT:-8080}"

log() { echo "[blue-green] $*"; }

escribir_conf() {
  local slot="$1"
  mkdir -p "$CONF_DIR"
  cat > "$CONF_DIR/default.conf" <<EOF
# Generado por scripts/blue-green.sh - slot activo: ${slot}
upstream app_activa {
    server app-${slot}:8080;
}
server {
    listen 80;
    location / {
        proxy_pass http://app_activa;
        proxy_set_header Host \$host;
        add_header X-Deploy-Slot "${slot}" always;
    }
}
EOF
}

up_router() {
  docker network inspect "$RED" >/dev/null 2>&1 || docker network create "$RED" >/dev/null
  escribir_conf "blue"
  docker rm -f "$ROUTER" >/dev/null 2>&1 || true
  docker run -d --name "$ROUTER" --network "$RED" -p "${PUERTO_PUBLICO}:80" \
    -v "$CONF_DIR:/etc/nginx/conf.d:ro" nginx:alpine >/dev/null
  log "Router nginx levantado en http://localhost:${PUERTO_PUBLICO} (slot inicial: blue)"
}

start() {
  local slot="$1" imagen="$2" commit="$3"
  docker rm -f "app-${slot}" >/dev/null 2>&1 || true
  docker run -d --name "app-${slot}" --network "$RED" \
    -e APP_SLOT="$slot" -e APP_COMMIT="$commit" "$imagen" >/dev/null
  log "Contenedor app-${slot} iniciado con imagen ${imagen} (commit ${commit})"
}

wait_healthy() {
  local slot="$1" i estado
  for i in $(seq 1 45); do
    estado=$(docker inspect --format '{{.State.Health.Status}}' "app-${slot}" 2>/dev/null || echo "desconocido")
    if [ "$estado" = "healthy" ]; then
      log "app-${slot} está healthy"
      return 0
    fi
    sleep 2
  done
  log "app-${slot} no llegó a healthy (estado: ${estado})"
  docker logs "app-${slot}" | tail -30
  return 1
}

switch() {
  local slot="$1"
  escribir_conf "$slot"
  docker exec "$ROUTER" nginx -s reload
  sleep 1
  log "Tráfico enrutado a app-${slot}"
}

active() {
  curl -fsS -D - -o /dev/null "http://localhost:${PUERTO_PUBLICO}/api/info" \
    | tr -d '\r' | awk -F': ' 'tolower($1)=="x-deploy-slot"{print $2}'
}

info() {
  local slot="$1"
  docker exec "app-${slot}" curl -fsS http://localhost:8080/api/info
}

down() {
  docker rm -f "$ROUTER" app-blue app-green >/dev/null 2>&1 || true
  docker network rm "$RED" >/dev/null 2>&1 || true
  log "Ambiente de pruebas eliminado"
}

case "${1:-}" in
  up-router)    up_router ;;
  start)        start "$2" "$3" "$4" ;;
  wait-healthy) wait_healthy "$2" ;;
  switch)       switch "$2" ;;
  active)       active ;;
  info)         info "$2" ;;
  down)         down ;;
  *) echo "Uso: $0 {up-router|start <slot> <imagen> <commit>|wait-healthy <slot>|switch <slot>|active|info <slot>|down}"; exit 2 ;;
esac
