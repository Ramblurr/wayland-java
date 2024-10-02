plugins {
    id("io.freefair.aggregate-javadoc") version "8.10"
//    id("io.freefair.javadoc-links") version "8.10"
}

dependencies {
    javadoc(project(":wayland-native"))
    javadoc(project(":stubs-shared"))
    javadoc(project(":wayland-scanner"))
    javadoc(project(":stubs-client"))
    javadoc(project(":stubs-server"))
    javadoc(project(":wayland-protocols"))
    javadoc(project(":examples"))

//    rootProject.subprojects.forEach { subproject ->
//        subproject.plugins.withId("java") {
//            javadoc(subproject)
//        }
//    }
    javadoc(fileTree(project(":wayland-native").dependencyProject.layout.buildDirectory.dir("generated/sources/jextract/java/main")))
    javadoc(fileTree(project(":wayland-protocols").dependencyProject.layout.buildDirectory.dir("generated/sources/annotationProcessor/java/main")))
}


tasks.withType<Javadoc>() {
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}
