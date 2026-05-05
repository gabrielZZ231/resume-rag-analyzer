#!/bin/bash

# Pasta onde as chaves devem ficar
JWT_DIR="src/main/resources/jwt"

# Cria a pasta se não existir
mkdir -p "$JWT_DIR"

# Verifica se a chave privada já existe para não sobrescrever
if [ ! -f "$JWT_DIR/privateKeyPkcs8.pem" ]; then
    echo "🔑 Gerando chaves JWT para ambiente local..."
    
    # Gerar chave privada original
    openssl genrsa -out "$JWT_DIR/privateKey.pem" 2048
    
    # Converter para PKCS8 (exigido pelo SmallRye/Quarkus)
    openssl pkcs8 -topk8 -inform PEM -outform PEM -in "$JWT_DIR/privateKey.pem" -out "$JWT_DIR/privateKeyPkcs8.pem" -nocrypt
    
    # Gerar chave pública
    openssl rsa -in "$JWT_DIR/privateKey.pem" -pubout -outform PEM -out "$JWT_DIR/publicKey.pem"
    
    # Ajustar permissões para que o usuário do Docker consiga ler
    chmod 644 "$JWT_DIR"/*.pem
    
    echo "✅ Chaves geradas com sucesso em $JWT_DIR"
else
    echo "🛡️ Chaves JWT já existem. Pulando geração."
fi
