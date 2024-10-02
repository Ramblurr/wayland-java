rootProject.name = "wayland-java-bindings"

include(":wayland-examples")
include(":wayland-scanner")
include(":stubs-shared")
include(":wayland-client")
include(":wayland-server")
include(":stubs-server")
include(":stubs-client")
include(":jextract")

project(":wayland-examples").projectDir = file("examples")
