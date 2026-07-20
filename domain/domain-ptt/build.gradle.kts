plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
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
