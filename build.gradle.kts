plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.ebook.reader"
version = "1.0.0"

repositories {
    mavenLocal()
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    mavenCentral()
}

dependencies {
    implementation("io.documentnode:epub4j-core:4.2.3")
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
        untilBuild.set("253.*")
    }
    buildSearchableOptions {
        enabled = false
    }
}
