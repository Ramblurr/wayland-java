//Copyright 2015 Erik De Rijcke
//
//Licensed under the Apache License,Version2.0(the"License");
//you may not use this file except in compliance with the License.
//You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,software
//distributed under the License is distributed on an"AS IS"BASIS,
//WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,either express or implied.
//See the License for the specific language governing permissions and
//limitations under the License.
package org.freedesktop.wayland.client;

import com.sun.jna.Pointer;
import org.freedesktop.wayland.HasNative;
import org.freedesktop.wayland.client.jna.WaylandClientLibrary;
import org.freedesktop.wayland.util.ObjectCache;

/**
 * A queue for {@link Proxy} object events.
 * <p/>
 * Event queues allows the events on a display to be handled in a thread-safe
 * manner.
 *
 * @see Display
 */
public class EventQueue implements HasNative<Pointer> {
    private final Pointer pointer;
    private       boolean valid;

    protected EventQueue(final Pointer pointer) {
        this.pointer = pointer;
        this.valid = true;
        ObjectCache.store(getNative(),
                          this);
    }

    public static EventQueue get(final Pointer pointer) {
        EventQueue eventQueue = ObjectCache.from(pointer);
        if (eventQueue == null) {
            eventQueue = new EventQueue(pointer);
        }
        return eventQueue;
    }

    public Pointer getNative() {
        return this.pointer;
    }

    @Override
    public boolean isValid() {
        return valid;
    }

    /**
     * Destroy an event queue
     * <p/>
     * Destroy the given event queue. Any pending event on that queue is
     * discarded.
     * <p/>
     * The {@link Display} object used to create the queue should not be
     * destroyed until all event queues created with it are destroyed with
     * this function.
     */
    public void destroy() {
        if (isValid()) {
            this.valid = false;
            WaylandClientLibrary.INSTANCE()
                                .wl_event_queue_destroy(getNative());
            ObjectCache.remove(getNative());
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EventQueue that = (EventQueue) o;

        return getNative().equals(that.getNative());
    }

    @Override
    public int hashCode() {
        return getNative().hashCode();
    }

    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }
}