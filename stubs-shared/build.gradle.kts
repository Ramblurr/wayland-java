dependencies {
    api(libs.org.slf4j.slf4j.api)
    api(project(":jextract"))
    testImplementation(libs.org.junit.jupiter.junit.jupiter)
    testImplementation(libs.org.junit.platform.junit.platform.launcher)
    testImplementation(libs.org.slf4j.slf4j.simple)
}

description = "stubs-shared"

//java {
//    withJavadocJar()
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(22))
//    }
//}