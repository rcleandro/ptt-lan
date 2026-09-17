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

# Configuração por ambiente, passada no `docker run -e` (todas opcionais; ver README).
# Não são declaradas com ENV: isso as gravaria na imagem e, por serem definidas com valor vazio,
# o `${?VAR}` do application.conf sobrescreveria o padrão do keystore com uma senha em branco.
# PTT_ADMIN_PASSWORD     senha do painel /admin; sem ela as rotas de escrita ficam desligadas
# PTT_JWT_SECRET         segredo dos tokens; sem ele cada restart invalida as sessões
# PTT_KEYSTORE_PASSWORD  senha do keystore TLS (padrão "password")
# PTT_TRUST_PROXY        "true" só atrás de proxy reverso, para o rate limit usar X-Forwarded-For

# Comando para iniciar o servidor
ENTRYPOINT ["./bin/serverApp"]
