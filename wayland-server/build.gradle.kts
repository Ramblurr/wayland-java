dependencies {
    api(project(":stubs-server"))
    api(libs.com.github.spotbugs.spotbugs.annotations)
    annotationProcessor(project(":wayland-scanner"))
}

description = "Java bindings for wayland servers"

tasks.named<Javadoc>("javadoc") {
    source += fileTree("build/generated/sources/annotationProcessor/java/main")
}
