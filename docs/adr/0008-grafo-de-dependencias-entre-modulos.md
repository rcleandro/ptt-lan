# ADR 0008: Agregadores no grafo de módulos

## Status
Aceito (fase 22.8 do roadmap de melhorias).

## Contexto
O plano técnico (seção 17.3) descreve um grafo em que features não conhecem outras features nem a camada de
rede, e `core-*` não conhece feature nenhuma. O código real tinha duas divergências:

1. `feature-ptt` dependia de `core-network` por um detalhe só: o `PttState` guardava `ParticipantDto`, o DTO
   do protocolo, em vez do modelo de domínio. Uma tela passava a depender do formato do fio.
2. `core-di` e `core-navigation` dependem de **todas** as features. Não é acidente: o `core-di` monta o grafo
   do Koin e o `core-navigation` hospeda o `childStack` do Decompose com um `Child` por feature. Alguém tem
   que conhecer todas as peças para montá-las.

## Decisão
1. `PttState.participants` passa a ser `List<ParticipantDomain>`, e a dependência de `feature-ptt` em
   `core-network` foi removida. Mapear DTO para domínio é trabalho da camada `data`, que já o fazia — a tela
   só consumia o tipo errado.
2. `core-di` e `core-navigation` ficam explicitamente registrados como **agregadores**: é esperado que
   dependam de todas as features, e a regra "core não conhece feature" não se aplica a eles. O que a regra
   automática da fase 23.4 vai cobrar é o que importa de fato: **feature não depende de outra feature** e
   **feature não depende de `core-network`**.

## Consequências
- O estado das telas fica preso ao domínio, não ao protocolo; mudar um DTO do fio não obriga a mexer em UI.
- A regra automática de dependências (23.4) tem um alvo claro e checável, em vez de uma regra que o próprio
  desenho do app viola de propósito.
- Se um dia a navegação deixar de ser centralizada (um nav graph por feature, por exemplo), esta ADR é o
  ponto de partida para revisar o papel do `core-navigation`.
