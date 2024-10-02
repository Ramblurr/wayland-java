import org.apache.tools.ant.taskdefs.condition.Os
import java.io.ByteArrayOutputStream
import java.io.FileOutputStream

description = "jextract generated native wrappers for libwayland and parts of libc"

tasks.named<Javadoc>("javadoc") {
    source += fileTree("build/generated/sources/jextract/java/main")
}

tasks.compileJava {
    dependsOn("jextract")
}

fun isFoundInPath(file: String): Boolean {
    val pathEnv = System.getenv("PATH")
    val fileFound = pathEnv?.split(File.pathSeparator)?.find { folder ->
        if (File(folder, file).exists()) {
            true
        } else {
            false
        }
    }
    return fileFound != null
}

fun checkOS() {
    if (!Os.isFamily(Os.FAMILY_UNIX)) {
        logger.warn("! WARNING ! Building wayland-java is not supported on non-Linux machines.")
        logger.warn("            I have no idea what will happen.")
    }
}

fun assertCliToolsExist() {
    val msg =
        "The '%s' command could not be found in your PATH. Please refer to the wayland-java documentation for more information.";
    val tools = listOf(
        "jextract",
        "pkg-config",
        "gcc"
    )
    for (tool in tools) {
        if (!isFoundInPath(tool)) {
            throw GradleException(String.format(msg, tool))
        }
    }
}

// automatically detects the paths to the headers we need for non-wayland system deps
fun systemIncludes(): List<String> {
    val raw = ByteArrayOutputStream().apply {
        exec {
            commandLine("pkg-config", "--variable=pkgdatadir", name)
            commandLine("gcc", "-E", "-Wp,-v", "-xc", "/dev/null")
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
fun jextractArgsLibC(): List<String> {
    val includes = systemIncludes().flatMap { listOf("--include-dir", it) }
    val args = arrayOf(
        "--output", jextractOutput,
        "--target-package", "org.freedesktop.wayland.raw",
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
    )
    return includes + args + headers
}

fun jextractArgsWayland(): List<String> {
    val includes = systemIncludes().flatMap { listOf("--include-dir", it) }
    val libraries = listOf(
        "wayland-server",
        "wayland-client"
    ).flatMap { listOf("--library", it) }

    val args = arrayOf(
        "--output", jextractOutput,
        "--target-package", "org.freedesktop.wayland.raw",
        "--header-class-name", "LibWayland",
    )
    val headers = listOf(
        "<wayland-util.h>",
        "<wayland-server-core.h>",
        "<wayland-server-protocol.h>",
        "<wayland-client-core.h>",
        "<wayland-client-protocol.h>",
    )
    return includes + args + libraries + headers
}

fun fmtCommandLineArgs(args: List<String>): String {
    return args.joinToString(separator = " ", transform = {
        if (it.startsWith("--"))
            "$it"
        else
            "\"$it\""
    })
}

fun jextract(cwd: File, args: List<String>, logPath: File): ExecResult {
    val res = exec {
        workingDir = cwd
        commandLine(listOf("jextract") + args)
        standardOutput = FileOutputStream(logPath)
        errorOutput = standardOutput
        isIgnoreExitValue = true
    }
    if (res.exitValue != 0) {
        throw GradleException(
            "jextract execution failed. build cannot continue. the jextract command ran was:\n\n" +
                    "    jextract ${fmtCommandLineArgs(args)}\n\n" +
                    "This command was run from the working directory $cwd\n" +
                    "Logs from jextract's output can be found at $logPath\n"
        )
    }
    return res
}

// the meat of all this: run the jextract tool with the calculated arguments
task("jextract") {
    val jextractLogDir = layout.buildDirectory
    val workingDir = layout.projectDirectory.asFile
    layout.buildDirectory.get().asFile.mkdirs()
    checkOS()
    assertCliToolsExist()
    jextract(workingDir, jextractArgsLibC(), jextractLogDir.file("jextract-libc.log").get().asFile)
    jextract(workingDir, jextractArgsWayland(), jextractLogDir.file("jextract-libwayland.log").get().asFile)
}

// ensure the jextract generated files are added to the classpath
sourceSets["main"].java.srcDir(file(jextractOutput))
sourceSets["main"].compileClasspath += files(file(jextractOutput))
sourceSets["main"].runtimeClasspath += files(file(jextractOutput))
