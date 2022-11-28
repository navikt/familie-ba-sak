kubectl config use-context dev-gcp
PODNAVN=$(kubectl -n teamfamilie get pods -o name | grep familie-ba-sak | grep -v "frontend" |  sed "s/^.\{4\}//" | head -n 1);

PODVARIABLER="$(kubectl -n teamfamilie exec -c familie-ba-sak -it "$PODNAVN" -- env)"
AZURE_APP_CLIENT_ID="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_ID" | tr -d '\r' )"
AZURE_APP_CLIENT_SECRET="$(echo "$PODVARIABLER" | grep "AZURE_APP_CLIENT_SECRET" | tr -d '\r' )";

if [ -z "$AZURE_APP_CLIENT_ID" ]
then
      return 1
else
      printf "%s;%s" "$AZURE_APP_CLIENT_ID" "$AZURE_APP_CLIENT_SECRET" #| pbcopy
fi