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
import org.freedesktop.wayland.util.GlobalRef;
import org.freedesktop.wayland.util.InterfaceMeta;
import org.freedesktop.wayland.util.Memory;
import org.freedesktop.wayland.util.ObjectCache;
import org.freedesktop.wayland.raw.wl_global_bind_func_t;

import java.lang.foreign.MemorySegment;


public abstract class Global<R extends Resource<?>> {
    private static final MemorySegment FUNC_T_POINTER = wl_global_bind_func_t.allocate(
            (client, data, version, id) -> {
                final Global<?> global = (Global<?>) GlobalRef.reify(data).get();
                global.onBindClient(Client.get(client), version, id);
            },
            Memory.ARENA_AUTO
    );

    private final MemorySegment pointer;
    private final MemorySegment jObjectPointer;

    protected Global(final Display display,
                     final Class<R> resourceClass,
                     final int version) {
        if (version <= 0) {
            throw new IllegalArgumentException("Version must be bigger than 0");
        }

        this.jObjectPointer = GlobalRef.from(this);

        this.pointer = LibWayland.wl_global_create(display.pointer,
                InterfaceMeta.get(resourceClass)
                        .getNativeWlInterface(),
                version,
                this.jObjectPointer,
                FUNC_T_POINTER);
        ObjectCache.store(this.pointer, this);
    }

    public abstract R onBindClient(Client client,
                                   int version,
                                   int id);

    @Override
    public int hashCode() {
        return this.pointer.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Global)) {
            return false;
        }

        final Global global = (Global) o;

        return this.pointer.equals(global.pointer);
    }

    public void destroy() {
        LibWayland.wl_global_destroy(this.pointer);
        ObjectCache.remove(this.pointer);
        GlobalRef.remove(jObjectPointer);
    }
}

