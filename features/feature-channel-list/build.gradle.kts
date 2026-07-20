plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            implementation(project.dependencies.project(":core:core-designsystem"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(project.dependencies.project(":domain:domain-ptt"))

            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.project(":core:core-testing"))
            implementation(kotlin("test"))
        }
    }
}
