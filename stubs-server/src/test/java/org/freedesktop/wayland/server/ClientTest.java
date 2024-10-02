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
import org.newsclub.net.unix.AFUNIXSocketChannel;
import org.newsclub.net.unix.AFUNIXSocketPair;
import org.newsclub.net.unix.FileDescriptorCast;

import java.io.IOException;

class ClientTest {
    private int a = 0;
    private int b = 0;

    @Test
    public void client_destroy_listener() throws IOException, NoSuchFieldException, IllegalAccessException {
        AFUNIXSocketPair<AFUNIXSocketChannel> pair = AFUNIXSocketPair.open();
        var s1 = pair.getFirst();
        var s2 = pair.getSecond();

        this.a = 0;
        this.b = 0;

        Display display = Display.create();
        int fd = FileDescriptorCast.using(s1.getFileDescriptor()).as(Integer.class);
        Client client = Client.create(display, fd);

        Listener destroyListenerA = new Listener() {
            @Override
            public void handle() {
                a++;
            }
        };
        Listener destroyListenerB = new Listener() {
            @Override
            public void handle() {
                b++;
            }
        };

        client.addDestroyListener(destroyListenerA);
        client.addDestroyListener(destroyListenerB);

        destroyListenerA.remove();

        client.destroy();

        Assertions.assertEquals(0, a);
        Assertions.assertEquals(1, b);
    }

}