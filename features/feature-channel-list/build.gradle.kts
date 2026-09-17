plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(projects.core.coreDesignsystem)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.material.icons.extended)
            implementation(projects.domain.domainPtt)

            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.turbine)
            implementation(libs.mockk)
            implementation(kotlin("test"))
        }
    }
}
