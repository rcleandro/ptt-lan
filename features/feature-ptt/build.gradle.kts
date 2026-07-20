plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(projects.core.coreDesignsystem)
            implementation(projects.core.coreAudio)
            implementation(projects.domain.domainPtt)
            implementation(projects.core.coreNetwork)
            implementation(projects.core.coreDatastore)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
        commonTest.dependencies {
            implementation(projects.core.coreTesting)
            implementation(kotlin("test"))
        }
        jvmTest.dependencies {
            implementation(libs.mockk)
        }
    }
}
