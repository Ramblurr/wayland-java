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

import org.freedesktop.wayland.C;
import org.freedesktop.wayland.util.Memory;
import org.freedesktop.wayland.util.ObjectCache;
import org.freedesktop.wayland.wl_listener;
import org.slf4j.LoggerFactory;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

//import static org.freedesktop.jaccall.Pointer.ref;

/**
 * A single listener for Wayland signals
 * <p/>
 * {@code Listener} provides the means to listen for {@code wl_signal} notifications. Many
 * Wayland objects use {@code Listener} for notification of significant events like
 * object destruction.
 * <p/>
 * Clients should create {@code Listener} objects manually and can register them as
 * listeners to signals using #wl_signal_add, assuming the signal is
 * directly accessible. For opaque structs like wl_event_loop, adding a
 * listener should be done through provided accessor methods. A listener can
 * only listen to one signal at a time.
 */
abstract class Listener {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventLoop.class);
    private static final MemorySegment WL_NOTIFY_FUNC;

    static {
        try {
            WL_NOTIFY_FUNC = Linker.nativeLinker().upcallStub(
                    MethodHandles
                            .lookup()
                            .findStatic(Listener.class,
                                    "listenerNativeCallback",
                                    MethodType.methodType(void.class, MemorySegment.class, MemorySegment.class)),
                    FunctionDescriptor.ofVoid(C.C_POINTER, C.C_POINTER),
                    Memory.ARENA_AUTO
            );
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private final Arena arena;
    private boolean destroyed = false;

    public static void listenerNativeCallback(MemorySegment listenerPointer, MemorySegment data) {
        try {
            final Listener listener = ObjectCache.from(listenerPointer);
            listener.handle();
        } catch (Throwable t) {
            LOG.error("Exception in wayland Listener callback", t);
        }
    }

    public final MemorySegment wlListenerPointer;

    @FunctionalInterface
    public interface ListenerCallback {
        void handle();
    }

    public static Listener create(ListenerCallback callback) {
        return new Listener() {
            @Override
            public void handle() {
                callback.handle();
            }
        };
    }

    public Listener() {
        this.arena = Arena.ofShared();
        this.wlListenerPointer = wl_listener.allocate(arena);
        wl_listener.notify(this.wlListenerPointer,
                WL_NOTIFY_FUNC
        );
        ObjectCache.store(this.wlListenerPointer, this);
    }

    public void remove() {
        C.wl_list_remove(wl_listener.link(this.wlListenerPointer));
    }

    public void destroy() {
        ObjectCache.remove(this.wlListenerPointer);
        arena.close();
        destroyed = true;
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public abstract void handle();

    @Override
    public int hashCode() {
        return this.wlListenerPointer.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Listener)) {
            return false;
        }

        final Listener listener = (Listener) o;

        return this.wlListenerPointer == listener.wlListenerPointer;
    }
}

