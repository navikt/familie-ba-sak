#!/bin/bash
# Starter Texas via docker-compose. Henter .env først hvis den ikke finnes.

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

if [ ! -f "$SCRIPT_DIR/.env" ]; then
  echo ".env finnes ikke, henter variabler fra Kubernetes..."
  "$SCRIPT_DIR/hentEnv.sh" || exit 1
fi

docker compose --profile dev up -d
