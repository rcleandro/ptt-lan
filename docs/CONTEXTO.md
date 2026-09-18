# PTT-LAN — Contexto do Projeto

> Retrato do **código como ele está hoje** (atualizado com o design Liquid Glass, a fase 18 e a fase 19.1).
> O plano/roadmap oficial continua em [`PTT_KMP_PLANO_TECNICO.md`](PTT_KMP_PLANO_TECNICO.md) (SSOT)
> e as decisões em [`adr/`](adr/). Onde o código diverge do plano, a seção
> [Divergências](#8-divergências-entre-plano-e-código) registra o que vale na prática.

---

## 1. O que é

App **push-to-talk** (walkie-talkie) em **Kotlin Multiplatform**. Um processo `serverApp` (Ktor/JVM)
faz o relay de áudio e o floor control ("um fala por vez"); clientes Android, iOS e Desktop
compartilham UI (Compose Multiplatform), navegação, domínio, rede e parte do pipeline de áudio.

Nasceu para LAN (descoberta via mDNS, sem internet) e evoluiu (fases 15–17) para aceitar também
servidor em domínio público, com login JWT e rate limiting. É também um projeto de estudo/portfólio
de KMP — por isso a arquitetura é mais "cerimoniosa" do que o tamanho exigiria (~9,4 mil linhas de Kotlin).

Todas as 17 fases do roadmap estão marcadas como concluídas no plano.

## 2. Stack

| Área | Tecnologia (versão em `gradle/libs.versions.toml`) |
|---|---|
| Linguagem / build | Kotlin 2.4.20, Gradle 9.7.1, AGP 9.4.0, JVM target 17, daemon toolchain JDK 21 |
| Android | `compileSdk`/`targetSdk` 37, `minSdk` 26 |
| Targets KMP | `android`, `jvm`, `iosArm64`, `iosSimulatorArm64` |
| UI | Compose Multiplatform 1.12.0 + Material 3, fontes IBM Plex Sans/Mono |
| Navegação / estado | Decompose 3.5.0 (child stack) + MVI manual por tela |
| DI | Koin 4.2.2 |
| Rede | Ktor 3.6.0 — client OkHttp (Android/JVM) e Darwin (iOS); server Netty |
| Serialização | kotlinx.serialization (JSON) |
| Persistência | SQLDelight 2.3.2, multiplatform-settings 1.3.0, Okio (arquivos) |
| Áudio | APIs nativas por plataforma + Opus via `kopus` 1.6.1.3 |
| Descoberta | NSD (Android), Bonjour (iOS), JmDNS (JVM/servidor) |
| Servidor extra | ktor-server-auth-jwt, ktor-server-rate-limit, ktor-server-forwarded-header, Logback |
| Qualidade | Detekt 2.0.0-alpha.6, ktlint-gradle (motor 1.8.0), Kover, Dokka, MockK, Turbine |

## 3. Mapa de módulos

```
androidApp/  desktopApp/  iosApp/ (Xcode + shared.framework)   serverApp/ (Ktor JVM)
     │            │             │
     └────────────┴──── shared (só iOS: MainViewController) ───┘
                         │
          core-navigation (RootComponent)   core-di (appModules)
                         │                        │
                 features/feature-*  ◄────────────┘
                         │
                    domain-ptt  ◄──── data-ptt
                                          │
         core-network · core-audio · core-database · core-datastore · core-common
```

| Módulo | Conteúdo real |
|---|---|
| `androidApp` | `MainActivity` (permissões, teclas do volante/mídia → PTT, liga/desliga o foreground service), `PttApplication` (startKoin), `PttForegroundService`, metadados Android Automotive |
| `desktopApp` | `Main.kt`: startKoin + `RootComponent` + janela Compose; empacota DMG/MSI/DEB. `DesktopServerHost` liga o modo host (24.2) |
| `iosApp` | Shell SwiftUI (`ContentView` → `MainViewControllerKt.MainViewController()`); `project.yml` para XcodeGen |
| `shared` | Só `iosMain`: gera `shared.framework` estático e expõe `MainViewController` |
| `serverApp` | Executável do servidor: `main`, keystore, Netty, anúncio mDNS e `application.conf` |
| `server-core` | Núcleo do servidor (24.1, [ADR 0010](adr/0010-modo-host-no-app.md)): `module()` com `/ws`, `/api/auth/login`, painel `/admin` + `/api/admin/*`, `ChannelRegistry`/`PttChannel`, anúncio mDNS (`announceOnLan`), `PttHostServer` (modo host, 24.2) e os testes |
| `core-common` | `isLocalNetwork()`, `StorageInfoProvider` expect/actual |
| `core-network` | `HttpClient` + `createPlatformHttpClient` (expect/actual), `PttWebSocketClient`, protocolo (`ControlMessage`, `AudioEnvelope`), `ServerDiscoveryService` |
| `core-audio` | Interfaces `AudioRecorder`/`AudioPlayer`/`AudioCodec`, `PcmPassthroughCodec`, `OpusAudioCodec`, implementações por plataforma com jitter buffer, `MicrophonePermissionManager` |
| `core-database` | SQLDelight `PttDatabase` (tabelas `Channel`, `VoiceMessage`) + drivers |
| `core-datastore` | `SettingsFactory` expect/actual (nome `ptt_lan_settings`) |
| `core-designsystem` | Tema Liquid Glass (ADR 0006): `PttTheme(appTheme, reduceTransparency)`, tokens de cor/tipo/forma, `Modifier.glass`/`contentCard`, `AmbientGlow`, `PttButton` (lente), `PttTopBar`, `GlassIconButton`, `PillButton`, `PttTextField`, `SegmentedControl`, `PttSwitch`, `ConnectionStatusBadge`, `ChannelCard`, `ParticipantAvatar`, snackbar |
| `core-di` | `coreModule` + `platformModule` (expect/actual) e `appModules()` que agrega domain, data e **todos os módulos de feature** |
| `core-navigation` | `RootComponent` (stack Decompose, tema e transparência vindos das settings) e `RootScreen` (aplica o tema; sem Scaffold) |
| `core-telemetry` | Apenas `AnalyticsTracker` + `NoOpAnalyticsTracker` |
| ~~`core-testing`, `feature-admin-web`~~ | Removidos na 22.2. O painel admin real está em `serverApp/src/main/resources/static/index.html` |
| `domain-ptt` | Interfaces de repositório, modelos e use cases (finos, delegam ao repositório) |
| `data-ptt` | `ConnectionRepositoryImpl`, `ChannelRepositoryImpl`, `ChannelSessionRepositoryImpl`, `VoiceRepositoryImpl` (floor + TX/RX), `HistoryRepositoryImpl` (replay, com `PlaybackPosition`) e `HistoryRecorder` (22.7) |
| `features/*` | `connection`, `channel-list`, `ptt`, `history`, `settings` — cada uma com `XComponent` (MVI), `XScreen` (Compose) e `di/XFeatureModule` |

Convention plugins em `buildSrc`: `ptt.kmp.library` (targets + namespace derivado do path),
`ptt.compose.library`, `ptt.android.library`. Referências entre módulos usam type-safe project
accessors (`projects.core.coreNetwork`).

## 4. Fluxos principais

### 4.1 Conexão
1. `ConnectionComponent` lista servidores via `DiscoverServersUseCase` (mDNS `_pttlan._tcp`) ou
   aceita IP/host manual (porta fixa **9443**). Salva `nickname` e `manualIp` nas settings.
2. `ConnectionRepositoryImpl.connect` → `PttWebSocketClient.login` (`POST https://host:9443/api/auth/login`
   com `nickname` + `deviceId = "device-${nickname.hashCode()}"`) → recebe `token` + `userId`. O `userId` é gerado
   pelo servidor, vai no `sub` do JWT e fica em `ConnectionRepository.sessionUserId`.
3. `PttWebSocketClient.connect` abre `wss://host:9443/ws?token=…` e fica em loop de reconexão
   (backoff 1s→30s, jitter ±20%). Falha na **primeira** tentativa é propagada; depois disso reconecta sozinho.
   Timeout: 5s em rede local, 15s fora.
4. `RootComponent` observa o status: se cair de `Connected` para `Reconnecting`, desconecta e volta
   para a tela de conexão com "Servidor desconectado".

### 4.2 Canal e PTT
- `ChannelListComponent` mostra canais ativos (mensagem `active_channels_list` do servidor) e recentes (SQLDelight).
- Ao abrir `PttScreen(channelId)`, `PttComponent` envia `JoinChannel` e observa participantes, speaker e floor denied.
  O `userId` local é o `sessionUserId` emitido no login (o mesmo token é reutilizado nas reconexões).
- **Apertar PTT** → `StartTransmittingUseCase` envia só `StartSpeaking`. A captura começa **quando chega
  `SpeakerChanged(isSpeaking=true)` para o próprio usuário** (`PttComponent` chama `voiceRepository.startTransmitting`).
- **Soltar** → `StopTransmittingUseCase`: para captura e envia `StopSpeaking`.
- Hardware: `MainActivity.dispatchKeyEvent` mapeia `MEDIA_PLAY_PAUSE`, `MEDIA_NEXT`, `HEADSETHOOK` e `SPACE` para `RootComponent.handlePttKey`.

### 4.3 Áudio
- PCM 16-bit mono **48 kHz**. Codec escolhido pela setting `use_opus` (padrão PCM).
- Transmissão: `AudioRecorder.startCapture()` → encode → `AudioEnvelope` → frame binário.
- Recepção: decodifica conforme `envelope.codec` → `AudioPlayer.play(chunk, sequenceNumber, timestampMs)`.
- Jitter buffer nos players (ex.: `JvmAudioPlayer`): fila por `sequenceNumber`, pré-buffer de 5 pacotes,
  descarta pacotes atrasados, reseta a sequência quando o stream esvazia.

### 4.4 Histórico (replay local)
- Só grava se `allow_cache = true` (padrão **false**). Cada fala recebida vira um arquivo
  `<canal>_<epochMs>.pcm` no diretório de `cache_location`, com o PCM decodificado (sem cifra desde a 22.9).
- Metadados em `VoiceMessage`; limite de 50 por canal e `max_cache_size_mb` (padrão 500) para o total de arquivos.
- `HistoryComponent` lista, toca, pausa e apaga. O servidor **não** guarda áudio (ADR 0005).

### 4.5 Background
- Android: com `Connected` + canal ativo + `always_listening` (padrão true), `MainActivity` inicia
  `PttForegroundService` (tipo `microphone|mediaPlayback`) para manter o processo vivo.
- iOS: só em primeiro plano (ADR 0004).

## 5. Protocolo (`core-network/.../protocol`)

Um WebSocket por cliente em `/ws`.

**Frames de texto** — `ControlMessage` (JSON polimórfico, discriminador `type`). Nas mensagens C→S, os campos
`userId`/`nickname` são **ignorados pelo servidor**: a identidade vem do JWT validado no handshake.

| `type` | Direção | Campos |
|---|---|---|
| `join_channel` | C→S | channelId, nickname, userId |
| `leave_channel` | C→S | channelId, userId |
| `start_speaking` / `stop_speaking` | C→S | channelId, userId |
| `participant_list` | S→C | channelId, participants[userId, nickname, isSpeaking] |
| `speaker_changed` | S→C | channelId, userId, nickname, isSpeaking |
| `floor_denied` | S→C | channelId, reason |
| `active_channels_list` | S→C | activeChannels[channelId, participantCount] |
| `system_alert` | S→C | message (broadcast do admin) |

**Frames binários** — `[Int32 big-endian: N][N bytes: AudioEnvelope em JSON][payload PCM/Opus]`.
`AudioEnvelope = { channelId, senderId, sequenceNumber, codec: PCM16|OPUS, timestampMs }`.
O servidor não abre o envelope: repassa o frame inteiro a todos os participantes do canal do remetente,
**somente se** o remetente for o speaker atual.

Qualquer mudança aqui afeta cliente e servidor ao mesmo tempo — o `serverApp` reutiliza as classes de `core-network`.
Existe teste de round-trip em `ControlMessageTest`.

## 6. Servidor (`serverApp`)

- Entrada `Application.kt`: `main` gera `build/keystore.jks` self-signed (alias `pttlan`, senha `password`) se não existir
  e sobe via `EngineMain` com `application.conf` (só **HTTPS 9443**; o conector HTTP em claro foi removido na 19.2).
- `module()`: WebSockets (ping 20s), ContentNegotiation, anúncio mDNS (porta 9443, ignora interfaces docker/utun/tailscale/vbox…),
  Koin (`ChannelRegistry`), autenticação JWT + Basic (admin), RateLimit (global 100/min por IP; login 5/min).
  `XForwardedHeaders` só é instalado com `PTT_TRUST_PROXY=true`, para o rate limit enxergar o IP real atrás do proxy sem permitir spoofing em LAN.
  Senhas e segredos vêm do ambiente (`PTT_ADMIN_PASSWORD`, `PTT_JWT_SECRET`, `PTT_KEYSTORE_PASSWORD`), com fallback de LAN — ver README.
- **Auth** (`JwtConfig`): HMAC256 com segredo de `PTT_JWT_SECRET`; sem a variável, um aleatório por boot → reiniciar invalida todos os tokens. Validade 1 dia.
  O login não tem senha: qualquer nickname/deviceId não vazio recebe token. O servidor gera o `userId` (claim `sub`)
  e o devolve em `LoginResponse`.
- `/ws`: exige `?token=`; `userId` e `nickname` vêm só do token (os das mensagens são ignorados); nickname precisa ser único (case-insensitive) entre conexões — senão fecha com "Nome já em uso".
  Query param `version` (constante `APP_VERSION` do cliente, 20.5) aparece no painel; `protocol` (`PROTOCOL_VERSION`, 21.5)
  é recusado quando diverge do servidor. As duas pontas serializam com `PttJson` (`ignoreUnknownKeys`).
- `ChannelRegistry`: estado em memória (`ConcurrentHashMap`). Canal `Geral` sempre existe; canais vazios são removidos após 5 min.
  Guarda logs (últimos 100), tempo de fala por nickname e série temporal por minuto (30 min).
- `PttChannel`: participantes + floor control com `Mutex`; áudio sai por uma fila por ouvinte
  (`Channel(50, DROP_OLDEST)` + coroutine de envio), então um cliente lento só perde os próprios pacotes. Floor liberado por `StopSpeaking`, por desconexão ou pelo
  watchdog da 20.4 (sem áudio por `ptt.floorIdleTimeoutMs`, padrão 2s, ou fala acima de `ptt.maxSpeechDurationMs`, padrão 60s).
- Painel admin: `GET /admin` (HTML + Chart.js via CDN) consumindo `GET /api/admin/metrics`, `POST /api/admin/system/{restart|shutdown|broadcast}`,
  `GET /api/admin/logs/csv`, `POST /api/admin/channels/{id}/kick/{userId}`, `POST /api/admin/channels/{id}/delete`.
  Protegido por Basic auth (usuário `admin`) quando `PTT_ADMIN_PASSWORD` (config `ptt.adminPassword`) está definida; sem a variável
  as rotas de escrita não são registradas e só sobram `metrics` e `logs/csv` abertos.
- Docker: `Dockerfile` multi-stage (Temurin 21, `installDist`), imagem publicada no GHCR para amd64/arm64 (Raspberry Pi).

## 7. Settings (multiplatform-settings)

| Chave | Tipo | Padrão | Uso |
|---|---|---|---|
| `nickname` | String | `User-xxxx` | Identidade no canal / login |
| `manualIp` | String | — | Último host digitado |
| `app_theme` | Int | 0 | Índice de `AppTheme` (SYSTEM, LIGHT, DARK) |
| `reduce_transparency` | Boolean | false | Troca o vidro por superfícies sólidas |
| `use_opus` | Boolean | false | Codec de transmissão |
| `always_listening` | Boolean | true | Foreground service no Android |
| `allow_cache` | Boolean | false | Grava histórico local |
| `cache_location` | String | `"Interno"` | Diretório do cache (`StorageInfoProvider`) |
| `max_cache_size_mb` | Int | 500 | Limite total dos `.pcm` |

## 8. Divergências entre plano e código

O plano é o SSOT de intenção, mas estes pontos refletem o código atual:

| Tema | Plano | Código |
|---|---|---|
| Envelope de áudio | ProtoBuf | JSON com prefixo de tamanho (Int32) |
| Taxa de amostragem | 16 kHz | 48 kHz |
| TLS | Self-signed + TOFU com fingerprint exibido | Android/JVM (mesma implementação em `jvmAndAndroidMain`) e iOS aceitam **qualquer** certificado e hostname quando o host é "local" (`isLocalNetwork`: localhost, `.local`, 10/8, 172.16/12, 192.168/16) e usam a validação do sistema fora da LAN. Sem fingerprint |
| "Criptografia do stream" | Sobre TLS | Só TLS. O `AudioCrypto` do cache foi removido na 22.9 ([ADR 0009](adr/0009-remover-audiocrypto.md)): a chave estava no binário |
| Redis / multi-instância (Fase 17) | Estado e pub/sub no Redis | Removido na 22.1 ([ADR 0007](adr/0007-remover-redis.md)): o estado é em memória e o servidor roda em uma instância só |
| Admin | — | Basic auth com `PTT_ADMIN_PASSWORD`; sem a variável, leitura (`metrics`, `logs/csv`) segue aberta e as rotas de escrita ficam desligadas |
| Floor control | Liberado também por timeout de heartbeat; regra replicada no domain | O `Heartbeat` saiu do protocolo (21.5); quem cobre é o watchdog de inatividade da 20.4, e a regra só existe no servidor |
| Dependências entre módulos | `core-di` não conhece features; features não usam `core-network` | `feature-ptt` deixou de usar `core-network` (22.8); `core-di` e `core-navigation` seguem agregando todas as features, agora documentado na [ADR 0008](adr/0008-grafo-de-dependencias-entre-modulos.md). A regra automática fica para a 23.4 |
| Engine do client | CIO | OkHttp (Android/JVM), Darwin (iOS) |
| Limites Detekt | Classe 300 / função 40 linhas | `LargeClass` 600 / `LongMethod` 60 em `config/detekt/detekt.yml` |
| Testes | Fakes em `core-testing`, snapshot tests, Kover no CI | `core-testing` foi removido (22.2); snapshot tests ainda não existem (23.5); o CI roda `jvmTest`, `:server-core:test` e `koverVerify` com piso por módulo (23.2) |
| Targets iOS | Inclui `iosX64` | Só `iosArm64` e `iosSimulatorArm64` |

## 9. Testes existentes

| Onde | Arquivo |
|---|---|
| `core-common` (commonTest) | `NetworkCommonUtilsTest` |
| `core-network` (commonTest) | `ControlMessageTest` |
| `domain-ptt` (jvmTest) | `ConnectToServerUseCaseTest` |
| `feature-*` (jvmTest, MockK) | `ConnectionComponentTest`, `ChannelListComponentTest`, `PttComponentTest`, `HistoryComponentTest`, `SettingsComponentTest` |
| `serverApp` (test, `testApplication`) | `ServerIntegrationTest` (floor control; identidade do token não pode ser forjada por mensagem), `AuthIntegrationTest` (login, `userId` = `sub` e rate limit) |

Os testes de Component rodam em `jvmTest` porque MockK não suporta Native.

## 10. Comandos

```bash
./gradlew :serverApp:run                       # servidor (gera keystore em build/ na 1ª vez)
./gradlew :desktopApp:run                      # cliente desktop
./gradlew :androidApp:installDebug             # cliente Android
cd iosApp && xcodegen && open iosApp.xcodeproj # cliente iOS (framework gerado pelo :shared)

./gradlew jvmTest :server-core:test            # testes JVM (`test` sozinho não roda os jvmTest das KMP)
./gradlew :features:feature-ptt:jvmTest        # um módulo
./gradlew :server-core:test --tests "*ServerIntegrationTest"
./gradlew iosSimulatorArm64Test                # testes no simulador iOS
./gradlew detekt ktlintCheck                   # lint (ktlintFormat para corrigir)
./gradlew dokkaHtmlMultiModule                 # docs em build/dokka/htmlMultiModule
./gradlew :desktopApp:packageDistributionForCurrentOS
docker build -t ptt-server .                   # imagem do servidor (porta 9443)
```

Run configurations do IntelliJ/Android Studio: `.run/Run_Desktop__JVM_.run.xml` e `.run/Run_Server__Ktor_.run.xml`.

**CI** (`.github/workflows/ci.yml`, push/PR em `main` e `develop`): lint → unit-test → builds Android, Desktop (Linux/macOS/Windows),
Server (`distZip`), iOS (XcodeGen + testes no simulador + `xcodebuild`) → Docker multi-arch (publica no GHCR só em push, não em PR).
Usa JDK 21 e cache do Gradle (`setup-gradle`).
Em **pull request** tudo que pode falhar continua rodando (lint, testes e compilação de todos os alvos), mas o empacotamento é reduzido:
Desktop só no runner Linux, imagem Docker só `linux/amd64` (o arm64 emulado por QEMU é o passo mais lento) e os artefatos de Desktop,
Server e iOS não são publicados — só o APK continua disponível para baixar do PR.
Cache: `setup-gradle` (Gradle), `actions/cache` do `~/.konan` (toolchain Kotlin/Native) e `type=gha` para as camadas da imagem Docker.
`.github/dependabot.yml` abre PRs semanais agrupados para GitHub Actions e dependências Gradle.

## 11. Convenções

- Pacotes `com.pttlan.<camada>.<módulo>`; sufixos `UseCase`, `RepositoryImpl`, `Component`, `Screen`, `FeatureModule`.
- Toda tela = `XComponent` com `StateFlow<XState>`, `SharedFlow`/`Channel` de `XEffect` e `onIntent(XIntent)`;
  navegação acontece por efeitos coletados no `RootComponent`.
- Componentes recebem `ComponentContext` e parâmetros via `parametersOf` do Koin.
- Design system (ADR 0003 e 0006): vidro só em controles, conteúdo em `contentCard`; proibido `Color(...)`, `.sp` e `.copy(fontSize=…)` em features; usar `MaterialTheme`/`PttTheme`.
  `.dp` só para layout.
- Commits: Conventional Commits (`feat:`, `fix(audio):`, `docs:`, `ci:`…), mensagens em inglês ou português.
- Decisão de arquitetura nova → ADR em `docs/adr/NNNN-titulo.md`. Não existe ADR 0001.
- Strings de UI em pt-BR direto no código (sem recursos de localização).

## 12. Pontos de atenção

- O `userId` vale por login (novo a cada conexão) e o `deviceId` deriva do hash do nickname: ainda não há identidade
  persistente de dispositivo (roadmap 20.2).
- `docs/ptt-lan-design-system.html` é a referência visual do design system (Fase 8).
