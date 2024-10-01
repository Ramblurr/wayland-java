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
import org.freedesktop.wayland.client.WlShmPoolEvents;

import java.util.LinkedList;

public class BufferPool implements WlShmPoolEvents {

    private LinkedList<WlBufferProxy> bufferQueue = new LinkedList<WlBufferProxy>();
    private boolean destroyed;

    public void queueBuffer(WlBufferProxy buffer) {
        if (destroyed) {
            throw new IllegalStateException("Pool destroyed");
        }

        this.bufferQueue.add(buffer);
    }

    public WlBufferProxy popBuffer() {
        if (destroyed) {
            throw new IllegalStateException("Pool destroyed");
        }

        return bufferQueue.pop();
    }

    public void destroy() {
        if (destroyed) {
            throw new IllegalStateException("Pool destroyed");
        }

        for (WlBufferProxy wlBufferProxy : bufferQueue) {
            wlBufferProxy.destroy();
        }
        bufferQueue.clear();
        this.destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }
}
