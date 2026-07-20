plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            implementation(project.dependencies.project(":core:core-designsystem"))
            implementation(project.dependencies.project(":core:core-datastore"))
            implementation(project.dependencies.project(":domain:domain-ptt"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.material.icons.extended)
        }
        jvmTest.dependencies {
            implementation(project.dependencies.project(":core:core-testing"))
            implementation(kotlin("test"))
        }
    }
}
