# wayland-java

[![license](https://img.shields.io/:license-Apache2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![CI](https://github.com/Ramblurr/wayland-java/actions/workflows/ci.yml/badge.svg)](https://github.com/Ramblurr/wayland-java/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-protocols/badge.svg)](https://mvnrepository.com/artifact/org.freedesktop.wayland)
[![Javadoc](https://img.shields.io/badge/javadoc-online-green)](https://ramblurr.github.io/wayland-java/javadoc)

> Modern (JDK 22+) Java/JVM bindings for libwayland and wayland-protocols.

### What?

This repo provides several java libraries to create [Wayland][wayland] client and server applications in Java **without writing any C**.

It leverages the finalized and GA [Foreign Memory & Memory API][jep454] from JEP 454 that is available as of JDK 22 (formerly known as Project Panama).

Project status: As of Q3 2024, this project is under active development, it is not ready for production use. Testing and feedback is welcome.

### Why?

Because app developers should be able to write wayland applications that run on their favorite JVM-hosted language without mucking about with the complex mess that JNI/JNA.

This project was forked from [Erik De Rijcke][erik]'s 2015-era effort on [udevbe/wayland-java-bindings][erikwayland] and rewritten to use Project Panama for FFI.

## Get Started

### Requirements
In addition to adding the maven dependencies to your project you'll need to have the following available at runtime to be successful:

* JDK 22+ (opendjk tested)
* running on Linux with a `libc`
* `libwayland`

### Adding the dependency

This repo publishes several artifacts that you can add as dependencies to your favorite build tool depending on your needs.

If you need the [core wayland protocol][wayland-core] and [stable wayland-protocols][wayland-protocols-stable]) then you want the `wayland-protocols` artifact. 

* `wayland-protocols`: [![org.freedesktop.wayland:wayland-protocols:XXX](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-protocols/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-protocols)

If you need a different set of wayland protocols, then see the next section.

## Wayland Protocols

This library provides [client stubs][artifact-client], [server stubs][artifact-server], and [shared stubs][artifact-shared] artifacts that do the heavy lifting.

Additionally we provide the [wayland-scanner][artifact-scanner] that generates bindings from wayland protocol XML descriptions.

The [wayland-protocols][artifact-protocols] artifact, as described above, contains a pre-chosen subset of all the available protocols. If that artifact doesn't meet your needs, then you should generate your own.

### Generate your own protocol bindings

1. Add the [![org.freedesktop.wayland:wayland-scanner:XXX](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-scanner/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-scanner) artifact to your build path, no need to put it on your classpath as it will only be used during compilation.

2. Add either the [client stubs][artifact-client] or [server stubs][artifact-server] to your classpath.
    * [![org.freedesktop.wayland:wayland-stubs-client:XXX](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-stubs-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-stubs-client)
    * [![org.freedesktop.wayland:wayland-stubs-server:XXX](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-stubs-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-stubs-server)
3. Add the appropriate `@Wayland*Protocols` annotation to your own private `package-info.java` file and configure the parameters. (see next section for more information)
4. Ensure your build tool is configured to run annotation processors.
5. Build your project.

### Which `@Wayland*Protocol` annotation?

There are three annotations you can use to generate protocol bindings. 

From low-level to high-level:

1. [`org.freedesktop.wayland.generator.api.WaylandCustomProtocols`][annotation-custom]

     ``` java
     // in src/main/java/my/domain/wayland/package-info.java
     @WaylandCustomProtocols({
             // load the foobar.xml protocol from the dir discovered via `pkg-config --variable=pkgdatadir wayland-foo`
             @WaylandCustomProtocol(path = "foo.xml", pkgConfig = "wayland-foo"),
             // load the foobar.xml protocol from an absolute path
             @WaylandCustomProtocol(path = "/my/path/to/foo.xml"),
             // load the foobar.xml protocol relative to the project directory
             @WaylandCustomProtocol(path = "fixtures/foo.xml"),
    })
     package my.domain.wayland;
     ```
     
     There are more options available, see the [javadoc][annotation-custom]
     
2. [`org.freedesktop.wayland.generator.api.WaylandProtocols`][annotation-protocols] - Load a bunch of protocols at once from a directory where each protocol xml is in its own directory (like the [wayland-protocols repo][wayland-protocols]) see [wayland-protocols/src/main/java/org/freedesktop/wayland/package-info.java][example-pkg-info] and javadoc
3. [`org.freedesktop.wayland.generator.api.WaylandCoreProtocols`][annotation-protocols-core]  - Load the core wayland protocols see [wayland-protocols/src/main/java/org/freedesktop/wayland/package-info.java][example-pkg-info] and javadoc

#### Custom protocol

The generated bindings should automatically appear in the build dir under the same package as your `package-info.java` file.

## Develop/Contribute

### Requirements

You'll need the following to build the projects in this repo:

* a modern Linux machine
* `libwayland` and `wayland-protocols` installed
* headers available
* these cli tools available on your PATH
    * `jextract`
    * `gcc`
    * `pkg-config`


If your are running Nix, then just `nix develop` and you'll have everything you need.

### Build

At the CLI a build is as easy as:

``` shell
./gradlew build
```


Or you can open the project in intellij, just choose the gradle model.

# License

```
   Copyright © 2015 Erik De Rijcke
   Copyright © 2024 Casey Link

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```

[wayland]: https://wayland.freedesktop.org/
[jep454]: https://openjdk.org/jeps/454
[erik]: https://github.com/udevbe
[erikwayland]: https://github.com/udevbe/wayland-java-bindings
[wayland-core]: https://wayland.app/protocols/wayland
[wayland-protocols]: https://gitlab.freedesktop.org/wayland/wayland-protocols
[wayland-protocols-stable]: https://gitlab.freedesktop.org/wayland/wayland-protocols/-/tree/main/stable?ref_type=heads
[artifact-client]: https://mvnrepository.com/artifact/org.freedesktop.wayland/stubs-client
[artifact-server]: https://mvnrepository.com/artifact/org.freedesktop.wayland/stubs-server
[artifact-shared]: https://mvnrepository.com/artifact/org.freedesktop.wayland/stubs-shared
[artifact-scanner]: https://mvnrepository.com/artifact/org.freedesktop.wayland/wayland-scanner
[artifact-protocols]: https://mvnrepository.com/artifact/org.freedesktop.wayland/wayland-protocols
[annotation-custom]: https://ramblurr.github.io/wayland-java/javadoc/current/org/freedesktop/wayland/generator/api/WaylandCustomProtocols.html
[annotation-protocols]: https://ramblurr.github.io/wayland-java/javadoc/current/org/freedesktop/wayland/generator/api/WaylandProtocols.html
[annotation-protocols-core]: https://ramblurr.github.io/wayland-java/javadoc/current/org/freedesktop/wayland/generator/api/WaylandCoreProtocols.html
[example-pkg-info]: ./wayland-protocols/src/main/java/org/freedesktop/wayland/package-info.java
