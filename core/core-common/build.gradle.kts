plugins {
    id("ptt.kmp.library")
}

kotlin {
    android {
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}

dependencies {
    "androidMainImplementation"(libs.androidx.core.ktx)
    "androidHostTestImplementation"(kotlin("test"))
    "androidHostTestImplementation"(libs.robolectric)
    "androidHostTestImplementation"(libs.androidx.junit)
}
