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

import org.freedesktop.wayland.raw.C;
import org.freedesktop.wayland.raw.LibWayland;

import java.lang.foreign.MemorySegment;

public final class EventSource {

    private final MemorySegment handlerRef;
    private final MemorySegment wlEventSource;

    EventSource(final MemorySegment handlerRef, final MemorySegment wlEventSource) {
        this.wlEventSource = wlEventSource;
        this.handlerRef = handlerRef;
    }

    public int updateFileDescriptor(final int mask) {
        return LibWayland.wl_event_source_fd_update(this.wlEventSource, mask);
    }

    public int updateTimer(final int msDelay) {
        return LibWayland.wl_event_source_timer_update(this.wlEventSource, msDelay);
    }

    public void check() {
        LibWayland.wl_event_source_check(this.wlEventSource);
    }

    @Override
    public int hashCode() {
        return this.wlEventSource.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EventSource that = (EventSource) o;

        return this.wlEventSource.equals(that.wlEventSource);

    }

    public int remove() {
        return LibWayland.wl_event_source_remove(this.wlEventSource);
    }
}
