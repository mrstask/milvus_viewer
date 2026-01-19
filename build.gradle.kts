plugins {
    id("org.jetbrains.intellij.platform") version "2.9.0"
    kotlin("jvm") version "1.9.24"
}

group = "dev.potik"
version = "1.0.1"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    implementation("io.milvus:milvus-sdk-java:2.4.8")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.17.2")
    
    intellijPlatform {
        pycharmProfessional("2024.2")
        pluginVerifier()
        zipSigner()
    }
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

intellijPlatform {
    buildSearchableOptions = false
    instrumentCode = false
    
    pluginVerification {
        ides {
            recommended()
        }
    }
    
    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        channels = listOf("Stable")
    }
}

tasks {
    patchPluginXml {
        sinceBuild.set("242")
        untilBuild.set("252.*")
    }
    
    publishPlugin {
        dependsOn("patchPluginXml")
    }
}
