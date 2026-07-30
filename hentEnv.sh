#!/bin/bash
# Henter Azure-variabler fra Kubernetes og skriver til .env for docker-compose (texas)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Sjekk naisdevice
NAIS_STATUS=$(nais device status)
if [[ "$NAIS_STATUS" != *"Connected"* ]]; then
  echo "Naisdevice er ikke tilkoblet. Start naisdevice og velg connect. Status må være grønn."
  exit 1
fi

# Sjekk gcloud
if ! gcloud auth print-identity-token > /dev/null 2>&1; then
  echo "Ikke logget inn på gcloud. Kjør 'nais login'"
  exit 1
fi

kubectl config use-context dev-gcp

AZURE_SECRET=$(kubectl -n teamfamilie get secret -o name | grep "azure-familie-ba-sak" | grep -v "frontend" | head -1 | cut -d/ -f2)

if [[ -z "$AZURE_SECRET" ]]; then
  echo "Fant ikke Azure-secret i Kubernetes."
  exit 1
fi

AZURE_VARIABLER=$(kubectl -n teamfamilie get secret "$AZURE_SECRET" -o json | jq '.data | map_values(@base64d)')

cat > "$SCRIPT_DIR/.env" <<EOF
AZURE_APP_CLIENT_ID=$(echo "$AZURE_VARIABLER" | jq -r '.AZURE_APP_CLIENT_ID')
AZURE_APP_CLIENT_SECRET=$(echo "$AZURE_VARIABLER" | jq -r '.AZURE_APP_CLIENT_SECRET')
AZURE_APP_TENANT_ID=$(echo "$AZURE_VARIABLER" | jq -r '.AZURE_APP_TENANT_ID')
AZURE_APP_JWK=$(echo "$AZURE_VARIABLER" | jq -r '.AZURE_APP_JWK')
EOF

echo ".env opprettet med Azure-variabler ✓"
