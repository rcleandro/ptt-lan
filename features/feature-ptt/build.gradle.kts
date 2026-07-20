plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            implementation(project.dependencies.project(":core:core-designsystem"))
            implementation(project.dependencies.project(":core:core-audio"))
            implementation(project.dependencies.project(":domain:domain-ptt"))
            implementation(project.dependencies.project(":core:core-network"))
            implementation(project.dependencies.project(":core:core-datastore"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
        commonTest.dependencies {
            implementation(project.dependencies.project(":core:core-testing"))
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.mockk)
        }
    }
}
