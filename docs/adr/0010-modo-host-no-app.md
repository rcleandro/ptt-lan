# ADR 0010: Servidor embutido no app (modo host) em vez de P2P

## Status
Aceito. Implementado pela fase 24 do [roadmap de melhorias](../ROADMAP_MELHORIAS.md); o host Android depende
do spike descrito em [Plano](#plano).

## Contexto
Hoje todo canal precisa de um `serverApp` rodando em algum lugar da rede: um PC com `./gradlew :serverApp:run`
ou a imagem Docker num Raspberry. Para um grupo que só quer conversar na mesma rede Wi-Fi (ou no hotspot de um
dos celulares), subir uma máquina à parte é o maior atrito do produto. A pergunta é se os próprios usuários
podem dispensar esse servidor.

Há dois caminhos:

1. **P2P de fato**: sem servidor. Cada cliente manda o áudio direto para os outros (malha completa) ou por
   multicast UDP, e o floor control ("um fala por vez") é decidido entre os pares.
2. **Modo host**: um dos aparelhos roda o mesmo servidor de hoje dentro do app e os outros se conectam a ele
   como a qualquer servidor da LAN.

O servidor é pequeno (~1 mil linhas em `serverApp/src/main/kotlin`), o protocolo já é compartilhado via
`core-network`, e o núcleo (`ChannelRegistry`, `PttChannel`) é Kotlin + corrotinas sem nada específico de
plataforma. O que prende o servidor à JVM desktop está concentrado em `Application.kt` e no painel:

| Peça | Desktop (JVM) | Android | iOS |
|---|---|---|---|
| Ktor server (Netty) | igual a hoje | Netty ou CIO — a validar | Ktor server em Kotlin/Native é limitado e sem TLS |
| Keystore `build/keystore.jks` | igual a hoje | Android não lê JKS: PKCS12 em `filesDir` | — |
| Anúncio mDNS (JmDNS) | igual a hoje | `NsdManager.registerService` | — |
| `ManagementFactory` (métricas do painel) | igual a hoje | não existe | — |
| Rodar em background | sim | foreground service (`PttForegroundService` já existe) | não ([ADR 0004](0004-ios-background-audio.md)) |

## Decisão
Adotar o **modo host** e descartar o P2P sem servidor.

O P2P troca um problema resolvido por três abertos:

- **Floor control distribuído.** Sem árbitro, "um fala por vez" exige consenso ou lock por timestamp entre os
  pares, com colisões e casos de borda que hoje não existem porque o `PttChannel` resolve com um `Mutex`.
- **Transporte.** Em malha, quem fala envia N-1 streams. Multicast no Wi-Fi sai na taxa básica, perde pacotes
  e costuma ser limitado ou bloqueado pelo AP.
- **Tudo o que depende do servidor sai junto:** JWT, unicidade de nickname, rate limit, painel admin e o
  próprio protocolo atual. Na prática seria outro produto.

O modo host mantém o protocolo, o cliente e as regras do servidor. Plataformas:

- **Desktop:** suportado. É a mesma JVM do `serverApp`.
- **Android:** suportado, condicionado ao spike.
- **iOS:** só cliente. Sem background e sem servidor Ktor viável em Kotlin/Native, um host iOS derrubaria o
  canal ao bloquear a tela.

## Plano
1. Extrair o núcleo do servidor (`ChannelRegistry`, `PttChannel`, rotas, `JwtConfig`) para um módulo JVM
   (`server-core`). Em `serverApp` ficam só `main`, engine, keystore e JmDNS. O comportamento do `serverApp`
   e da imagem Docker não muda.
2. `desktopApp`: ação "Hospedar" na tela de conexão que sobe o servidor no próprio processo e conecta nele
   como cliente comum.
3. **Spike Android (1–2 dias)** — critério de saída: um celular hospeda, outros dois conectam por mDNS em
   `wss://…:9443` e falam por 10 min com a tela bloqueada. Validar: engine (Netty x CIO) com TLS, keystore
   PKCS12 gerado no aparelho, `NsdManager.registerService`, tipo do foreground service e consumo de bateria.
4. Android host, se o spike passar.

## Consequências
- **O canal vive enquanto o host viver.** Não há migração de host; eleger outro e transferir o estado é
  complexo e não entra agora. Quando o host sai, os clientes caem na tela de conexão com "Servidor
  desconectado", como já acontece hoje.
- **Carga no host.** Ele recebe e retransmite o áudio de todos: em PCM 48 kHz são ~768 kbps por ouvinte.
  O codec é escolhido por quem fala, então ligar Opus só no host não reduz essa carga; o ganho pede Opus como
  padrão em todos os clientes, o que ficou como decisão à parte (ver 24.5 no roadmap).
- **Acesso.** O login não tem senha, então qualquer um na rede entra no canal hospedado. No modo host entra
  um PIN de sala, verificado em `/api/auth/login`.
- **Painel admin.** Fica restrito ao `serverApp`: no modo host (Desktop e Android) o painel não é servido.
- **Redes com isolamento de clientes** (Wi-Fi de visitantes e corporativo) continuam impedindo mDNS e
  conexão direta, assim como já impedem com o servidor dedicado. O hotspot do próprio host contorna.
- O `serverApp` e a imagem Docker continuam sendo o caminho para servidor fixo (Raspberry, nuvem).
- Se o spike Android falhar, esta ADR fica restrita ao Desktop e o item 4 sai do plano.
