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

import org.freedesktop.wayland.client.WlBufferEvents;
import org.freedesktop.wayland.client.WlBufferProxy;
import org.freedesktop.wayland.util.ShmPool;

import java.io.IOException;
import java.nio.ByteBuffer;


public class Buffer implements WlBufferEvents {

    private final BufferPool bufferPool;
    private final ShmPool shmPool;
    private final int        width;
    private final int        height;

    public Buffer(final BufferPool bufferPool,
                  final ShmPool shmPool,
                  final int width,
                  final int height) {
        this.bufferPool = bufferPool;
        this.shmPool = shmPool;
        this.width = width;
        this.height = height;
    }

    @Override
    public void release(final WlBufferProxy emitter) {
        if (this.bufferPool.isDestroyed()) {
            emitter.destroy();
            try {
                this.shmPool.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            this.bufferPool.queueBuffer(emitter);
        }
    }

    public ByteBuffer getByteBuffer() {
        return shmPool.asByteBuffer();
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
}
