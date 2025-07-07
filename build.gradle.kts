plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.yourdomain.curlgenie"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.javaparser:javaparser-core:3.25.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
}

intellij {
    version.set("2023.2")
    type.set("IC")
}
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks {
    patchPluginXml {
        changeNotes.set("Initial version of cURLGenie plugin.")
    }
}