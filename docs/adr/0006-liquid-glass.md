# ADR 0006: Design Liquid Glass

## Status
Aceito. Substitui a paleta e os componentes do ADR 0003; a convenção de lint do ADR 0003 continua valendo.

## Contexto
O design da Fase 8 (ADR 0003) ficou num Material 3 genérico: `TopAppBar`, FABs empilhados, formulários com `OutlinedTextField` e tudo no mesmo plano. Três problemas concretos:

- A tela PTT não mostrava quem estava falando, embora `currentSpeakerName` já existisse no estado.
- A regra "âmbar só para transmissão ativa" do ADR 0003 foi quebrada: o `PttButton` usava âmbar para *pedindo a palavra* e verde `#4CAF50` para *transmitindo*.
- O texto terciário (`#6B7278` sobre `#14171A`, ~3,9:1) ficava abaixo do contraste AA.

A proposta visual aprovada fica no canvas "PTT-LAN Liquid Glass" (https://claude.ai/artifact/BP2u9himAWBQYVZ32iX25N).

## Decisão
1. **Duas camadas.** Controles (barras, dock, pílulas, botão PTT) são vidro flutuante (`Modifier.glass`). Conteúdo (listas, cartões) usa superfície sólida (`Modifier.contentCard`). Nunca vidro sobre vidro.
2. **Cor = estado.** Âmbar `AccentTx` só para o próprio usuário transmitindo ou pedindo a palavra; azul `Primary` para receber e para ações; verde `StatusOnline` só para conectado ou canal livre. O token `statusTransmitting` foi removido.
3. **Botão PTT como lente:** vidro claro (livre), anel âmbar tracejado (pedindo a palavra), tinta âmbar com anéis (transmitindo) e tinta azul (recebendo). A luz ambiente (`AmbientGlow`) atrás do vidro repete a cor do estado.
4. **Formas concêntricas:** controles em cápsula; dock 40, painel 32, cartão 22, ícone em cartão 12 (`PttShapes`).
5. **Tema aplicado no `RootScreen`.** `RootComponent` expõe `appTheme` e `reduceTransparency`; os apps Android, Desktop e iOS só chamam `RootScreen`.
6. **Acessibilidade:** configuração "Reduzir transparência" (`reduce_transparency`) troca todo vidro por superfície sólida e desliga a luz ambiente. Texto terciário passou a `#7F8890`.
7. **Layout adaptável na tela PTT:** em telas largas e baixas (paisagem e Android Automotive), área de fala e área do canal ficam lado a lado.

## Implementação do vidro
O vidro atual é **translúcido, sem desfoque do fundo**: base escurecida + gradiente branco + borda com brilho no topo. O desfoque real (backdrop blur) exige a biblioteca Haze, que a partir da 1.7 depende do Compose Multiplatform 1.12; o projeto está no 1.11.1. Como todo vidro passa por `Modifier.glass`, adicionar o desfoque depois é mudança num ponto só:

1. Atualizar o Compose Multiplatform para 1.12.x.
2. Adicionar `dev.chrisbanes.haze:haze` e aplicar `hazeEffect` dentro de `Modifier.glass`, com `hazeSource` no conteúdo de cada tela.
3. Manter o caminho sólido para Android < 12 (sem `RenderEffect`) e para "Reduzir transparência".

## Consequências
**Positivas:**
- Hierarquia clara entre controles e conteúdo; a tela PTT informa o estado por texto, cor e forma.
- Menos código nos apps (tema num lugar só) e componentes reutilizáveis novos: `GlassIconButton`, `PillButton`, `PttTextField`, `LabeledTextField`, `PttSwitch`, `SegmentedControl`, `PttTopBar`, `SectionLabel`, `StatusDot`, `AmbientGlow`.

**Negativas:**
- Sem desfoque real, conteúdo rolando sob as barras fica menos legível que no mockup; por isso as listas reservam espaço para barra e dock.
- `ChannelCard` perdeu os parâmetros `id` e `isActive`, e `ParticipantAvatar` passou a ser vertical. Quem usar esses componentes precisa ser ajustado.

## Ícones do app
O ícone segue o mesmo design: a lente âmbar do botão PTT no estado "transmitindo", com dois anéis e luz ambiente sobre fundo grafite. Os mestres vetoriais ficam em `docs/brand/`:

| Arquivo | Uso |
|---|---|
| `app-icon.svg` | Quadrado sem transparência (o sistema aplica a máscara). Gera `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/icon_1024.png` (sem canal alfa). |
| `app-icon-rounded.svg` | Grade do macOS (quadrado arredondado de 824px num canvas de 1024px). Gera `desktopApp/src/main/resources/icon.png`, `icon.icns` (16–1024px, via `iconutil`) e `icon.ico` (16–256px). |

No Android, o ícone é **adaptativo e vetorial** (`mipmap-anydpi-v26/ic_launcher.xml`): o fundo com a luz ambiente fica em `drawable/ic_launcher_background.xml`, a lente em `drawable/ic_launcher_foreground.xml` e a camada `drawable/ic_launcher_monochrome.xml` atende os ícones temáticos do Android 13+. Como o `minSdk` é 26, os PNGs por densidade foram removidos. A geometria (lente com raio de 21dp no canvas de 108dp, dentro da zona segura de 66dp) é a mesma dos SVGs; ao mudar um, mude os outros.

Para regerar os PNGs, rasterize os SVGs com um renderizador de SVG completo (rsvg-convert, Inkscape, Figma). O renderizador interno do ImageMagick não trata bem os gradientes.
