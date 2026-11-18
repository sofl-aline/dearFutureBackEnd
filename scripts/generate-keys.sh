#!/usr/bin/env sh
set -eu
umask 077
mkdir -p secrets
if [ -e secrets/private.pem ] || [ -e secrets/public.pem ]; then
  echo "Já existem chaves. Nada foi sobrescrito."
  exit 1
fi
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out secrets/private.pem
openssl pkey -in secrets/private.pem -pubout -out secrets/public.pem
echo "Chaves locais criadas em secrets/. Não adicionar ao Git."
