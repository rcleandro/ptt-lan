plugins {
    id("ptt.kmp.library")
}

kotlin {
    jvm()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-di"))
            implementation(project(":core:core-designsystem"))
        }
    }
}
