plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.1.0"
    id("com.gradleup.shadow") version "9.4.1"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly(files("libs/api-1.0.7.jar"))

    // OnixEvents для @EventHandler
    compileOnly ("com.github.OnixNine:OnixEvents:c54a0de95a")

    implementation("org.xerial:sqlite-jdbc:3.47.0.0")
    implementation("org.postgresql:postgresql:42.7.4") {
        exclude(group = "org.checkerframework", module = "checker-qual")
    }
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion("1.18.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }
    shadowJar {
        archiveClassifier.set("")
        relocate("org.sqlite", "dev.ersa.history.libs.sqlite")
        relocate("org.postgresql", "dev.ersa.history.libs.postgresql")
        exclude("META-INF/services/java.sql.Driver")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
