plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
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
