import org.gradle.api.tasks.testing.logging.TestLogEvent
import java.io.ByteArrayOutputStream

val version = "2.0.0"
val suffix = "SNAPSHOT"

// Strings embedded into the build.
var gitRevision by extra("")
var waylandJavaVersion by extra("")


val gitDescribe: String? by lazy {
    val stdout = ByteArrayOutputStream()
    try {
        rootProject.exec {
            commandLine("git", "describe", "--tags")
            errorOutput = null
            standardOutput = stdout
        }
        stdout.toString().trim().replace("-g", "-")
    } catch (e: Exception) {
        null
    }
}

val gitBranch: String? by lazy {
    val stdout = ByteArrayOutputStream()
    try {
        rootProject.exec {
            commandLine("git", "rev-parse", "--abbrev-ref", "HEAD")
            standardOutput = stdout
        }
        stdout.toString().trim()
    } catch (e: Exception) {
        null
    }
}

if ("release" !in gradle.startParameter.taskNames) {
    val hash = this.gitDescribe

    if (hash == null) {
        gitRevision = "dirty"
        waylandJavaVersion = "$version-dirty"
        project.logger.lifecycle("Building SNAPSHOT (no .git folder found)")
    } else {
        gitRevision = hash
        waylandJavaVersion = "$hash-SNAPSHOT"
        project.logger.lifecycle("Building SNAPSHOT ($gitBranch): $gitRevision")
    }
} else {
    gitRevision = ""
    waylandJavaVersion = if (suffix.isNotEmpty()) "$version-$suffix" else version;
    project.logger.lifecycle("Building RELEASE ($gitBranch): $waylandJavaVersion")
}


plugins {
    idea
    java
    `java-library`
    `maven-publish`
}
allprojects {
    group = "org.freedesktop"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
        mavenLocal()
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
    val mavenProjects = arrayOf("wayland-client", "wayland-server", "wayland-scanner", "stubs-shared", "wayland-native", "stubs-client", "stubs-server")
    if (project.name in mavenProjects) {
        apply(plugin = "maven-publish")
//        apply(plugin = "signing")

        java {
            withJavadocJar()
            withSourcesJar()
        }
        publishing {
            publications {
                create<MavenPublication>(project.name) {
                    from(components["java"])
                }
                register("mavenJava", MavenPublication::class) {
                    from(components["java"])
                    groupId = "org.freedesktop"
                    artifactId = project.name
                    version = waylandJavaVersion

                    pom {
                        name = project.name
                        description = project.description
                        url = "https://github.com/ramblurr/wayland-java"
                        licenses {
                            license {
                                name = "The Apache License 2.0"
                                url = "https://opensource.org/licenses/Apache-2.0"
                            }
                        }
                        developers {
                            developer {
                                id = "Zubnix"
                                name = "Erik De Rijcke"
                                email = "DeRijcke.Erik@gmail.com"
                            }
                            developer {
                                id = "ramblurr"
                                name = "Casey Link"
                                email = "casey@outskirtslabs.com"
                            }
                        }
                        scm {
                            connection = "scm:git:git://github.com/ramblurr/wayland-java.git"
                            developerConnection = "scm:git:git@github.com:ramblurr/wayland-java.git"
                            url = "https://github.com/ramblurr/wayland-java"
                        }
                    }
                }
            }
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