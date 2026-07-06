plugins {
    id("ptt.kmp.library")
    id("ptt.compose.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-common"))
            api(compose.ui)
            api(compose.foundation)
            api(compose.material3)
            api(compose.runtime)
        }
    }
}
