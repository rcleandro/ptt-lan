plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.domain.domainPtt)
            implementation(projects.core.coreDesignsystem)
            implementation(projects.core.coreCommon)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.material.icons.extended)
        }
        jvmTest.dependencies {
            implementation(projects.core.coreTesting)
            implementation(kotlin("test"))
        }
    }
}
