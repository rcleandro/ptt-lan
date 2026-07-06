import os

modules = [
    "core/core-common",
    "core/core-network",
    "core/core-audio",
    "core/core-database",
    "core/core-datastore",
    "core/core-di",
    "core/core-navigation",
    "core/core-designsystem",
    "core/core-telemetry",
    "core/core-testing",
    "domain/domain-ptt",
    "data/data-ptt",
    "features/feature-connection",
    "features/feature-channel-list",
    "features/feature-ptt",
    "features/feature-history",
    "features/feature-settings",
    "features/feature-admin-web",
    "shared"
]

kmp_build_gradle = """plugins {
    id("ptt.kmp.library")
}
"""

for mod in modules:
    path = os.path.join(mod, "build.gradle.kts")
    os.makedirs(mod, exist_ok=True)
    with open(path, "w") as f:
        f.write(kmp_build_gradle)

# For apps, just create empty for now
apps = ["androidApp", "desktopApp", "iosApp", "serverApp"]
for app in apps:
    path = os.path.join(app, "build.gradle.kts")
    os.makedirs(app, exist_ok=True)
    with open(path, "w") as f:
        f.write("// App module\n")
