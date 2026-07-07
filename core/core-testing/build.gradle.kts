plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            api(kotlin("test"))
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.coroutines.test)
            api(libs.turbine)
        }
        jvmMain.dependencies {
            api(libs.mockk)
        }
    }
}
