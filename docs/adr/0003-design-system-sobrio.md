# ADR 0003: Design System Sóbrio e Moderno

## Status
Aceito

## Contexto
O PTT-LAN é uma ferramenta técnica de comunicação push-to-talk. Inicialmente, o aplicativo possuía estilos genéricos embutidos nas telas. Para conferir uma identidade visual moderna e sobria (Fase 8), é necessário implementar um Design System próprio. Este novo tema visa proporcionar clareza, alta legibilidade e redução da fadiga visual (focando primariamente no Dark Mode), evitando paletas genéricas. 

Além disso, é necessário garantir que não haja dívida técnica visual. Nenhum componente deve usar cores hardcoded ou espaçamentos arbitrários; tudo deve vir de tokens centralizados no módulo `core-designsystem`.

## Decisão
Decidimos implementar um tema (Dark-first) sob a especificação detalhada da Fase 8:
- **Cores**: A paleta será dividida semanticamente (`Background`, `Surface`, `SurfaceElevated`, `Border`, `TextPrimary`, `TextSecondary`, `TextTertiary`, `Primary`, `PrimaryDim`).
- O estado mais crítico (transmissão ativa) usará de forma exclusiva a cor **AccentTransmitting** (`#C77D4C`).
- Valores de fallback (Status) serão `StatusOnline`, `StatusOffline` e `StatusIdle`.
- **Tipografia**: Uso da fonte **IBM Plex Sans** para interface geral, e **IBM Plex Mono** para dados técnicos (IP, ms).
- **Componentes**: 4 componentes principais serão criados e testados através de previews e snapshot tests (`PttButton`, `ConnectionStatusBadge`, `ChannelCard`, `ParticipantAvatar`).

## Consequências
**Positivas:**
- Consistência visual e temática em todas as features.
- Facilidade de manutenção, com um único ponto de verdade para o UI (tokens de design).
- Oportunidade de evoluir facilmente o app (como adicionar light mode de forma automatizada).
- Componentes altamente reutilizáveis e bem documentados com Previews e testes de Snapshot.

**Negativas:**
- Maior rigidez ao desenvolver features novas (é necessário usar o `PttTheme.customColors` em vez das cores padrão do Material).
- Adoção e migração das telas existentes exigirá refatoração.
