plugins {
    id("ptt.kmp.library")
    alias(libs.plugins.sqldelight)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.project(":core:core-common"))
            api(libs.sqldelight.coroutines)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native)
        }
    }
}

dependencies {
    "androidMainImplementation"(libs.sqldelight.android)
}

sqldelight {
    databases {
        create("PttDatabase") {
            packageName.set("com.pttlan.core.database")
        }
    }
}
