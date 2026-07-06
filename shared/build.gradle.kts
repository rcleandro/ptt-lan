plugins {
    id("ptt.kmp.library")
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":core:core-di"))
            implementation(project(":core:core-designsystem"))
        }
    }
}
