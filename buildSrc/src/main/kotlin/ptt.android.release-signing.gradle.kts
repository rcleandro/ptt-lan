import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

// The phone and the watch apps share one applicationId, so Wear OS only pairs them when both carry the same key.
// keystore.properties (root, not committed) holds storeFile, storePassword, keyAlias and keyPassword; without it,
// release falls back to the debug key so the variant still builds and installs.
val keystoreFile = rootProject.file("keystore.properties")

extensions.configure<ApplicationExtension> {
    buildTypes.getByName("release").signingConfig =
        if (keystoreFile.exists()) {
            val props = Properties().apply { keystoreFile.inputStream().use(::load) }
            signingConfigs.create("release") {
                storeFile = rootProject.file(props.getProperty("storeFile"))
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        } else {
            logger.warn("keystore.properties not found: ${project.path} release is signed with the debug key")
            signingConfigs.getByName("debug")
        }
}
