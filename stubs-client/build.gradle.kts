dependencies {
    api(project(":stubs-shared"))
    api(project(":wayland-native"))
    api(libs.org.slf4j.slf4j.api)
    api(libs.com.kohlschutter.junixsocket.junixsocket.core)
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    testImplementation(libs.org.junit.platform.junit.platform.launcher)
    testImplementation(libs.org.slf4j.slf4j.simple)
}

description = "Client stubs for generated wayland bindings"
