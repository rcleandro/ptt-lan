plugins {
    alias(libs.plugins.benManesVersions)
}
subprojects {
    if (name != "androidApp") {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("io.gitlab.arturbosch.detekt")
    }
}
