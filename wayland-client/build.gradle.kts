dependencies {
    api(project(":stubs-client"))
    api(libs.com.github.spotbugs.spotbugs)
    annotationProcessor(project(":wayland-scanner"))
}

description = "wayland-client"

//java {
//    withJavadocJar()
//
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(22))
//    }
//}

tasks.named<Javadoc>("javadoc") {
    source += fileTree("build/generated/sources/annotationProcessor/java/main")
}

