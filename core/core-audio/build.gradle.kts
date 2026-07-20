plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kopus)
        }
    }
}

dependencies {
    "androidMainImplementation"(libs.androidx.core.ktx)
}
