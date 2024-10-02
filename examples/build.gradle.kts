dependencies {
    api(project(":wayland-protocols"))
    api(libs.com.github.spotbugs.spotbugs.annotations)
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    testImplementation(libs.org.junit.platform.junit.platform.launcher)
    testImplementation(libs.com.google.guava.guava)
}

description = "wayland-examples"