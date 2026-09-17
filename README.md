# PTT-LAN

Bem-vindo ao repositório do **PTT-LAN**, um aplicativo Push-To-Talk multi-plataforma focado em baixa latência e comunicação local (LAN) usando Kotlin Multiplatform (KMP).

## 📚 Fonte Única de Verdade (SSOT)

O projeto é guiado **estritamente** pelo nosso Plano Técnico, que serve como Fonte Única de Verdade (SSOT) para todas as decisões arquiteturais, roadmaps, design system e especificações.

👉 **Leia o documento principal antes de contribuir:**  
[docs/PTT_KMP_PLANO_TECNICO.md](docs/PTT_KMP_PLANO_TECNICO.md)

Nenhum desenvolvedor ou agente de IA deve introduzir novas tecnologias ou mudar a arquitetura se não estiver explícito no Plano Técnico ou aprovado via um ADR (Architecture Decision Record) na pasta `docs/adr/`.

## 🛠️ Tecnologias Principais

- **Kotlin Multiplatform (KMP)**: Lógica de domínio compartilhada para Android, iOS e Desktop.
- **Compose Multiplatform**: UI compartilhada usando a mesma base de código declarativa.
- **Ktor**: `serverApp` (Floor Control, Broadcast WebSocket) e Client (Networking).
- **SQLDelight**: Persistência local (Histórico e Configurações).
- **Decompose**: Navegação e arquitetura MVI em todos os módulos de feature.
- **Opus Codec**: Alta qualidade e baixa latência (via `kopus`).
- **Android Automotive OS**: Suporte nativo ao volante.

## 🚀 Como iniciar

1. Clone o repositório.
2. Tenha o **JDK 21** instalado (o mesmo usado pelo CI e pela imagem Docker).
3. Abra o projeto no **Android Studio** ou **IntelliJ IDEA**.
4. Compile e rode:
   - Servidor: `./gradlew :serverApp:run` — HTTPS/WSS na porta **9443** (único conector; o HTTP em claro foi desligado). Na primeira execução é gerado um certificado self-signed em `serverApp/build/keystore.jks`.
   - Painel admin: `https://localhost:9443/admin`. Defina `PTT_ADMIN_PASSWORD` para protegê-lo com Basic auth (usuário `admin`); sem a variável, as rotas de escrita (`restart`, `shutdown`, `broadcast`, `kick`, `delete`) ficam desligadas.
   - Android: `./gradlew :androidApp:installDebug`
   - Desktop: `./gradlew :desktopApp:run`
   - iOS: `cd iosApp && xcodegen`, depois abra `iosApp.xcodeproj` no Xcode.
5. Variáveis de ambiente do servidor (todas opcionais; os padrões servem para LAN):

   | Variável | Padrão | Para que serve |
   | --- | --- | --- |
   | `PTT_ADMIN_PASSWORD` | — | Senha do painel `/admin` (usuário `admin`). Sem ela, as rotas de escrita do painel ficam desligadas. |
   | `PTT_JWT_SECRET` | chave aleatória por boot | Mantém os tokens válidos entre reinícios e entre instâncias. Sem ela, todo restart desloga os clientes. |
   | `PTT_KEYSTORE_PASSWORD` | `password` | Senha do keystore TLS, usada tanto para gerar `build/keystore.jks` quanto para lê-lo. |
   | `PTT_TRUST_PROXY` | `false` | Ligue **apenas** atrás de um proxy reverso: faz o servidor usar `X-Forwarded-For` como IP do cliente no rate limit. Em LAN, ligada, permitiria a qualquer cliente forjar o próprio IP. |

6. Testes e qualidade: `./gradlew jvmTest :serverApp:test detekt ktlintCheck`. (Nos módulos KMP, `./gradlew test` não executa os testes `jvmTest`.)

### Servidor via Docker

```bash
docker build -t ptt-lan-server .
docker run -p 9443:9443 \
  -e PTT_ADMIN_PASSWORD=troque-isto \
  -e PTT_JWT_SECRET=troque-isto \
  -e PTT_KEYSTORE_PASSWORD=troque-isto \
  ptt-lan-server
```

A cada push na `main`, o CI publica a imagem multi-arquitetura (amd64/arm64) em `ghcr.io/rcleandro/ptt-lan`.

Consulte a seção `19. Guias Rápidos de Onboarding` no [Plano Técnico](docs/PTT_KMP_PLANO_TECNICO.md) para detalhes sobre as regras de arquitetura em KMP (como lidar com dependências inversas) e o padrão MVI utilizado.

## 📖 Documentação Adicional
- **Contexto do projeto**: [docs/CONTEXTO.md](docs/CONTEXTO.md) descreve o código como ele está hoje e onde diverge do plano.
- **Roadmap de melhorias**: [docs/ROADMAP_MELHORIAS.md](docs/ROADMAP_MELHORIAS.md) (fases 18 em diante).
- **ADRs**: As decisões tomadas ao longo do projeto estão documentadas em `docs/adr/`.
- **API Reference**: Acesse a documentação Dokka gerada executando `./gradlew dokkaHtmlMultiModule`. Os arquivos estarão em `build/dokka/htmlMultiModule`.
