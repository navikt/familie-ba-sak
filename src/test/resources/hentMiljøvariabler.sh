#!/bin/bash
# Henter miljøvariabler for DevLauncherPostgresPreprod med nais-cli.
# NB: DevLauncherPostgresPreprod forkaster første linje på stdout – behold statuslinja før printf.

TEAM=teamfamilie
MILJO=dev-gcp
APP=familie-ba-sak
BEGRUNNELSE="Lokal kjoering av familie-ba-sak mot preprod"

if [[ "$(nais device status)" != *"Connected"* ]]; then
  echo "Naisdevice er ikke tilkoblet. Start naisdevice og velg connect. Status må være grønn."
  exit 1
fi

# nais-cli skriver feilmeldinger til stdout, så vi fanger dem og viser dem videre.
# Appens miljøvariabler hentes én gang og gjenbrukes for alle oppslagene under.
if ! APP_ENV=$(nais app env "$APP" -e "$MILJO" -t "$TEAM" -o json 2>&1); then
  printf '%s\n' "Klarte ikke hente miljøvariabler for $APP:" "$APP_ENV"
  exit 1
fi

finn_secret() { # $1 = nøkkel -> setter FUNNET_SECRET
  FUNNET_SECRET=$(printf '%s\n' "$APP_ENV" | jq -r --arg k "$1" '.[] | select(.name == $k) | .source.name' | head -1)
  if [[ -z "$FUNNET_SECRET" ]]; then
    printf '%s\n' "Fant ikke secreten bak $1 for $APP. Er du logget inn med 'nais login -y'?"
    return 1
  fi
}

hent_secret_kv() { # $1 = secret-navn -> setter SECRET_KV
  local ut
  if ! ut=$(nais secret get "$1" -e "$MILJO" -t "$TEAM" --with-values --reason "$BEGRUNNELSE" -o json 2>&1); then
    printf '%s\n' "Klarte ikke hente secreten $1:" "$ut"
    return 1
  fi
  SECRET_KV=$(printf '%s\n' "$ut" | jq -r '.data[] | "\(.key)=\(.value)"')
}

finn_secret AZURE_APP_CLIENT_SECRET || exit 1
AZURE_SECRET=$FUNNET_SECRET
finn_secret TOKEN_X_CLIENT_ID || exit 1
TOKEN_X_SECRET=$FUNNET_SECRET
finn_secret UNLEASH_SERVER_API_TOKEN || exit 1
UNLEASH_SECRET=$FUNNET_SECRET

hent_secret_kv "$AZURE_SECRET" || exit 1
AZURE_KV=$SECRET_KV
hent_secret_kv "$TOKEN_X_SECRET" || exit 1
TOKEN_X_KV=$SECRET_KV
hent_secret_kv "$UNLEASH_SECRET" || exit 1
UNLEASH_KV=$SECRET_KV

UTDATA=""
MANGLER=""
legg_til() { # $1 = KEY=VALUE-blokk, $2 = nøkkel
  local linje
  linje=$(printf '%s\n' "$1" | grep "^$2=" | head -1)
  if [[ -z "$linje" ]]; then
    MANGLER="$MANGLER $2"
  else
    UTDATA="$UTDATA$linje"$'\n'
  fi
}

legg_til "$AZURE_KV" AZURE_APP_CLIENT_ID
legg_til "$AZURE_KV" AZURE_APP_CLIENT_SECRET
legg_til "$AZURE_KV" AZURE_OPENID_CONFIG_ISSUER
legg_til "$AZURE_KV" AZURE_OPENID_CONFIG_JWKS_URI
legg_til "$AZURE_KV" AZURE_APP_TENANT_ID
legg_til "$TOKEN_X_KV" TOKEN_X_CLIENT_ID
legg_til "$TOKEN_X_KV" TOKEN_X_ISSUER
legg_til "$TOKEN_X_KV" TOKEN_X_JWKS_URI
legg_til "$UNLEASH_KV" UNLEASH_SERVER_API_URL
legg_til "$UNLEASH_KV" UNLEASH_SERVER_API_TOKEN
legg_til "$AZURE_KV" AZURE_APP_JWK

if [[ -n "$MANGLER" ]]; then
  printf '%s\n' "Manglet følgende nøkler i secretene:$MANGLER"
  exit 1
fi

echo "Henter miljøvariabler med nais-cli..."
printf '%s' "$UTDATA"
