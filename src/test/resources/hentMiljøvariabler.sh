 # Check the status of nais device
 NAIS_STATUS=$(nais device status)

 if [[ "$NAIS_STATUS" != *"Connected"* ]]; then
   echo "Naisdevice er ikke tilkoblet. Start naisdevice og velg connect. Status må være grønn."
   exit 1
 fi

 # Check the status of gcloud auth print-identity-token
 if ! gcloud auth print-identity-token > /dev/null 2>&1; then
   echo "Ikke logget inn på gcloud. Kjør nais login"
   exit 1
 fi

kubectl config use-context dev-gcp
AZURE_SECRET=$(kubectl -n teamfamilie get secrets | grep azure-familie-ba-sak | grep -v "frontend" |  sed 's/^\([a-zA-Z0-9-]*\).*/\1/'| head -n 1);

PODVARIABLER="$(kubectl -n teamfamilie get secret "$AZURE_SECRET" -o json | jq '.data | map_values(@base64d)')"
UNLEASH_VARIABLER="$(kubectl -n teamfamilie get secret familie-ba-sak-unleash-api-token -o json | jq '.data | map_values(@base64d)')"

AZURE_APP_CLIENT_ID="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_ID" | sed 's/:/=/1' | tr -d '",'| tr -d ' "')"
AZURE_APP_CLIENT_SECRET="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_SECRET" | sed 's/:/=/1' | tr -d '",'| tr -d ' "')"

UNLEASH_SERVER_API_URL="$(echo "$UNLEASH_VARIABLER" | grep "UNLEASH_SERVER_API_URL" | sed 's/:/=/1' | tr -d ' "')"
UNLEASH_SERVER_API_TOKEN="$(echo "$UNLEASH_VARIABLER" | grep "UNLEASH_SERVER_API_TOKEN" | sed 's/:/=/1' | tr -d ' ,"')"

if [ -z "$AZURE_APP_CLIENT_ID" ]
then
      return 1
else
      printf "%s;%s;%s;%s" "$AZURE_APP_CLIENT_ID" "$AZURE_APP_CLIENT_SECRET" "$UNLEASH_SERVER_API_URL" "$UNLEASH_SERVER_API_TOKEN"
fi