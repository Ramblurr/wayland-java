dependencies {
    api(project(":stubs-client"))
    api(libs.com.github.spotbugs.spotbugs.annotations)
    annotationProcessor(project(":wayland-scanner"))
}

description = "Java bindings for wayland clients"

tasks.named<Javadoc>("javadoc") {
    source += fileTree("build/generated/sources/annotationProcessor/java/main")
}

