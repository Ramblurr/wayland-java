dependencies {
    api(project(":wayland-client"))
    api(libs.com.github.spotbugs.spotbugs)
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    testImplementation(libs.org.junit.platform.junit.platform.launcher)
    testImplementation(libs.com.google.guava.guava)
}

description = "wayland-examples"

java {
    //withJavadocJar()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}