plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.coreCommon)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kopus)
        }
    }
}

dependencies {
    "androidMainImplementation"(libs.androidx.core.ktx)
}
