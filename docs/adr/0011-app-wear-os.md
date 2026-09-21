# ADR 0011: App Wear OS como cliente independente no Wi-Fi

## Status
Aceito. Implementado pela fase 25 do [roadmap de melhorias](../ROADMAP_MELHORIAS.md), condicionado ao spike
descrito em [Plano](#plano).

## Contexto
Rádio PTT é usado com as mãos ocupadas, e o relógio é o lugar natural do botão de falar. A pergunta é se o
PTT-LAN pode rodar no Wear OS e de que forma.

Quase tudo abaixo da interface já roda num relógio:

| Peça | No Wear OS |
|---|---|
| `domain-ptt`, `data-ptt`, `core-network`, `core-datastore` | iguais: é Android |
| `core-audio` (`AudioRecord`, `AudioTrack`) | existem no relógio; alto-falante varia por modelo |
| Opus (kopus) | já traz `arm64-v8a` e `armeabi-v7a` (há Galaxy Watch em 32 bits) |
| `ConnectionComponent`, `ChannelListComponent`, `PttComponent`, `RootComponent` | independentes de interface (Decompose) |
| Telas atuais | feitas para celular; no relógio precisam de Compose for Wear OS |
| `server-core` (modo host) | inviável: +5 MB, bateria e aquecimento |

Há dois caminhos:

1. **Cliente independente**: o relógio se conecta sozinho ao servidor (ou ao host) pelo Wi-Fi.
2. **Extensão do app do celular**: o relógio envia o PTT e o áudio ao celular pela Wearable Data Layer
   (`MessageClient`, `ChannelClient`), e o celular faz a conexão.

## Decisão
Seguir com o **cliente independente**, condicionado ao spike.

Ele reaproveita quase todo o código e não exige um protocolo novo entre relógio e celular. A extensão do
celular tem rede e bateria melhores, mas cria um segundo caminho de áudio, depende do app do celular aberto e
acrescenta um salto de Bluetooth na latência. Fica como plano B.

O relógio é **só cliente**: não hospeda sala.

## Plano
1. **Spike em relógio físico (1–2 dias).** Critério de saída: o relógio, sem o celular por perto, acha o
   servidor na rede (mDNS ou IP manual), entra num canal e fala e ouve com um celular por 30 min. Validar:
   Wi-Fi sob demanda (`ConnectivityManager.requestNetwork` com `TRANSPORT_WIFI` e `bindProcessToNetwork`),
   descoberta NSD por esse Wi-Fi, latência do áudio, alto-falante e consumo de bateria.
2. Se passar: módulo `:wearApp`, com o mesmo papel do `androidApp`, telas em Compose for Wear OS e sessão com
   Ongoing Activity.
3. Se o Wi-Fi sob demanda falhar ou variar demais entre fabricantes: nova ADR para a extensão do celular.

## Consequências
- **Sem "sempre escutando" no relógio.** A escuta contínua do celular esgotaria a bateria de um relógio
  (300–500 mAh) em poucas horas. A sessão dura enquanto o app está aberto, ou pouco além disso.
- **Rede.** O relógio usa a internet do celular por Bluetooth e desliga o Wi-Fi para economizar. Por esse
  caminho não há garantia de alcançar a LAN nem de o multicast funcionar, por isso o app pede o Wi-Fi. Ligar o
  Wi-Fi leva alguns segundos e varia por fabricante. O IP manual continua como alternativa.
- **Áudio.** Alto-falante de relógio é baixo para ambiente aberto, e alguns modelos não têm. O app aceita
  fone Bluetooth e verifica a saída disponível.
- **Botão de falar.** Segurar na tela funciona em todos. Botões físicos (`KEYCODE_STEM_*`) entram pelo
  `handlePttKey` que já existe, onde o relógio os tiver livres.
- **Permissão de rede local.** No Android 17 (o do Galaxy Watch9) o app precisa de `ACCESS_LOCAL_NETWORK`,
  pedida em tempo de execução, para descobrir e alcançar servidores na LAN; sem ela o NSD só abre um seletor do
  sistema e as conexões para IPs locais expiram. Vale também para o `androidApp` em celulares com Android 17.
- **Fora do relógio:** histórico, configurações completas e o modo host.
