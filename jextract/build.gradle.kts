import java.io.ByteArrayOutputStream
description = "jextract generated native wrappers for wayland-java"

//java {
//    withJavadocJar()
//    toolchain {
//        languageVersion.set(JavaLanguageVersion.of(22))
//    }
//}

tasks.named<Javadoc>("javadoc") {
    source += fileTree("build/generated/sources/jextract/java/main")
}

tasks.compileJava {
    dependsOn(
        "jextract"
    )
}

fun systemIncludes(): List<String> {
    val raw = ByteArrayOutputStream().apply {
        exec {
            commandLine("pkg-config", "--variable=pkgdatadir", name)
            commandLine("gcc", "-E", "-Wp,-v", "-xc", "/dev/null")
            //standardOutput = this@apply
            standardOutput = ByteArrayOutputStream()
            errorOutput = this@apply
        }
    }.toString().trim()
    return raw.lineSequence()
        .dropWhile { it != "#include <...> search starts here:" }
        .drop(1)
        .takeWhile { it != "End of search list." }
        .map { it.trim() }
        .toList()
}

val jextractOutput = "build/generated/sources/jextract/java/main"

fun buildJextractArgs(): List<String> {
    val includes = systemIncludes().flatMap { listOf("--include-dir", it) }
    val libraries = listOf(
        "wayland-server",
        "wayland-client"
    ).flatMap { listOf("--library", it) }
    val args = arrayOf(
        "--output", jextractOutput,
        "--target-package", "org.freedesktop.wayland",
        "--header-class-name", "C",
    )
    val headers = listOf(
        /*
        "<assert.h>",
        "<getopt.h>",
        "<stdbool.h>",
        "<time.h>",
        */
        "<stdio.h>",
        "<unistd.h>",
        "<fcntl.h>",
        "<sys/mman.h>",
        "<unistd.h>",
        "<stdlib.h>",
        "<signal.h>",
        "<wayland-util.h>",
        "<wayland-server-core.h>",
        "<wayland-server-protocol.h>",
        "<wayland-client-core.h>",
        "<wayland-client-protocol.h>",
    )
    return includes + args + libraries + headers
}

// TODO get this dynamically
val jextractBinary = "/nix/store/3yydk26390abzmfcahwqz6h5lqrp6bld-jextract-unstable-2024-09-27/bin/jextract"

task<Exec>("jextract") {
    commandLine(listOf(jextractBinary) + buildJextractArgs())
}
sourceSets["main"].java.srcDir(file(jextractOutput))
sourceSets["main"].compileClasspath += files(file(jextractOutput))
sourceSets["main"].runtimeClasspath += files(file(jextractOutput))
