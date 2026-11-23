# PTT-LAN — Roteiro de testes nos dispositivos

Roteiro da Fase 29 do [roadmap](ROADMAP_MELHORIAS.md). Passa o app inteiro, fluxo por fluxo, em todos os
aparelhos, e deve ser repetido antes de cada release. Cada rodada ganha uma coluna nova na matriz, com a data e o
build testado.

## Aparelhos

| Sigla | Aparelho | App | Observação |
|---|---|---|---|
| AND | Motorola razr 60 (Android 16, API 36) | `androidApp` | Também testa a tela externa e a postura mesa |
| WEAR | Galaxy Watch (Wear OS) | `wearApp` | Só cliente; sem histórico e sem hospedar |
| MAC | Mac (macOS) | `desktopApp` | Hospeda sala |
| IOS | iPhone (ou simulador) | `iosApp` | Telas nativas em SwiftUI (Fase 32); não hospeda |
| IPAD | iPad (ou simulador) | `iosApp` | Lista e canal lado a lado a partir de 840pt |
| DUO | iPhone dobrável (simulador do Xcode 27.1) | `iosApp` | Larguras da 27.1 e 27.2 |
| AW | Apple Watch | `watchApp` | Controle remoto do iPhone (Fase 33), não conecta sozinho |

Não se aplica: hospedar em WEAR, IOS, IPAD, DUO e AW; histórico e configurações em WEAR e AW. Essas células ficam
com `—`.

## Preparação

1. Instalar o build da rodada em todos os aparelhos: o APK de **release** no AND e no WEAR, porque o R8 (31.2)
   pode quebrar o que o debug não quebra.
2. Todos na mesma Wi-Fi, sem isolamento de clientes. Os casos de rede ruim ficam na 29.5.
3. Um servidor de referência: o `serverApp` ou a imagem Docker (portas 9393 e 9443), além das salas hospedadas
   pelos próprios aparelhos nos fluxos de hospedar.
4. Começar sem dados: apagar os dados do app (ou reinstalar) num aparelho de cada plataforma, para passar pela
   primeira abertura e pelas permissões.
5. Para os fluxos com fone, um fone Bluetooth pareado.

## Fluxos

Cada fluxo tem um código (usado na matriz), os passos e o resultado esperado. Em todos vale: nenhum texto
técnico de erro na tela, nenhum texto cortado e nenhum travamento.

### C — Conectar

| Código | Passos | Esperado |
|---|---|---|
| C1 Primeira abertura | Abrir o app recém-instalado | Pede microfone e notificações (no Android 17, também rede local); negar o microfone não fecha o app e mostra o que fazer |
| C2 Descoberta | Na tela de conexão, esperar a lista de servidores | O servidor de referência e as salas hospedadas aparecem com o nome em poucos segundos; "Procurar de novo" atualiza |
| C3 IP manual | Digitar `ip:9443` do servidor e conectar | Entra na lista de canais; endereço inválido mostra "Endereço inválido" sem sair da tela |
| C4 Nome em uso | Conectar com o mesmo apelido de outro aparelho já conectado | Mensagem dizendo que o nome está em uso |
| C5 Certificado mudou | Reinstalar o servidor (certificado novo) e conectar de novo num aparelho que já tinha confiado | Aviso de certificado mudou; "Confiar" conecta e cancelar não conecta |
| C6 Sair | "Desconectar" na lista de canais (no iOS, também o gesto de voltar da borda) | Volta para a conexão e o servidor registra a saída |

### H — Hospedar (AND e MAC)

| Código | Passos | Esperado |
|---|---|---|
| H1 Sem PIN | "Hospedar" sem PIN | Sala anunciada como `PTT-LAN-<nome>`; outros aparelhos a acham na descoberta e entram |
| H2 Com PIN | Hospedar com PIN de 6 dígitos ou mais | PIN curto é recusado ao hospedar; quem entra com o PIN certo conecta e com o errado vê a mensagem de PIN errado |
| H3 Tentativas | Errar o PIN várias vezes seguidas | Bloqueio por excesso de tentativas, com mensagem |
| H4 Tela bloqueada (AND) | Hospedar, bloquear a tela e deixar dois aparelhos falando por 10 min | A sala continua de pé, com a notificação do serviço; sem cortes nem quedas |
| H5 Parar de hospedar | "Parar de hospedar" e confirmar | Os conectados voltam para a tela de conexão com o motivo; a sala some da descoberta |

### K — Canais

| Código | Passos | Esperado |
|---|---|---|
| K1 Lista | Entrar no servidor | Canal "Geral" e salas ativas com o número de pessoas, atualizando quando alguém entra ou sai |
| K2 Criar | Criar um canal com nome novo | O canal aparece para todos e quem criou entra nele |
| K3 Trocar | Sair de um canal e entrar em outro (no IPAD e no AND aberto, tocar outro canal na lista ao lado) | Troca sem desconectar; a lista de participantes acompanha |

### P — Falar e ouvir

| Código | Passos | Esperado |
|---|---|---|
| P1 Falar | Segurar o botão, falar 5 s e soltar | Vibração ao apertar e soltar; os outros ouvem com voz clara e veem quem está falando |
| P2 Ouvir | Outro aparelho fala | Botão e participante em azul; áudio sem picotes |
| P3 Disputa | Dois aparelhos apertam juntos | Só um ganha a palavra; o outro vê o aviso de canal ocupado e não transmite |
| P4 Quem cai | Quem está falando fecha o app ou perde a rede | Sai da lista e a palavra é solta em segundos, sem prender o canal |
| P5 Botões físicos (WEAR) | Usar o botão lateral do relógio | Fala enquanto segura, como o botão da tela |

### R — Histórico e replay (AND, MAC, IOS, IPAD, DUO)

| Código | Passos | Esperado |
|---|---|---|
| R1 Gravação | Com "Salvar áudios" ligado, ouvir algumas falas e abrir o histórico | As falas aparecem agrupadas por sala, na ordem em que foram ditas, com "N novas" |
| R2 Tocar a sala | "Tocar a sala" | Toca tudo em sequência; o mini player mostra quem falou e "3 de 12" |
| R3 Controles | Anterior, próxima, ±5 s, arrastar a barra e velocidade 1,5× e 2× | Cada controle faz o que diz; a velocidade não muda o tom da voz |
| R4 Ao vivo durante o replay | Alguém fala no canal durante o replay | O replay pausa, o ao vivo toca e o replay retoma depois |
| R5 Tela apagada | Tocar a sala e apagar a tela | Continua tocando até o fim |
| R6 Compartilhar e apagar | Compartilhar uma mensagem e apagar outra | Sai um `.wav` que toca em outro app; a apagada some |

### S — Configurações (AND, MAC, IOS, IPAD, DUO; nome e PIN no WEAR)

| Código | Passos | Esperado |
|---|---|---|
| S1 Tema | Trocar entre claro, escuro e o do sistema | Todas as telas seguem o tema escolhido, que continua depois de reabrir o app |
| S2 Histórico | Ligar e desligar "Salvar áudios", trocar o local e o limite de espaço, "Limpar histórico" | As opções aparecem só com o salvar ligado; limpar pede confirmação e esvazia o histórico |

### B — Segundo plano e reconexão

| Código | Passos | Esperado |
|---|---|---|
| B1 Segundo plano | Num canal, ir para outro app ou bloquear a tela | No Android e no Wear, continua ouvindo (notificação ou Ongoing Activity); no iOS, anotar o comportamento (sem recebimento em segundo plano, ADR 0004) |
| B2 Queda curta | Desligar o Wi-Fi do aparelho por ~3 s | "Reconectando…" e volta ao mesmo canal sozinho, sem "Nome já em uso" (fecha a 20.3) |
| B3 Servidor cai | Parar o servidor por 30 s e subir de novo | Mostra que está reconectando e volta quando o servidor sobe |

### F — Fone Bluetooth

| Código | Passos | Esperado |
|---|---|---|
| F1 Saída no fone | Com o fone conectado, ouvir uma fala | O áudio sai no fone (no IOS fecha a 26.1) |
| F2 Microfone do fone | Ligar "Usar microfone do fone Bluetooth" e falar | Os outros ouvem pelo microfone do fone; anotar o atraso para ativar |

### W — Relógios

| Código | Passos | Esperado |
|---|---|---|
| W1 Wear sem o celular | Celular desligado ou longe; o relógio entra num canal e fala com um celular por 30 min | Acha o servidor, fala e ouve; anotar tempo para o Wi-Fi subir e bateria gasta (fecha a 25.1) |
| W2 Apple Watch como controle | Com o iPhone conectado e no bolso, trocar de canal e falar pelo relógio | O relógio mostra os canais, quem fala e os participantes; segurar o botão fala pelo iPhone |
| W3 iPhone fora de alcance | Afastar o iPhone ou desligar o Bluetooth do relógio | O relógio avisa e desabilita o botão; volta sozinho quando o iPhone fica alcançável |

### D — Dobrar e girar (AND, IPAD, DUO)

| Código | Passos | Esperado |
|---|---|---|
| D1 Girar e redimensionar | Retrato, paisagem e, no iPad, Split View | Nenhuma tela esticada ou cortada |
| D2 Tela externa e postura mesa (AND) | Usar a tela externa e o Flip meio aberto | PTT cabe na tela externa; na postura mesa, botão embaixo e status em cima |
| D3 Dobrar falando (AND) | Dobrar ou desdobrar com o botão pressionado | A conexão continua e a palavra não fica presa |

## Matriz

Uma rodada por bloco. Resultado: `✅` passou, `❌` falhou (com o número do achado), `⚠️` passou com ressalva,
`—` não se aplica, vazio = não testado.

### Rodada 1 — aaaa-mm-dd — build x.y.z

| Fluxo | AND | WEAR | MAC | IOS | IPAD | DUO | AW |
|---|---|---|---|---|---|---|---|
| C1 Primeira abertura | | | | | | | — |
| C2 Descoberta | | | | | | | — |
| C3 IP manual | | | | | | | — |
| C4 Nome em uso | | | | | | | — |
| C5 Certificado mudou | | | | | | | — |
| C6 Sair | | | | | | | — |
| H1 Sem PIN | | — | | — | — | — | — |
| H2 Com PIN | | — | | — | — | — | — |
| H3 Tentativas | | — | | — | — | — | — |
| H4 Tela bloqueada | | — | — | — | — | — | — |
| H5 Parar de hospedar | | — | | — | — | — | — |
| K1 Lista | | | | | | | |
| K2 Criar | | | | | | | — |
| K3 Trocar | | | | | | | |
| P1 Falar | | | | | | | |
| P2 Ouvir | | | | | | | |
| P3 Disputa | | | | | | | |
| P4 Quem cai | | | | | | | — |
| P5 Botões físicos | — | | — | — | — | — | — |
| R1 Gravação | | — | | | | | — |
| R2 Tocar a sala | | — | | | | | — |
| R3 Controles | | — | | | | | — |
| R4 Ao vivo durante o replay | | — | | | | | — |
| R5 Tela apagada | | — | — | | | | — |
| R6 Compartilhar e apagar | | — | | | | | — |
| S1 Tema | | — | | | | | — |
| S2 Histórico | | — | | | | | — |
| B1 Segundo plano | | | | | | | — |
| B2 Queda curta | | | | | | | — |
| B3 Servidor cai | | | | | | | — |
| F1 Saída no fone | | | | | | | — |
| F2 Microfone do fone | | | — | | | | — |
| W1 Wear sem o celular | — | | — | — | — | — | — |
| W2 Apple Watch como controle | — | — | — | — | — | — | |
| W3 iPhone fora de alcance | — | — | — | — | — | — | |
| D1 Girar e redimensionar | | — | | | | | — |
| D2 Tela externa e postura mesa | | — | — | — | — | — | — |
| D3 Dobrar falando | | — | — | — | — | — | — |

### Achados da rodada 1

| # | Fluxo | Aparelho | O que aconteceu | Destino |
|---|---|---|---|---|
| | | | | |

O destino é a correção nesta fase (pequena) ou o item do roadmap que foi criado para ela.
