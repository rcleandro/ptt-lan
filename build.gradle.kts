plugins {
    alias(libs.plugins.benManesVersions)
}
subprojects {
    if (name != "androidApp") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")
    }
}
