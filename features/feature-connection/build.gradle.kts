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
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(projects.core.coreDatastore)
        }
        jvmTest.dependencies {
            implementation(projects.core.coreTesting)
            implementation(kotlin("test"))
            implementation(libs.multiplatform.settings.test)
        }
    }
}
