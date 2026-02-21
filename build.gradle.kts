plugins {
    kotlin("jvm") version "2.3.10"
    kotlin("plugin.spring") version "2.3.10"
    id("org.springframework.boot") version "4.0.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.netflix.dgs.codegen") version "8.3.0"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "kr.io.team"
version = "0.0.1-SNAPSHOT"
description = "server"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
    all {
        resolutionStrategy {
            force("commons-fileupload:commons-fileupload:1.6.0")
        }
    }
}

repositories {
    mavenCentral()
}

extra["netflixDgsVersion"] = "11.0.0"
extra["springCloudVersion"] = "2025.1.0"
extra["exposedVersion"] = "1.0.0"
extra["kotestVersion"] = "6.1.2"
extra["mockkVersion"] = "1.14.9"

dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-h2console")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("tools.jackson.module:jackson-module-kotlin")

    // Exposed ORM
    implementation("org.jetbrains.exposed:exposed-spring-boot4-starter:${property("exposedVersion")}")
    implementation("org.jetbrains.exposed:exposed-kotlin-datetime:${property("exposedVersion")}")
    implementation("org.jetbrains.exposed:exposed-json:${property("exposedVersion")}")

    // Database
    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    // Monitoring
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    // Test - Spring Boot
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("com.netflix.graphql.dgs:graphql-dgs-spring-graphql-starter-test")

    // Test - Kotest
    testImplementation("io.kotest:kotest-runner-junit5:${property("kotestVersion")}")
    testImplementation("io.kotest:kotest-assertions-core:${property("kotestVersion")}")
    testImplementation("io.kotest:kotest-extensions-spring:${property("kotestVersion")}")

    // Test - MockK
    testImplementation("io.mockk:mockk:${property("mockkVersion")}")
    testImplementation("com.ninja-squad:springmockk:5.0.1")

    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
    imports {
        mavenBom("com.netflix.graphql.dgs:graphql-dgs-platform-dependencies:${property("netflixDgsVersion")}")
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

tasks.generateJava {
    schemaPaths.add("$projectDir/src/main/resources/graphql-client")
    packageName = "kr.io.team.loop.codegen"
    generateClient = true
}

ktlint {
    version = "1.8.0"
}

tasks.withType<Test> {
    useJUnitPlatform()
}
