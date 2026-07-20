import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    alias(libs.plugins.benManesVersions)
    alias(libs.plugins.dokka)
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover)
}
subprojects {
    pluginManager.apply("org.jetbrains.dokka")
    
    if (name != "androidApp") {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("dev.detekt")
        
        extensions.configure<KtlintExtension> {
            filter {
                exclude { it.file.path.contains("build") }
            }
        }
    }

    tasks.withType<Test> {
        jvmArgs("-Xshare:off")
    }
}
