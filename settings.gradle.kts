@file:Suppress("UnstableApiUsage")
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ptt-lan"

include(":androidApp")
include(":desktopApp")
include(":iosApp")
include(":serverApp")
include(":shared")

include(":core:core-common")
include(":core:core-network")
include(":core:core-audio")
include(":core:core-database")
include(":core:core-datastore")
include(":core:core-di")
include(":core:core-navigation")
include(":core:core-designsystem")
include(":core:core-telemetry")
include(":core:core-testing")

include(":domain:domain-ptt")

include(":data:data-ptt")

include(":features:feature-connection")
include(":features:feature-channel-list")
include(":features:feature-ptt")
include(":features:feature-history")
include(":features:feature-settings")
include(":features:feature-admin-web")
