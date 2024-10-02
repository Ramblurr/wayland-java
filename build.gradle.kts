import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    idea
    java
    `java-library`
}
allprojects {
    group = "org.freedesktop"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenLocal()
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")
    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(22))
        }
    }
    val mavenProjects = arrayOf("wayland-client", "wayland-server", "wayland-scanner")
    if (project.name in mavenProjects) {
//        apply(plugin = "maven-publish")
//        apply(plugin = "signing")

        java {
            withJavadocJar()
            withSourcesJar()
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }

    tasks.withType<Javadoc>() {
        (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}