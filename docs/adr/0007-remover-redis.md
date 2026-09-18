# ADR 0007: Remover o Redis enquanto houver uma instância só

## Status
Aceito (fase 22.1 do roadmap de melhorias). Encerra o item de Redis da Fase 17 do plano técnico.

## Contexto
A Fase 17 do plano previa Redis para estado compartilhado e pub/sub entre várias instâncias do servidor.
O que existia no código era só metade disso: `RedisManager` subia junto com a aplicação, tentava conectar em
`redis://localhost:6379` a cada boot e, quando falhava, registrava um aviso e seguia. O `ChannelRegistry`
recebia o `RedisManager` no construtor e nunca o usava — todo o estado de canais, participantes e floor
continuava em memória, num `ConcurrentHashMap`.

O custo disso era real e a contrapartida era zero: uma dependência (Lettuce) no artefato do servidor, uma
conexão tentada e falhada em todo boot, um aviso no log que parecia problema e não era, e a leitura errada,
para quem chegasse no projeto, de que o servidor já suportaria mais de uma instância.

Implementar de verdade é grande: o estado dos canais e o floor control teriam de sair da memória, o broadcast
de áudio passaria por pub/sub e o floor precisaria de lock distribuído. Isso só se paga com deploy
multi-instância à vista, o que não é o caso — o produto é um servidor por LAN.

## Decisão
Remover `RedisManager`, a dependência `lettuce-core` e o parâmetro correspondente do `ChannelRegistry`.
O estado continua em memória, explicitamente, enquanto o servidor for uma instância só.

## Consequências
- O servidor não tenta mais conectar em Redis nem loga o aviso de falha no boot; sobe mais limpo.
- `docker-compose` e ambientes de execução não precisam mais de um serviço Redis ao lado.
- Rodar mais de uma instância continua não sendo suportado. Quando isso for necessário de fato, a decisão
  volta à mesa como uma fase própria — e aí o desenho (estado, pub/sub e lock do floor) é feito inteiro,
  em vez de meio.
