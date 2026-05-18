plugins {
    java
    checkstyle
    id("org.springframework.boot") version "4.0.5"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.aquilaoffice"
version = "0.0.1-SNAPSHOT"
description = "aquila-office-back"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

checkstyle {
    toolVersion = "10.23.1"
    configDirectory.set(layout.projectDirectory.dir("config/checkstyle"))
    isIgnoreFailures = false
    maxWarnings = 0
}

tasks.withType<org.gradle.api.plugins.quality.Checkstyle>().configureEach {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
