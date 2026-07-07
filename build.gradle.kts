plugins {
    alias(libs.plugins.benManesVersions)
    alias(libs.plugins.dokka)
}
subprojects {
    pluginManager.apply("org.jetbrains.dokka")
    if (name != "androidApp") {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        
        extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
            filter {
                exclude { it.file.path.contains("build") }
            }
        }
    }
}
