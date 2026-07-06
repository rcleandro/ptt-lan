import urllib.request
import json
import re

libraries = {
    "agp": ("com.android.tools.build", "gradle"),
    "kotlin": ("org.jetbrains.kotlin", "kotlin-gradle-plugin"),
    "compose": ("org.jetbrains.compose", "compose-gradle-plugin"),
    "ktor": ("io.ktor", "ktor-client-core"),
    "sqldelight": ("app.cash.sqldelight", "gradle-plugin"),
    "koin": ("io.insert-koin", "koin-core"),
    "decompose": ("com.arkivanov.decompose", "decompose"),
    "multiplatformSettings": ("com.russhwolf", "multiplatform-settings"),
    "kermit": ("co.touchlab", "kermit"),
    "coroutines": ("org.jetbrains.kotlinx", "kotlinx-coroutines-core"),
    "serialization": ("org.jetbrains.kotlinx", "kotlinx-serialization-core"),
    "detekt": ("io.gitlab.arturbosch.detekt", "detekt-gradle-plugin"),
    "ktlint": ("org.jlleitschuh.gradle", "ktlint-gradle"),
    "kover": ("org.jetbrains.kotlinx", "kover-gradle-plugin")
}

def get_latest_version(group, artifact):
    url = f"https://search.maven.org/solrsearch/select?q=g:%22{group}%22+AND+a:%22{artifact}%22&rows=1&wt=json"
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
        with urllib.request.urlopen(req) as response:
            data = json.loads(response.read().decode())
            if data['response']['numFound'] > 0:
                return data['response']['docs'][0]['latestVersion']
    except Exception as e:
        print(f"Error fetching {group}:{artifact} - {e}")
    return "UNKNOWN"

for name, (group, artifact) in libraries.items():
    version = get_latest_version(group, artifact)
    print(f"{name} = \"{version}\"")
