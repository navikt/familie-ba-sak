export CREDENTIAL_PASSWORD=$(cat /var/run/secrets/srvfamilie-ba-sak/password)
export CREDENTIAL_USERNAME=$(cat /var/run/secrets/srvfamilie-ba-sak/username)
echo "- exported CREDENTIAL_USERNAME og CREDENTIAL_PASSWORD for familie-ba-sak "
export JAVA_OPTS='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=0.0.0.0:8089'
