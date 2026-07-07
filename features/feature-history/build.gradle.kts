plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":domain:domain-ptt"))
            implementation(project(":core:core-designsystem"))
            implementation(project(":core:core-common"))
            api(libs.decompose)
            implementation(libs.decompose.extensions.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.material.icons.extended)
        }
        jvmTest.dependencies {
            implementation(project(":core:core-testing"))
            implementation(kotlin("test"))
        }
    }
}
