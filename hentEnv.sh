#!/bin/bash
# Henter Azure-variabler med nais-cli og skriver til .env for docker-compose (texas)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TEAM=teamfamilie
MILJO=dev-gcp
APP=familie-ba-sak
BEGRUNNELSE="Lokal kjoering av familie-ba-sak med texas"

if [[ "$(nais device status)" != *"Connected"* ]]; then
  echo "Naisdevice er ikke tilkoblet. Start naisdevice og velg connect. Status må være grønn."
  exit 1
fi

# nais-cli skriver feilmeldinger til stdout, så vi fanger dem og viser dem videre.
if ! APP_ENV=$(nais app env "$APP" -e "$MILJO" -t "$TEAM" -o json 2>&1); then
  printf '%s\n' "Klarte ikke hente miljøvariabler for $APP:" "$APP_ENV"
  exit 1
fi

AZURE_SECRET=$(printf '%s\n' "$APP_ENV" |
  jq -r '.[] | select(.name == "AZURE_APP_CLIENT_SECRET") | .source.name' | head -1)

if [[ -z "$AZURE_SECRET" ]]; then
  echo "Fant ikke azure-secreten til $APP. Er du logget inn med 'nais login -y'?"
  exit 1
fi

if ! SECRET_JSON=$(nais secret get "$AZURE_SECRET" -e "$MILJO" -t "$TEAM" \
  --with-values --reason "$BEGRUNNELSE" -o json 2>&1); then
  printf '%s\n' "Klarte ikke hente secreten $AZURE_SECRET:" "$SECRET_JSON"
  exit 1
fi

AZURE_KV=$(printf '%s\n' "$SECRET_JSON" | jq -r '.data[] | "\(.key)=\(.value)"')

UTDATA=""
MANGLER=""
for NOKKEL in AZURE_APP_CLIENT_ID AZURE_APP_CLIENT_SECRET AZURE_APP_TENANT_ID AZURE_APP_JWK; do
  LINJE=$(printf '%s\n' "$AZURE_KV" | grep "^$NOKKEL=" | head -1)
  if [[ -z "$LINJE" ]]; then
    MANGLER="$MANGLER $NOKKEL"
  else
    UTDATA="$UTDATA$LINJE"$'\n'
  fi
done

# Alle fire må være med – docker-compose interpolerer dem inn i texas-URLene,
# og en manglende verdi gir stille feil som https://login.microsoftonline.com//v2.0
if [[ -n "$MANGLER" ]]; then
  echo "Manglet følgende nøkler i $AZURE_SECRET:$MANGLER"
  exit 1
fi

printf '%s' "$UTDATA" > "$SCRIPT_DIR/.env"

echo ".env opprettet med Azure-variabler ✓"
