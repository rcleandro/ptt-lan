plugins {
    id("com.github.ben-manes.versions") version "0.51.0"
}
subprojects {
    if (name != "androidApp") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        apply(plugin = "io.gitlab.arturbosch.detekt")
    }
}
