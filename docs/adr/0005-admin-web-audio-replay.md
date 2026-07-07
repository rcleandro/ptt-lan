# ADR 0005: Limitação de Replay no Painel Admin (PTT-LAN)

## Contexto (Fase 11)
A especificação da Fase 11 previa a criação de um Painel Admin Web servido pelo `serverApp` com o seguinte requisito:
> Replay com waveform visual no painel (usa os arquivos de áudio já persistidos na Fase 7).

## Análise
Na Fase 7 (Histórico e Replay), a persistência dos arquivos de áudio foi implementada **no lado do cliente (Client-side)** utilizando `core-database` (SQLDelight) e o sistema de arquivos local do dispositivo (Android/iOS/Desktop).
O `serverApp` atua apenas como um relay (repasse) de pacotes binários WebSocket e não grava ou armazena o histórico de áudio trafegado, mantendo a característica de servidor leve e focado apenas em *floor-control*.

Sendo o Painel Admin Web hospedado pelo servidor, ele não possui acesso direto aos arquivos de áudio locais dos clientes. Para implementar o Replay no painel web, o servidor precisaria também gravar os arquivos de áudio em disco, duplicando a responsabilidade de persistência e consumindo espaço no host do servidor (o que muitas vezes pode ser um celular atuando como host em LAN).

## Decisão
1. **Replay de Áudio no Admin Web Removido:** O painel administrativo não exibirá replay de áudios passados.
2. **Foco do Painel em Métricas em Tempo Real:** O painel web servirá estritamente para monitoramento em tempo real (Conexões Globais, Canais Ativos e Participantes Falando).
3. Essa decisão mantém o servidor leve e sem dependência de armazenamento de longa duração.
