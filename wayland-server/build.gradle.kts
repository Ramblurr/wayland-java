dependencies {
    api(project(":stubs-server"))
    api(libs.com.github.spotbugs.spotbugs)
    annotationProcessor(project(":wayland-scanner"))
}

description = "wayland-server"

//java {
//    withJavadocJar()
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(22))
//    }
//}

tasks.named<Javadoc>("javadoc") {
    source += fileTree("build/generated/sources/annotationProcessor/java/main")
}
