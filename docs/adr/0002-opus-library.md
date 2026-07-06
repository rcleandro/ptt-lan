# ADR 0002: Escolha de Biblioteca Opus (KMP)

## Status
Aceito

## Contexto
Na Fase 9, o projeto requer a implementação de compressão Opus (`OpusAudioCodec`) para os alvos Android, JVM (Desktop) e iOS.
Originalmente, o plano técnico (`PTT_KMP_PLANO_TECNICO.md`) sugeria o uso de wrappers JNI nativos sobre `libopus` no Android/JVM, e `cinterop` + CocoaPods no iOS (Seção 7.3). 

No entanto, manter compilação C/C++ via NDK e gerenciar `cinterop` para KMP introduz alta complexidade de manutenção, tempo de build aumentado e dificuldades em alinhar dependências em sistemas variados.

## Decisão
Foi escolhida a biblioteca **Kopus** (`eu.buney.kopus:kopus`), um wrapper Kotlin Multiplatform oficial em torno da API C nativa do Opus.

## Consequências
**Positivas:**
- Elimina a necessidade de escrever C/C++ (JNI) no Android.
- Elimina a necessidade de dependências complexas do CocoaPods apenas para o Opus no iOS.
- Funciona uniformemente em `commonMain` para Android, Desktop (JVM) e iOS.
- API 100% Kotlin Multiplatform (código `expect`/`actual` de codec será centralizado e idêntico).

**Negativas:**
- Adiciona uma dependência externa mantida por terceiros ao core de áudio, no entanto é um wrapper thin que empacota bibliotecas nativas de forma padrão.
