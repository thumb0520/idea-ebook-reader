plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.15.0"
}

group = "com.ebook.reader"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.documentnode:epublib:4.0") {
        exclude(group = "org.slf4j", module = "slf4j-api")
        exclude(group = "xmlpull", module = "xmlpull")
    }
    implementation("org.jsoup:jsoup:1.16.1")
}

intellij {
    version.set("2023.2")
    type.set("IC")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }
    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("242.*")
    }
    buildSearchableOptions {
        enabled = false
    }
}
