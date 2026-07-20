plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(projects.core.coreDesignsystem)
            implementation(projects.core.coreDatastore)
            implementation(projects.domain.domainPtt)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.material.icons.extended)
        }
        jvmTest.dependencies {
            implementation(projects.core.coreTesting)
            implementation(kotlin("test"))
        }
    }
}
