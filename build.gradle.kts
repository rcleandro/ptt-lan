import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.benManesVersions)
    alias(libs.plugins.dokka)
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover)
}
val ktlintEngineVersion = libs.versions.ktlintEngine.get()

/**
 * Bans `println`/`printStackTrace` outside tests (21.4). Detekt's `ForbiddenMethodCall` is configured for the
 * same thing, but it only runs with type resolution (`detektMain`), which this build does not wire up yet.
 */
val checkNoPrintln by tasks.registering {
    group = "verification"
    description = "Fails when production sources log through println or printStackTrace"

    val sources =
        fileTree(rootDir) {
            include("*/src/**/*.kt", "*/*/src/**/*.kt")
            exclude("**/build/**", "**/*Test*/**", "**/test/**")
        }
    inputs.files(sources)

    doLast {
        val offenders =
            sources.flatMap { file ->
                file
                    .readLines()
                    .withIndex()
                    .filter { (_, line) -> line.contains("println(") || line.contains("printStackTrace()") }
                    .map { (index, line) -> "${file.relativeTo(rootDir)}:${index + 1}: ${line.trim()}" }
            }
        if (offenders.isNotEmpty()) {
            error(
                "Use a logger (Kermit on clients, SLF4J on the server) instead of println/printStackTrace:\n" +
                    offenders.joinToString("\n"),
            )
        }
    }
}

subprojects {
    pluginManager.apply("org.jetbrains.dokka")
    
    if (name != "androidApp") {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("dev.detekt")
        
        extensions.configure<KtlintExtension> {
            version.set(ktlintEngineVersion)
            filter {
                exclude { it.file.path.contains("build") }
            }
        }
    }

    tasks.matching { it.name == "detekt" }.configureEach {
        dependsOn(checkNoPrintln)
    }

    tasks.withType<Test> {
        jvmArgs("-Xshare:off")
    }
}
