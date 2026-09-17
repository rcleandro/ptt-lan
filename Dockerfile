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

# Senha do painel admin (Basic auth). Sem ela, as rotas de escrita do /admin ficam desligadas.
ENV PTT_ADMIN_PASSWORD=

# Comando para iniciar o servidor
ENTRYPOINT ["./bin/serverApp"]
