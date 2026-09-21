# ADR 0012: Microfone do fone Bluetooth durante a sessão inteira

## Status
Aceito. Implementado pelo item 26.2 do [roadmap de melhorias](../ROADMAP_MELHORIAS.md).

## Contexto
O áudio do app sai no fone Bluetooth (Android e Wear pelo `USAGE_MEDIA`; iOS desde a 26.1), mas a fala é gravada
pelo microfone do aparelho. Num fone Bluetooth clássico, o microfone só existe no perfil de chamada (HFP/SCO), e
esse perfil muda tudo junto: entrada e saída passam a 8–16 kHz, qualidade de telefone, contra os 48 kHz de hoje.
Trocar de perfil leva cerca de 1 s. Fones LE Audio (Bluetooth 5.2+) têm microfone sem essa perda, mas são minoria.

Com o microfone do fone ligado, há dois momentos possíveis para ativar o perfil de chamada:

1. **Só enquanto se segura o botão**: ouve em qualidade cheia e troca de perfil a cada fala. Custa ~1 s entre
   apertar e começar a transmitir (as primeiras palavras se perdem) e um estalo em cada troca.
2. **A sessão inteira**: ativa ao conectar e desativa ao sair. Falar é instantâneo, e tudo o que se ouve fica em
   qualidade de telefone.

## Decisão
Oferecer o microfone do fone como **opção nas configurações, desligada por padrão**, e, quando ligada, ativar o
perfil de chamada **durante a sessão inteira** (caminho 2).

Em PTT, atraso ao apertar é pior que qualidade menor: quem aperta começa a falar na hora. E quem liga a opção
escolhe essa troca de propósito; quem não liga continua como hoje.

## Consequências
- **Android e Wear** (API 31+): `setCommunicationDevice` com o fone (SCO ou LE Audio) e modo de comunicação;
  captura por `VOICE_COMMUNICATION` e reprodução por `USAGE_VOICE_COMMUNICATION` enquanto ativo. Abaixo da API 31
  a opção não tem efeito.
- **iOS**: a sessão acrescenta `AllowBluetooth` (HFP) às opções enquanto ativo.
- **Desktop**: segue o dispositivo padrão do sistema, como já faz; a opção não se aplica.
- Sem fone conectado ao começar a sessão, nada muda. Um fone conectado no meio da sessão só é usado na próxima.
- **Relógio pareado fica mudo.** Um Galaxy Watch pareado com o mesmo celular é também fone de chamadas dele: vê o
  modo de chamada como uma chamada ativa (`HfpClientConnectionService`) e silencia a mídia, inclusive o PTT-LAN.
  Visto no Galaxy Watch9: o Desktop ouviu o microfone do fone e o relógio, na mesma sala, não. Afeta só o relógio da
  própria pessoa, que já ouve pelo fone; aceito e avisado na opção.
