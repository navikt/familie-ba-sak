# Check the status of nais device
NAIS_STATUS=$(nais device status)

if [[ "$NAIS_STATUS" != *"Connected"* ]]; then
  echo "Naisdevice er ikke tilkoblet. Start naisdevice og velg connect. Status må være grønn."
  return 1
fi

# Check the status of gcloud auth print-identity-token
if ! gcloud auth print-identity-token > /dev/null 2>&1; then
  echo "Ikke logget inn på gcloud. Kjør nais login"
  return 1
fi

kubectl config use-context dev-gcp

AZURE_SECRET=$(kubectl -n teamfamilie get secret -o name | grep "azure-familie-ba-sak" | grep -v "frontend" | head -1 | cut -d/ -f2)
TOKEN_X_SECRET=$(kubectl -n teamfamilie get secret -o name | grep "tokenx-familie-ba-sak" | head -1 | cut -d/ -f2)

AZURE_VARIABLER="$(kubectl -n teamfamilie get secret "$AZURE_SECRET" -o json | jq '.data | map_values(@base64d)')"
TOKEN_X_VARIABLER="$(kubectl -n teamfamilie get secret "$TOKEN_X_SECRET" -o json | jq '.data | map_values(@base64d)')"
UNLEASH_VARIABLER="$(kubectl -n teamfamilie get secret familie-ba-sak-unleash-api-token -o json | jq '.data | map_values(@base64d)')"

_jq() { echo "$1" | jq -r ".\"$2\""; }

AZURE_APP_CLIENT_ID="$(_jq "$AZURE_VARIABLER" "AZURE_APP_CLIENT_ID")"
AZURE_APP_CLIENT_SECRET="$(_jq "$AZURE_VARIABLER" "AZURE_APP_CLIENT_SECRET")"
AZURE_OPENID_CONFIG_ISSUER="$(_jq "$AZURE_VARIABLER" "AZURE_OPENID_CONFIG_ISSUER")"
AZURE_OPENID_CONFIG_JWKS_URI="$(_jq "$AZURE_VARIABLER" "AZURE_OPENID_CONFIG_JWKS_URI")"

TOKEN_X_CLIENT_ID="$(_jq "$TOKEN_X_VARIABLER" "TOKEN_X_CLIENT_ID")"
TOKEN_X_ISSUER="$(_jq "$TOKEN_X_VARIABLER" "TOKEN_X_ISSUER")"
TOKEN_X_JWKS_URI="$(_jq "$TOKEN_X_VARIABLER" "TOKEN_X_JWKS_URI")"

UNLEASH_SERVER_API_URL="$(_jq "$UNLEASH_VARIABLER" "UNLEASH_SERVER_API_URL")"
UNLEASH_SERVER_API_TOKEN="$(_jq "$UNLEASH_VARIABLER" "UNLEASH_SERVER_API_TOKEN")"

if [ -z "$AZURE_APP_CLIENT_ID" ]; then
  return 1
fi

printf "%s;%s;%s;%s;%s;%s;%s;%s;%s" \
  "$AZURE_APP_CLIENT_ID" "$AZURE_APP_CLIENT_SECRET" \
  "$AZURE_OPENID_CONFIG_ISSUER" "$AZURE_OPENID_CONFIG_JWKS_URI" \
  "$TOKEN_X_CLIENT_ID" "$TOKEN_X_ISSUER" "$TOKEN_X_JWKS_URI" \
  "$UNLEASH_SERVER_API_URL" "$UNLEASH_SERVER_API_TOKEN"