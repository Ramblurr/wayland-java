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

package org.freedesktop.wayland.examples;

import org.freedesktop.wayland.client.WlBufferProxy;
import org.freedesktop.wayland.client.WlShmPoolProxy;
import org.freedesktop.wayland.shared.WlShmFormat;
import org.freedesktop.wayland.util.ShmPool;

import java.io.IOException;

public class BufferPoolFactory {

    private final Display display;

    public BufferPoolFactory(Display display) {
        this.display = display;
    }

    public BufferPool create(int width,
                             int height,
                             int size,
                             WlShmFormat shmFormat) throws IOException {

        final BufferPool bufferPool = new BufferPool();
        for (int i = 0; i < size; i++) {
            final int bufferSize = width * height * 4;
            final ShmPool shmPool = new ShmPool(bufferSize);

            final WlShmPoolProxy
                    wlShmPoolProxy =
                    this.display.getShmProxy()
                                .createPool(bufferPool,
                                            shmPool.getFileDescriptor(),
                                            bufferSize);
            final WlBufferProxy buffer = wlShmPoolProxy.createBuffer(new Buffer(bufferPool,
                                                                                shmPool,
                                                                                width,
                                                                                height),
                                                                     0,
                                                                     width,
                                                                     height,
                                                                     width * 4,
                                                                     shmFormat.value);
            bufferPool.queueBuffer(buffer);
            wlShmPoolProxy.destroy();
        }
        return bufferPool;
    }
}
