plugins {
    id("ptt.kmp.library")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":domain:domain-ptt"))
            implementation(project.dependencies.project(":core:core-common"))
            implementation(project.dependencies.project(":core:core-network"))
            implementation(project.dependencies.project(":core:core-database"))
            implementation(project.dependencies.project(":core:core-audio"))
            implementation(project.dependencies.project(":core:core-datastore"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
            implementation(libs.kotlinx.datetime)
            implementation(libs.okio)
        }
    }
}
