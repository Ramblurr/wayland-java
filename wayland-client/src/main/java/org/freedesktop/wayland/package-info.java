/*
 * Copyright © 2015 Erik De Rijcke
 * Copyright © 2024 Casey Link
 *
 * Licensed under the Apache License,Version2.0(the"License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,software
 * distributed under the License is distributed on an"AS IS"BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */
@Protocols({
        @Protocol(path = "wayland.xml",
                pkgConfig = "wayland-scanner",
                generateServer = false),
        @Protocol(path = "stable/xdg-shell/xdg-shell.xml",
                pkgConfig = "wayland-protocols",
                generateServer = false),
        @Protocol(path = "stable/linux-dmabuf/linux-dmabuf-v1.xml",
                pkgConfig = "wayland-protocols",
                generateServer = false),
        @Protocol(path = "stable/presentation-time/presentation-time.xml",
                pkgConfig = "wayland-protocols",
                generateServer = false),
        @Protocol(path = "stable/tablet/tablet-v2.xml",
                pkgConfig = "wayland-protocols",
                generateServer = false),
        @Protocol(path = "stable/viewporter/viewporter.xml",
                pkgConfig = "wayland-protocols",
                generateServer = false),
})
package org.freedesktop.wayland;

import org.freedesktop.wayland.generator.api.Protocol;
import org.freedesktop.wayland.generator.api.Protocols;