dependencies {
    api(libs.org.slf4j.slf4j.api)
    api(project(":wayland-native"))
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    testImplementation(libs.org.junit.platform.junit.platform.launcher)
    testImplementation(libs.org.slf4j.slf4j.simple)
}

description = "stubs-shared"