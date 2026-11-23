# ADR 0013: Telas nativas no iOS (SwiftUI) e app para Apple Watch

## Status
Proposto. Planejado nas fases 32 e 33 do [roadmap de melhorias](../ROADMAP_MELHORIAS.md), cada uma começando por
um spike com critério de saída.

## Contexto
No iOS, o app é o mesmo Compose Multiplatform do Android, dentro de um `UIViewController`
(`MainViewController`, mostrado por `ContentView.swift`). Funciona, mas não parece um app de iPhone: rolagem,
teclado, acessibilidade (VoiceOver, Dynamic Type) e o vidro do iOS 26 são imitações em Compose (ADR 0006). Também
não existe app para Apple Watch, embora o relógio seja o lugar natural do botão de falar, como no Wear OS
(ADR 0011).

O que já existe e ajuda:

| Peça | Situação |
|---|---|
| Lógica das telas | Em componentes Decompose com MVI (`state: StateFlow`, `effects`, `onIntent`), sem depender de interface |
| Rede, áudio, Opus, banco, settings | Kotlin comum, já compilado para `iosArm64` e `iosSimulatorArm64` |
| Textos | Compose resources em `core-designsystem`; a leitura (`getString`) não depende de Compose UI |
| Navegação | `RootComponent` com child stack do Decompose |

O que pesa contra:

| Problema | Onde |
|---|---|
| Componentes e telas Compose no mesmo módulo | `features/*/src/commonMain` (ex.: `ConnectionComponent.kt` ao lado de `ConnectionScreen.kt`) |
| `StateFlow`, classes seladas e `suspend` chegam ao Swift sem tipos bons | Interop Kotlin/Native → Objective-C |
| Compose Multiplatform não roda no watchOS | Não há alvo |
| O kopus (Opus) não publica watchOS | Maven Central: só `iosArm64` e `iosSimulatorArm64` |
| No watchOS, WebSocket (`URLSessionWebSocketTask`) e Network framework (Bonjour) são restritos | Apple TN3135: só com sessão de áudio *long-form* ativa (que no relógio exige fone Bluetooth) ou em app VoIP com CallKit; HTTP pelo `URLSession` é livre |
| Não há framework PushToTalk no watchOS | Só no iOS 16+ |

Ktor (cliente Darwin e WebSockets), coroutines, serialization, Koin, Decompose, multiplatform-settings,
SQLDelight e Kermit publicam artefatos `watchosArm64`.

## Decisão
1. **iOS: telas em SwiftUI sobre os mesmos componentes.** A lógica continua em Kotlin. O Swift só desenha o
   `state` e manda `intent`. O Compose continua no Android, no Wear OS e no Desktop. O iOS deixa de embutir o
   Compose quando a última tela migrar.
2. **Apple Watch: primeiro como controle remoto do iPhone**, por WatchConnectivity. O relógio mostra canal, quem
   fala e participantes, e manda apertar ou soltar e trocar de canal. A conexão, o microfone e o áudio ficam no
   iPhone (ou no fone ligado a ele). É o inverso da ADR 0011, porque aqui o cliente independente esbarra em três
   bloqueios ao mesmo tempo: a rede (TN3135), o Opus e o Compose.
3. **Cliente independente no relógio só depois de um spike** que prove a rede (CallKit ou áudio *long-form*) e
   o Opus (AudioToolbox, que codifica e decodifica Opus nas plataformas da Apple, ou libopus compilada para
   watchOS).

## Plano
1. **Spike do iOS (1–2 dias):** a tela de conexão em SwiftUI sobre o `ConnectionComponent`, decidindo a ponte
   entre Kotlin e Swift: o plugin SKIE (Touchlab: `Flow` vira `AsyncSequence`, classe selada vira `enum`) ou um
   adaptador escrito à mão. Critério de saída: estado e efeitos chegam ao SwiftUI sem vazar memória nem travar a
   main thread, e um teste roda no CI.
2. Se passar: as demais telas, uma por vez, cada uma substituindo a versão em Compose no iOS.
3. **Relógio:** target watchOS no `project.yml` com o controle remoto. O spike do cliente independente fica para
   depois, e só se o controle remoto não bastar.

## Consequências
- **Duas interfaces para manter:** cada mudança de tela passa a ser feita em Compose e em SwiftUI. A regra de
  colocar toda decisão no componente, e não na tela, fica mais importante, porque é ela que mantém as duas
  iguais.
- **Os módulos de feature precisam separar lógica e tela** antes de o relógio virar cliente independente
  (watchOS não compila Compose). Para o iOS em SwiftUI basta exportar os componentes no framework.
- **Textos continuam num lugar só:** o Swift lê as strings pelos resources do Kotlin, sem `Localizable` duplicado.
- **O relógio em modo controle remoto depende do iPhone por perto e do app do iPhone vivo**, com as mesmas
  limitações de segundo plano da ADR 0004.
- **PushToTalk no iOS** daria o botão de falar na tela bloqueada e o recebimento em segundo plano, que a ADR 0004
  descartou. Mas ele exige push pela APNs, ou seja, servidor alcançável pela internet (agora possível pelo túnel)
  e conta de desenvolvedor Apple. Fica como item opcional.
