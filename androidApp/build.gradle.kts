plugins {
    alias(libs.plugins.androidApplication)

    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.pttlan.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.pttlan.android"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        // Netty jars all ship these; none of them is read at runtime
        resources.excludes += listOf("META-INF/INDEX.LIST", "META-INF/io.netty.versions.properties", "META-INF/license/**")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(projects.core.coreDatastore)
    implementation(projects.core.coreDi)
    implementation(projects.core.coreNavigation)
    implementation(projects.core.coreDesignsystem)
    implementation(projects.features.featureConnection)
    implementation(projects.features.featureChannelList)
    implementation(projects.features.featurePtt)
    implementation(projects.features.featureHistory)
    implementation(projects.features.featureSettings)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.ui.tooling)
    implementation(libs.ui.tooling.preview)
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(projects.domain.domainPtt)
    implementation(projects.serverCore) {
        // HTTP/3 natives for desktop OSes: the host serves HTTPS/WSS over TCP only, and they add megabytes
        exclude(group = "io.netty", module = "netty-codec-native-quic")
    }

    implementation(libs.androidx.window)

    testImplementation(kotlin("test-junit"))
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.window.testing)

    androidTestImplementation(projects.core.coreNetwork)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
