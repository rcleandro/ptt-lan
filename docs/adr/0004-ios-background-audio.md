# ADR 0004: Limitações do Background Mode no iOS (PTT-LAN)

## Contexto (Fase 10)
O objetivo da Fase 10 era permitir que o aplicativo funcionasse em segundo plano ("sempre ouvindo") para receber áudio continuamente. No Android, o uso de um `Foreground Service` (com permissões apropriadas, persistindo um WebSocket aberto) resolve o caso com confiabilidade.

No iOS, as políticas da Apple são muito mais restritivas em relação à execução de aplicativos em segundo plano para manter WebSockets (rede) ativos de forma contínua, mesmo habilitando as opções do Info.plist (`UIBackgroundModes`: `audio`, `voip`, `fetch`).

## Análise e Limitações do iOS
- **Modo `audio`:** Permite que o áudio continue tocando quando o aplicativo vai para segundo plano (ex: tocadores de música/podcasts). Contudo, este modo não garante que a conexão de rede TCP/WebSocket continue viva de maneira ininterrupta caso não haja fluxo contínuo de áudio naquele exato instante. Se houver silêncio na rede, o iOS suspende os sockets em poucos minutos.
- **Modo `voip`:** Anteriormente utilizado para manter sockets abertos, foi altamente restringido nas versões recentes do iOS devido a abusos. A Apple agora exige o uso do **PushKit** para acordar a aplicação via Push Notification silencioso antes do recebimento de chamadas VoIP.
- **Ambiente LAN:** Como o aplicativo PTT-LAN opera exclusivamente em redes locais (com servidor descoberto por mDNS e sem infraestrutura em nuvem conectada à internet), não é viável usar APNs (Apple Push Notification service) para enviar PushKit tokens e acordar o aplicativo.

## Decisão
1. **Suporte Assíncrono Desabilitado no iOS:** Fica estabelecido e documentado que, para o aplicativo iOS rodando no modelo atual, a garantia de recebimento de áudio funciona de forma otimizada **apenas com o aplicativo em primeiro plano (Foreground)**.
2. **Android "Sempre Ouvindo":** O Android se beneficiará da feature de "Sempre ouvindo" através do seu modelo de permissivo de `Foreground Service` com notificação persistente, já implementado via `PttForegroundService`.
3. Não será investido tempo no ecossistema iOS para contornar suspensões de rede baseadas em "gambitarras" de tocar áudios mudos contínuos. A decisão técnica é aceitar a restrição imposta pelas premissas nativas do SO da Apple.
