plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":domain:domain-ptt"))
            implementation(project.dependencies.project(":core:core-designsystem"))
            implementation(project.dependencies.project(":core:core-common"))
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(project.dependencies.project(":core:core-datastore"))
        }
        jvmTest.dependencies {
            implementation(project.dependencies.project(":core:core-testing"))
            implementation(kotlin("test"))
            implementation(libs.multiplatform.settings.test)
        }
    }
}
