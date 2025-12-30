import nu.studer.gradle.jooq.JooqEdition

plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("plugin.jpa") version "2.2.21"
    id("nu.studer.jooq") version "10.2"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

val jooqVersion = "3.20.5"
extra["jooq.version"] = jooqVersion

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-client")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("tools.jackson.module:jackson-module-kotlin")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-redis-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-oauth2-client-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    runtimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
    // spring doc
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.9")
    // ✅ jOOQ Codegen (DDLDatabase용)
    jooqGenerator("org.jooq:jooq-codegen:$jooqVersion")
    jooqGenerator("org.jooq:jooq-meta:$jooqVersion")
    jooqGenerator("org.jooq:jooq-meta-extensions:$jooqVersion")
    jooqGenerator("com.h2database:h2:2.2.224")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

jooq {
    edition.set(JooqEdition.OSS)
    configurations {
        create("main") {
            jooqConfiguration.apply {

                // ✅ DB 연결 안 씀 (DDLDatabase라 jdbc 설정 불필요)
                generator.apply {
                    name = "org.jooq.codegen.DefaultGenerator"

                    database.apply {
                        // ✅ Flyway SQL로부터 스키마 구성
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"

                        properties.add(org.jooq.meta.jaxb.Property().withKey("scripts").withValue("src/main/resources/db/migration/*.sql"))
                        properties.add(org.jooq.meta.jaxb.Property().withKey("sort").withValue("flyway"))
                        properties.add(org.jooq.meta.jaxb.Property().withKey("defaultNameCase").withValue("lower"))

                        // flyway 히스토리 제외(DDLDatabase로 생성 시 생기진 않지만 안전하게)
                        excludes = "flyway_schema_history"
                    }

                    target.apply {
                        packageName = "com.blog.jooq"
                        directory = "build/generated-src/jooq/main"
                    }

                    generate.apply {
                        isDaos = true
                        isRecords = true
                        isFluentSetters = true
                    }
                }
            }
        }
    }
}

sourceSets {
    main {
        java.srcDir("build/generated-src/jooq/main")
    }
}
