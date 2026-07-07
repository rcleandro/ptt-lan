plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":core:core-datastore"))
            implementation(project(":domain:domain-ptt"))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
        jvmTest.dependencies {
            implementation(project(":core:core-testing"))
            implementation(kotlin("test"))
        }
    }
}
