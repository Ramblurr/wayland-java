# wayland-java

[![License](https://img.shields.io/:license-Apache2-blue.svg)](http://www.apache.org/licenses/LICENSE-2.0)
[![ImageBuild](https://github.com/Ramblurr/wayland-java/actions/workflows/ImageBuild.yaml/badge.svg)](https://github.com/Ramblurr/wayland-java/actions)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-examples/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland)

> Modern (JDK 22+) Java/JVM bindings for libwayland and wayland-protocols.

### What?

This repo provides several java libraries to create [Wayland][wayland] client and server applications in Java **without writing any C**.

It leverages the finalized and GA [Foreign Memory & Memory API][jep454] from JEP 454 that is available as of JDK 22 (formerly known as Project Panama).

Project status: As of Q3 2024, this project is under active development, it is not ready for production use. Testing and feedback is welcome.

### Why?

Because app developers should be able to write wayland applications that run on their favorite JVM-hosted language without mucking about with the complex mess that JNI/JNA.

This project was forked from [Erik De Rijcke][erik]'s 2015-era effort on [udevbe/wayland-java-bindings][erikwayland] and rewritten to use Project Panama for FFI.

### Get Started

#### Requirements
In addition to adding the maven dependencies to your project you'll need to have the following available at runtime to be successful:

* JDK 22+ (opendjk tested)
* running on Linux with a `libc`
* `libwayland`

#### Adding the dependency

This repo publishes several artifacts that you can add as dependencies to your favorite build tool.

You only need to choose one (usually either `wayland-server` or `wayland-client`).

Choose your artifact depending on which type of wayland application you are building:

* wayland server: [![org.freedesktop.wayland:wayland-server:XXX](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-server/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-server)
* wayland client: [![org.freedesktop.wayland:wayland-client:XXX](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-client)

### Wayland Protocols

The `wayland-client` and `wayland-server` contain protocol bindings for these wayland protocols:

* the core protocols
* `stable/xdg-shell`

If you want to use other protocols, then you will want to take advantage of the wayland-scanner from this library that will generate java at build time for the protocols.

* wayland-scanner: [![org.freedesktop.wayland:wayland-scanner:XXX](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-scanner/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.freedesktop.wayland/wayland-scanner)

Add it to your build path, no need to put it on your classpath as it will only be used during compilation.

Add a `@Protocols` annotation to your own private `package-info.java` file and set it to use your custom protocol xml file. Here's an [example](wayland-client/src/main/java/org/freedesktop/wayland/package-info.java).

Ensure your build tool is configured to run annotation processors.

Build your project. The generated bindings should automatically appear in the same package as your `package-info.java` file.

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
