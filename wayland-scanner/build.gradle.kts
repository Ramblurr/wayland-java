dependencies {
    api(libs.com.squareup.javawriter)
    api(project(":stubs-server"))
    api(project(":stubs-client"))
    api(libs.com.github.spotbugs.spotbugs)
    api(libs.com.google.guava.guava)
    api(libs.org.slf4j.slf4j.api)
    api(libs.org.slf4j.slf4j.simple)
}

description = "wayland-scanner"
