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
package org.freedesktop.wayland.util;


import org.freedesktop.wayland.wl_interface;

import java.lang.foreign.MemorySegment;
import java.util.HashMap;
import java.util.Map;

/**
 * Wrapper class for any Java type to get or create a native wayland interface for use with the native wayland
 * library. To get a native wayland interface for a given Java type, use {@link #get(Class)}.
 */
public class InterfaceMeta {

    public static final InterfaceMeta NO_INTERFACE = new InterfaceMeta(MemorySegment.NULL);
    private static final Map<Class<?>, InterfaceMeta> INTERFACE_MAP = new HashMap<>();

    public final MemorySegment wlInterfacePointer;

    protected InterfaceMeta(final MemorySegment wlInterfacePointer) {
        this.wlInterfacePointer = wlInterfacePointer;
        ObjectCache.store(this.wlInterfacePointer, this);
    }

    public MemorySegment getNativeWlInterface() {
        return this.wlInterfacePointer;
    }

    /**
     * Scans this type for {@link Interface} annotations and creates a native context if possible.
     *
     * @param type Any Java type.
     * @return The associated {@link InterfaceMeta} or {@link #NO_INTERFACE} if the type does not have a wayland interface
     * associated with it.
     */
    public static InterfaceMeta get(final Class<?> type) {
        InterfaceMeta interfaceMeta = INTERFACE_MAP.get(type);
        if (interfaceMeta == null) {
            final Interface waylandInterface = type.getAnnotation(Interface.class);
            if (waylandInterface == null) {
                interfaceMeta = NO_INTERFACE;
            } else {
                interfaceMeta = create(waylandInterface.name(),
                        waylandInterface.version(),
                        waylandInterface.methods(),
                        waylandInterface.events());
            }
            INTERFACE_MAP.put(type,
                    interfaceMeta);
        }
        return interfaceMeta;
    }

    protected static InterfaceMeta create(final String name,
                                          final int version,
                                          final Message[] methods,
                                          final Message[] events) {
        // TODO audit arena usage here
        MemorySegment wl_interface_ptr = wl_interface.allocate(Memory.ARENA_AUTO);
        wl_interface.name(wl_interface_ptr, Memory.ARENA_AUTO.allocateFrom(name));
        wl_interface.version(wl_interface_ptr, version);
        wl_interface.method_count(wl_interface_ptr, methods.length);
        wl_interface.event_count(wl_interface_ptr, events.length);
        wl_interface.methods(wl_interface_ptr, MessageMeta.initArray(methods, Memory.ARENA_AUTO));
        wl_interface.events(wl_interface_ptr, MessageMeta.initArray(events, Memory.ARENA_AUTO));
        return InterfaceMeta.get(wl_interface_ptr);
    }

    public static InterfaceMeta get(MemorySegment pointer) {
        InterfaceMeta interfaceMeta = ObjectCache.from(pointer);
        if (interfaceMeta == null) {
            interfaceMeta = new InterfaceMeta(pointer);
        }
        return interfaceMeta;
    }

    public String getName() {
        return wl_interface.name(this.wlInterfacePointer).getString(0);
    }

    @Override
    public int hashCode() {
        return getNativeWlInterface().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final InterfaceMeta that = (InterfaceMeta) o;

        return getNativeWlInterface().equals(that.getNativeWlInterface());
    }
}