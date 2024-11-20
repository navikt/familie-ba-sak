import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion = "2.0.21"
    kotlin("jvm") version kotlinVersion

    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.jetbrains.kotlin.plugin.spring") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.jpa") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.allopen") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.noarg") version kotlinVersion
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("org.sonarqube") version "5.1.0.4882"
    id("jacoco") // Built in to gradle

    // ------------- SLSA -------------- //
    // id("org.cyclonedx.bom") version "1.10.0"
}


configurations {
    implementation.configure {
        exclude(module = "spring-boot-starter-tomcat")
        exclude("org.apache.tomcat")
    }
}

group = "no.nav.familie.ba.sak"
version = "0.0.1-SNAPSHOT"
description = "familie-ba-sak"
java.sourceCompatibility = JavaVersion.VERSION_21

repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url = uri("https://packages.confluent.io/maven")
    }
    maven {
        url = uri("https://maven.pkg.github.com/navikt/maven-release")
        credentials {
            username = System.getenv("GITHUB_USERNAME")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    api(libs.org.springframework.boot.spring.boot.starter.jetty)
    api(libs.org.springframework.boot.spring.boot.starter.web)
    api(libs.org.springframework.boot.spring.boot.starter.validation)
    api(libs.org.springframework.boot.spring.boot.starter.actuator)
    api(libs.io.micrometer.micrometer.registry.prometheus)
    api(libs.org.springframework.boot.spring.boot.starter.data.jpa)
    api(libs.org.jetbrains.kotlin.kotlin.reflect)
    api(libs.org.jetbrains.kotlin.kotlin.stdlib.jdk8)
    api(libs.com.fasterxml.jackson.module.jackson.module.kotlin)
    api(libs.org.flywaydb.flyway.core)
    api(libs.org.springframework.kafka.spring.kafka)
    api(libs.com.neovisionaries.nv.i18n)
    api(libs.com.papertrailapp.logback.syslog4j)
    api(libs.org.postgresql.postgresql)
    api(libs.io.getunleash.unleash.client.java)
    api(libs.org.yaml.snakeyaml)
    api(libs.org.eclipse.jetty.jetty.server)
    api(libs.org.apache.maven.maven.model)
    api(libs.io.sentry.sentry.spring.boot.starter.jakarta)
    api(libs.io.sentry.sentry.logback)
    api(libs.org.apache.avro.avro)
    api(libs.io.confluent.kafka.avro.serializer)
    api(libs.org.springdoc.springdoc.openapi.starter.common)
    api(libs.org.springdoc.springdoc.openapi.starter.webmvc.ui)
    api(libs.no.nav.security.token.client.spring)
    api(libs.no.nav.security.token.client.core)
    api(libs.nav.foedselsnummer.core)
    api(libs.no.nav.familie.felles.sikkerhet)
    api(libs.no.nav.familie.prosessering.core)
    api(libs.no.nav.familie.felles.log)
    api(libs.no.nav.familie.felles.leader)
    api(libs.no.nav.familie.felles.unleash)
    api(libs.no.nav.familie.felles.http.client)
    api(libs.no.nav.familie.felles.modell)
    api(libs.no.nav.familie.felles.util)
    api(libs.no.nav.familie.felles.valutakurs.klient)
    api(libs.no.nav.familie.felles.metrikker)
    api(libs.no.nav.familie.kontrakter.felles)
    api(libs.no.nav.familie.kontrakter.barnetrygd)
    api(libs.no.nav.familie.eksterne.kontrakter.bisys)
    api(libs.no.nav.familie.eksterne.kontrakter.stonadsstatistikk)
    api(libs.no.nav.familie.eksterne.kontrakter.saksstatistikk)
    api(libs.no.nav.familie.felles.familie.utbetalingsgenerator)
    api(libs.org.springframework.retry.spring.retry)
    api(libs.com.github.jsqlparser.jsqlparser)
    api(libs.org.jetbrains.kotlinx.kotlinx.coroutines.core)
    runtimeOnly(libs.org.flywaydb.flyway.database.postgresql)
    runtimeOnly(libs.org.springframework.boot.spring.boot.devtools)
    testImplementation(libs.org.testcontainers.postgresql)
    testImplementation(libs.no.nav.security.token.validation.spring.test)
    testImplementation(libs.no.nav.security.mock.oauth2.server)
    testImplementation(libs.nav.foedselsnummer.testutils)
    testImplementation(libs.org.springframework.boot.spring.boot.starter.test)
    testImplementation(libs.io.mockk.mockk.jvm)
    testImplementation(libs.org.wiremock.wiremock.standalone)
    testImplementation(libs.com.worldturner.medeia.medeia.validator.jackson)
    testImplementation(libs.io.cucumber.cucumber.java)
    testImplementation(libs.io.cucumber.cucumber.junit.platform.engine)
    testImplementation(libs.org.junit.platform.junit.platform.suite)
}

sourceSets.getByName("test") {
    java.srcDir("src/test/enhetstester/kotlin")
    java.srcDir("src/test/integrasjonstester/kotlin")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks {
    bootJar {
        archiveFileName.set("familie-ba-sak.jar")
    }
}



// tasks.check {
    // dependsOn(ktlintCheck)
// }
//
// tasks.cyclonedxBom {
//     setIncludeConfigs(listOf("runtimeClasspath"))
//     setSkipConfigs(listOf("compileClasspath", "testCompileClasspath"))
// }
//
// tasks.register<JavaExec>("ktlintFormat") {
//     group = LifecycleBasePlugin.VERIFICATION_GROUP
//     description = "Check Kotlin code style and format"
//     classpath = ktlint
//     mainClass.set("com.pinterest.ktlint.Main")
//     jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
//     // see https://pinterest.github.io/ktlint/install/cli/#command-line-usage for more information
//     args(
//         "-F",
//         "src/**/*.kt",
//     )
// }

// tasks.jacocoTestReport {
//     dependsOn(tasks.test)
//     executionData(fileTree(layout.buildDirectory).include("/jacoco/test.exec"))
//     reports {
//         xml.required = true
//     }
// }
//
// tasks.register<JacocoReport>("jacocoIntegrationTestReport") {
//     dependsOn(":integrationTest")
//     sourceSets(sourceSets.main.get())
//     executionData(fileTree(layout.buildDirectory).include("/jacoco/integrationTest.exec"))
//     reports {
//         xml.required = true
//     }
// }
//
// sonar {
//     properties {
//         property("sonar.projectKey", System.getenv("SONAR_PROJECTKEY"))
//         property("sonar.organization", "navikt")
//         property("sonar.host.url", System.getenv("SONAR_HOST_URL"))
//         property("sonar.token", System.getenv("SONAR_TOKEN"))
//         property("sonar.sources", "src/main")
//     }
// }

allprojects {
    plugins.withId("java") {
        this@allprojects.tasks {
            val test =
                "test"(Test::class) {
                    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
                    useJUnitPlatform {
                        excludeTags("integrationTest")
                    }
                    // finalizedBy(jacocoTestReport)
                }
            val integrationTest =
                register<Test>("integrationTest") {
                    useJUnitPlatform {
                        includeTags("integrationTest")
                    }
                    shouldRunAfter(test)
                    // finalizedBy(":jacocoIntegrationTestReport")
                }
            "check" {
                dependsOn(integrationTest)
            }
        }
    }
}
