#!/usr/bin/env bash
# Rehearse the platform stack on your laptop with only the published images (no build).
# Local Docker Compose does not understand the platform's ${secret:NAME}, so this script turns
# each one into ${NAME} and reads the values from the root .env (never commit that file).
#   demo/rehearse.sh          start, then open http://localhost:8080
#   demo/rehearse.sh down     stop and remove the containers
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"
local_compose="$(mktemp --suffix=.yaml)"
trap 'rm -f "$local_compose"' EXIT
sed 's/\${secret:\([A-Z0-9_]*\)}/${\1}/g' "$root/demo/compose.yaml" > "$local_compose"
docker compose --project-name tender-demo --project-directory "$root/demo" \
  --env-file "$root/.env" -f "$local_compose" -f "$root/demo/rehearse.yaml" "${@:-up}"
