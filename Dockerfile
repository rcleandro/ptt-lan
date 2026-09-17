# Estágio 1: Build
FROM eclipse-temurin:21-jdk-jammy AS builder
WORKDIR /app
COPY . .
# Constrói a distribuição do servidor (isso já baixa as dependências e gera o binário)
RUN chmod +x ./gradlew
RUN ./gradlew :serverApp:installDist --no-daemon

# Estágio 2: Execução
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copia apenas a distribuição final do estágio de build
COPY --from=builder /app/serverApp/build/install/serverApp ./

# Expõe a porta HTTPS configurada no Ktor (o conector HTTP em claro foi desligado)
EXPOSE 9443

# Configuração por ambiente (todas opcionais; ver README):
# PTT_ADMIN_PASSWORD     senha do painel /admin; sem ela as rotas de escrita ficam desligadas
# PTT_JWT_SECRET         segredo dos tokens; sem ele cada restart invalida as sessões
# PTT_KEYSTORE_PASSWORD  senha do keystore TLS (padrão "password")
# PTT_TRUST_PROXY        "true" só atrás de proxy reverso, para o rate limit usar X-Forwarded-For
ENV PTT_ADMIN_PASSWORD= \
    PTT_JWT_SECRET= \
    PTT_KEYSTORE_PASSWORD= \
    PTT_TRUST_PROXY=

# Comando para iniciar o servidor
ENTRYPOINT ["./bin/serverApp"]
