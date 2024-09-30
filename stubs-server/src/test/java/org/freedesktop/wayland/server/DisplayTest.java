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

package org.freedesktop.wayland.server;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DisplayTest {

    @Test
    public void createDisplay() {
        Display display = Display.create();
        String name = display.addSocketAuto();
        Assertions.assertTrue(name.startsWith("wayland-"));
        System.out.println("Running wayland display on " + name);
        display.destroy();
    }

    @Test
    public void createNamedDisplay() {
        System.out.println(System.getProperty("java.library.path"));
        Display display = Display.create();
        var ret = display.addSocket("wayland-9");
        Assertions.assertEquals(0, ret);
        display.destroy();
    }

}