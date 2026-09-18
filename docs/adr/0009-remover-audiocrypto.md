# ADR 0009: Remover o AudioCrypto do cache de áudio

## Status
Aceito (fase 22.9 do roadmap de melhorias).

## Contexto
`AudioCrypto` era um RC4 com chave de 16 bytes escrita no próprio código, aplicado aos arquivos `.pcm` que o
app guarda no cache de histórico. A ideia era proteger as gravações no disco.

Na prática não protegia de ninguém:

- A chave está no binário distribuído. Qualquer pessoa com o APK, o `.app` ou o jar tem a chave.
- Os arquivos já ficam no diretório de cache do app, dentro do sandbox do sistema operacional. Quem consegue
  ler esse diretório (root, backup do dispositivo, device comprometido) também consegue ler o binário.
- RC4 é um algoritmo descontinuado, e o modo de uso aqui — mesma keystream para cada arquivo, já que o estado
  é reiniciado a cada `AudioCrypto()` — é justamente o padrão que torna RC4 quebrável.

Ou seja: custo de CPU em todo pacote gravado e lido, mais uma falsa sensação de proteção na documentação e na
tela de configurações.

A alternativa séria seria chave por instalação no Android Keystore / iOS Keychain. Isso protege num cenário
específico: dispositivo acessado por terceiros com o app instalado, sem root. É código nativo nas duas
plataformas, migração do cache existente e manutenção — e o conteúdo protegido é áudio de PTT em LAN, que
acabou de ser transmitido em claro para todo mundo do canal.

## Decisão
Remover `AudioCrypto`. As gravações do histórico passam a ser gravadas e lidas como PCM puro.

## Consequências
- O cache gravado antes desta mudança fica ilegível (seria decodificado como ruído). O expurgo do histórico
  cuida disso com o tempo; não há migração.
- Menos uma volta de CPU por pacote, na gravação e no replay.
- Se proteção real do cache virar requisito, a decisão volta com o desenho certo: chave por instalação no
  Keystore/Keychain, e não uma chave no código.
