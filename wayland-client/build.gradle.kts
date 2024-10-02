dependencies {
    api(project(":stubs-client"))
    api(libs.com.github.spotbugs.spotbugs)
    annotationProcessor(project(":wayland-scanner"))
}

description = "wayland-client"

tasks.named<Javadoc>("javadoc") {
    source += fileTree("build/generated/sources/annotationProcessor/java/main")
}

