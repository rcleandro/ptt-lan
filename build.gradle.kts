import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.benManesVersions)
    alias(libs.plugins.dokka)
    alias(libs.plugins.roborazzi) apply false
}
subprojects {
    pluginManager.apply("org.jetbrains.dokka")
    if (name != "androidApp") {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("io.gitlab.arturbosch.detekt")
        
        extensions.configure<KtlintExtension> {
            filter {
                exclude { it.file.path.contains("build") }
            }
        }
    }
}
