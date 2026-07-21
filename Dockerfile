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

# Expõe as portas configuradas no Ktor (9393 para HTTP e 9443 para HTTPS)
EXPOSE 9393 9443

# Comando para iniciar o servidor
ENTRYPOINT ["./bin/serverApp"]
