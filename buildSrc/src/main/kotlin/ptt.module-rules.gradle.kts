import org.gradle.api.artifacts.ProjectDependency

/**
 * Dependency rules of the module graph (23.4), with the scope [ADR 0008](docs/adr/0008-grafo-de-dependencias-entre-modulos.md)
 * settled: a feature must not depend on another feature, and a feature must not depend on `core-network`.
 *
 * `core-di` and `core-navigation` are aggregators on purpose — they wire Koin and host the navigation stack,
 * so depending on every feature is their job — and that is why the rule targets features only.
 */
val forbiddenFeatureDependencies =
    tasks.register("checkModuleDependencies") {
        group = "verification"
        description = "Fails when a feature module depends on another feature or on core-network"

        val featureProjects =
            rootProject.subprojects
                .filter { it.path.startsWith(":features:") }
                .map { feature ->
                    feature.path to
                        feature.configurations
                            .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                            .map { it.path }
                            .distinct()
                }

        doLast {
            val violations =
                featureProjects.flatMap { (featurePath, dependencies) ->
                    dependencies
                        .filter { dependency ->
                            dependency != featurePath &&
                                (dependency.startsWith(":features:") || dependency == ":core:core-network")
                        }.map { "$featurePath depends on $it" }
                }

            if (violations.isNotEmpty()) {
                error(
                    "Forbidden dependencies between modules (see docs/adr/0008-grafo-de-dependencias-entre-modulos.md):\n" +
                        violations.joinToString("\n"),
                )
            }
        }
    }

tasks.matching { it.name == "check" }.configureEach {
    dependsOn(forbiddenFeatureDependencies)
}
