dependencies {
    api(libs.com.squareup.javawriter)
    api(project(":stubs-server"))
    api(project(":stubs-client"))
    api(libs.com.github.spotbugs.spotbugs.annotations)
    api(libs.org.slf4j.slf4j.api)
    api(libs.org.slf4j.slf4j.simple)
}

description = "An annotation processor for generating java bindings from wayland-protocols ala wayland-scanner"
