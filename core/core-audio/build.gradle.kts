plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
        }
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
