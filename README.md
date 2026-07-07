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
2. Certifique-se de ter o **JDK 17+** instalado.
3. Abra o projeto no **Android Studio** ou **IntelliJ IDEA**.
4. Compile o servidor e as plataformas:
   - Para rodar o servidor: `./gradlew :serverApp:run`
   - Para rodar o app Android: `./gradlew :androidApp:installDebug`
   - Para rodar o app Desktop: `./gradlew :desktopApp:run`

Consulte a seção `19. Guias Rápidos de Onboarding` no [Plano Técnico](docs/PTT_KMP_PLANO_TECNICO.md) para detalhes sobre as regras de arquitetura em KMP (como lidar com dependências inversas) e o padrão MVI utilizado.

## 📖 Documentação Adicional
- **ADRs**: As decisões tomadas ao longo do projeto estão documentadas em `docs/adr/`.
- **API Reference**: Acesse a documentação Dokka gerada executando `./gradlew dokkaHtmlMultiModule`. Os arquivos estarão em `build/dokka/htmlMultiModule`.
