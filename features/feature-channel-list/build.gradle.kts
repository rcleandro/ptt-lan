plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":core:core-navigation"))
            implementation(project(":core:core-di"))
            implementation(project(":domain:domain-ptt"))
            
            implementation(libs.decompose)
            implementation(libs.decompose.extensions.compose)
        }
    }
}
