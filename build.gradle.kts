import org.jlleitschuh.gradle.ktlint.KtlintExtension

plugins {
    id("ptt.module-rules")
    alias(libs.plugins.benManesVersions)
    alias(libs.plugins.dokka)
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover)
}
val ktlintEngineVersion = libs.versions.ktlintEngine.get()

/**
 * Minimum line coverage per module (23.2). Each value is what the module covers today, rounded down: the
 * gate exists to stop regressions, and every test added in 23.1 is a reason to raise the number here.
 */
val coverageFloors =
    mapOf(
        ":domain:domain-ptt" to 90,
        ":data:data-ptt" to 30,
        ":core:core-network" to 45,
        ":core:core-audio" to 25,
        ":features:feature-ptt" to 60,
        ":features:feature-connection" to 55,
        ":features:feature-channel-list" to 85,
        ":features:feature-history" to 35,
        ":features:feature-settings" to 90,
        ":server-core" to 85,
    )

/**
 * Bans `println`/`printStackTrace` outside tests (21.4). Detekt's `ForbiddenMethodCall` does the same, but only
 * with type resolution, which iosMain does not get (23.3).
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

/**
 * Findings that predate 23.3, kept so `detekt` fails only on new ones. One file per module and task, all in
 * `config/detekt/baseline`: by default mainJvm and mainAndroid share one file and the last run overwrites it.
 */
fun Project.detektBaselineFile(task: Task, prefix: String): File =
    rootDir.resolve(
        "config/detekt/baseline/${path.drop(1).replace(':', '-')}-" +
            "${task.name.removePrefix(prefix).replaceFirstChar { it.lowercase() }}.xml",
    )

subprojects {
    pluginManager.apply("org.jetbrains.dokka")
    
    if (name != "androidApp" && name != "wearApp") {
        pluginManager.apply("org.jlleitschuh.gradle.ktlint")
        pluginManager.apply("dev.detekt")
        pluginManager.apply("org.jetbrains.kotlinx.kover")

        // Section 16.2 of the technical plan does not require coverage of pure Compose UI, and generated
        // code (SQLDelight, Compose singletons) would only dilute the number.
        extensions.configure<kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension> {
            reports {
                filters {
                    excludes {
                        // By annotation, not by file name: naming would tie the number to where a composable
                        // happens to live, and splitting a screen into sections would "drop" the coverage.
                        annotatedBy("androidx.compose.runtime.Composable")
                        classes(
                            "*ComposableSingletons*",
                            "*.di.*",
                            "com.pttlan.core.database.*",
                        )
                    }
                }

                // Floor, not goal: these are the numbers the code has today, so the build fails on a
                // regression. The targets of section 16.2 of the plan (domain 90, data 85, core-network 90,
                // features 80) are what 23.1 climbs towards — raise the floor as tests land.
                coverageFloors[project.path]?.let { floor ->
                    verify {
                        rule("Line coverage of ${project.path}") {
                            bound {
                                minValue = floor
                                coverageUnits = kotlinx.kover.gradle.plugin.dsl.CoverageUnit.LINE
                            }
                        }
                    }
                }
            }
        }
        
        extensions.configure<KtlintExtension> {
            version.set(ktlintEngineVersion)
            filter {
                exclude { it.file.path.contains("build") }
            }
        }
    }

    // Generated sources (SQLDelight, Compose resources) are fed to the type-resolved tasks too.
    val generated = "${File.separator}build${File.separator}"
    tasks.withType<dev.detekt.gradle.Detekt>().configureEach {
        exclude { it.file.path.contains(generated) }
        detektBaselineFile(this, "detekt").takeIf { it.exists() }?.let { baseline.set(it) }
    }
    tasks.withType<dev.detekt.gradle.DetektCreateBaselineTask>().configureEach {
        exclude { it.file.path.contains(generated) }
        baseline.set(detektBaselineFile(this, "detektBaseline"))
    }

    // On KMP modules the plain `detekt` task looks for src/main/kotlin and analyses nothing, so it runs the
    // type-resolved tasks instead (23.3): main and test on JVM, main on Android, and iosMain without types.
    val typeResolvedDetekt =
        setOf(
            "detektMain",
            "detektTest",
            "detektMainJvm",
            "detektTestJvm",
            "detektMainAndroid",
            "detektIosMainSourceSet",
        )
    tasks.matching { it.name == "detekt" }.configureEach {
        dependsOn(checkNoPrintln)
        dependsOn(project.tasks.matching { it.name in typeResolvedDetekt })
    }

    tasks.withType<Test> {
        jvmArgs("-Xshare:off")
    }
}
