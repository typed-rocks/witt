plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij.platform") version "2.0.0"
}

group = "typed.rocks"
version = "1.0.8"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}


intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "243"
            untilBuild = "243.*"
        }
    }

}
dependencies {
    intellijPlatform {
        create("IU", "2024.3.1")
        bundledPlugins(listOf("com.intellij.java", "JavaScript", "org.jetbrains.plugins.vue"))
        version = "1.0.8"
        instrumentationTools()

    }
    testImplementation(kotlin("script-runtime"))
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
}

