#!/usr/bin/env bash
#
# Starter en felles lokal Postgres-database for Team Familie.
#
# Selve databaseoppsettet (docker-compose.yaml + init-multiple-databases.sh) er delt
# og bor i https://github.com/navikt/familie slik at det er identisk for alle appene.
# Dette scriptet henter/oppdaterer det oppsettet automatisk og starter databasen,
# slik at du slipper å klone familie-repoet manuelt.
#
# Krav:
#   * SSH-tilgang til navikt/familie
#   * En kjørende container-motor: Docker Desktop, Colima (colima start) eller Podman (podman machine start)
#
# Miljøvariabler (valgfritt):
#   FAMILIE_DB_REF=<branch>   Hent oppsettet fra en spesifikk git-ref (nyttig for å teste en PR før merge).

set -euo pipefail

REPO_URL="git@github.com:navikt/familie.git"
REF="${FAMILIE_DB_REF:-}"
# Delt cache-katalog slik at alle familie-appene bruker samme compose-fil, project-navn og volum.
CACHE_DIR="${XDG_CACHE_HOME:-$HOME/.cache}/team-familie/familie"

# 1. Hent eller oppdater det delte oppsettet
if [ -d "$CACHE_DIR/.git" ]; then
    TARGET_REF="${REF:-$(git -C "$CACHE_DIR" rev-parse --abbrev-ref HEAD)}"
    echo "→ Oppdaterer felles DB-oppsett ($TARGET_REF) i $CACHE_DIR ..."
    git -C "$CACHE_DIR" fetch --depth 1 origin "$TARGET_REF"
    git -C "$CACHE_DIR" reset --hard FETCH_HEAD
else
    echo "→ Henter felles DB-oppsett til $CACHE_DIR ..."
    mkdir -p "$(dirname "$CACHE_DIR")"
    if [ -n "$REF" ]; then
        git clone --depth 1 --branch "$REF" "$REPO_URL" "$CACHE_DIR"
    else
        git clone --depth 1 "$REPO_URL" "$CACHE_DIR"
    fi
fi

# 1b. Sjekk at compose-fila faktisk finnes i det delte oppsettet
if [ ! -f "$CACHE_DIR/docker-compose.yaml" ]; then
    echo "✗ Fant ikke docker-compose.yaml i det delte oppsettet (ref: ${REF:-default branch})." >&2
    echo "  Er PR-en som legger til DB-oppsettet i navikt/familie merget?" >&2
    echo "  Test evt. mot en branch:  FAMILIE_DB_REF=<branch> ./startLokalDatabase.sh" >&2
    exit 1
fi

# 2. Finn riktig compose-kommando (docker eller podman)
if docker compose version >/dev/null 2>&1; then
    COMPOSE=(docker compose)
elif command -v podman >/dev/null 2>&1 && podman compose version >/dev/null 2>&1; then
    COMPOSE=(podman compose)
else
    echo "✗ Fant ikke 'docker compose' eller 'podman compose'." >&2
    echo "  Start Docker Desktop / Colima (colima start) / Podman (podman machine start) og prøv igjen." >&2
    exit 1
fi

# 3. Start databasen. Vi cd-er inn i katalogen slik at den relative monteringen
#    av init-scriptet (./init-multiple-databases.sh) og project-navnet blir riktig.
echo "→ Starter postgres ..."
( cd "$CACHE_DIR" && "${COMPOSE[@]}" up -d postgres )

echo
echo "✓ Ferdig! Databasene (inkl. 'familie-ba-sak') kjører på localhost:5432 – bruker/passord: postgres/test"
echo "  Stopp:   ( cd \"$CACHE_DIR\" && ${COMPOSE[*]} stop postgres )"
echo "  Logger:  ( cd \"$CACHE_DIR\" && ${COMPOSE[*]} logs -f postgres )"

