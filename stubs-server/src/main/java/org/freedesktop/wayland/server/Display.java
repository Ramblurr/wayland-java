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
import org.freedesktop.wayland.util.Memory;
import org.freedesktop.wayland.util.ObjectCache;

import java.lang.foreign.MemorySegment;
import java.util.HashSet;
import java.util.Set;

public class Display {
    public static final int OBJECT_ID = 1;

    public final MemorySegment pointer;
    private final Set<DestroyListener> destroyListeners = new HashSet<>();

    protected Display(final MemorySegment pointer) {
        this.pointer = pointer;
        addDestroyListener(new Listener() {
            @Override
            public void handle() {
                notifyDestroyListeners();
                Display.this.destroyListeners.clear();
                ObjectCache.remove(Display.this.pointer);
                destroy();
            }
        });
        ObjectCache.store(this.pointer,
                this);
    }

    protected void addDestroyListener(final Listener listener) {
        LibWayland.wl_display_add_destroy_listener(this.pointer, listener.wlListenerPointer);
    }

    private void notifyDestroyListeners() {
        for (final DestroyListener listener : new HashSet<>(this.destroyListeners)) {
            listener.handle();
        }
    }

    /**
     * Create Wayland display object. <p> This creates the wl_display object.
     *
     * @return The Wayland display object. Null if failed to create
     */
    public static Display create() {
        return Display.get(LibWayland.wl_display_create());
    }

    public static Display get(final MemorySegment pointer) {
        if (MemorySegment.NULL.equals(pointer)) {
            return null;
        }
        Display display = ObjectCache.from(pointer);
        if (display == null) {
            display = new Display(pointer);
        }
        return display;
    }

    /**
     * Add a socket to Wayland display for the clients to connect.
     * <p> This adds a Unix socket to Wayland display which
     * can be used by clients to connect to Wayland display. <p> If NULL is passed as name, then it would look for
     * WAYLAND_DISPLAY env variable for the socket name. If WAYLAND_DISPLAY is not set, then default wayland-0 is used.
     * <p> The Unix socket will be created in the directory pointed to by environment variable XDG_RUNTIME_DIR. If
     * XDG_RUNTIME_DIR is not set, then this function fails and returns -1. <p> The length of socket path, i.e., the
     * path set in XDG_RUNTIME_DIR and the socket name, must not exceed the maxium length of a Unix socket path. The
     * function also fails if the user do not have write permission in the XDG_RUNTIME_DIR path or if the socket name is
     * already in use.
     *
     * @param name Name of the Unix socket.
     * @return 0 if success. -1 if failed.
     */

    public int addSocket(final String name) {
        return LibWayland.wl_display_add_socket(this.pointer, Memory.ARENA_AUTO.allocateFrom(name));
    }

    public String addSocketAuto() {
        var namePtr = LibWayland.wl_display_add_socket_auto(this.pointer);
        if (MemorySegment.NULL.equals(namePtr))
            throw new RuntimeException("wl_display_add_socket_auto failed");
        return namePtr.getString(0);
    }

    public void terminate() {
        LibWayland.wl_display_terminate(this.pointer);
    }

    public void run() {
        LibWayland.wl_display_run(this.pointer);
    }

    public void flushClients() {
        LibWayland.wl_display_flush_clients(this.pointer);
    }

    /**
     * Get the current serial number <p> This function returns the most recent serial number, but does not increment
     * it.
     */
    public int getSerial() {
        return LibWayland.wl_display_get_serial(this.pointer);
    }

    /**
     * Get the next serial number <p> This function increments the display serial number and returns the new value.
     */
    public int nextSerial() {
        return LibWayland.wl_display_next_serial(this.pointer);
    }

    public EventLoop getEventLoop() {
        return EventLoop.get(LibWayland.wl_display_get_event_loop(this.pointer));
    }

    public void register(final DestroyListener destroyListener) {
        this.destroyListeners.add(destroyListener);
    }

    public void unregister(final DestroyListener destroyListener) {
        this.destroyListeners.remove(destroyListener);
    }

    public int initShm() {
        return LibWayland.wl_display_init_shm(this.pointer);
    }

    /**
     * Add support for a wl_shm pixel format <p> Add the specified wl_shm format to the list of formats the wl_shm
     * object advertises when a client binds to it.  Adding a format to the list means that clients will know that the
     * compositor supports this format and may use it for creating wl_shm buffers.  The compositor must be able to
     * handle the pixel format when a client requests it. <p> The compositor by default supports WL_SHM_FORMAT_ARGB8888
     * and WL_SHM_FORMAT_XRGB8888. <p>
     *
     * @param format The wl_shm pixel format to advertise
     * @return The wl_shm format that was added to the list or 0 if adding it to the list failed.
     */
    public int addShmFormat(final int format) {
        var ret = LibWayland.wl_display_add_shm_format(this.pointer, format);
        if (MemorySegment.NULL.equals(ret))
            return 0;
        return ret.get(C.C_INT, 0);
    }

    @Override
    public int hashCode() {
        return this.pointer.hashCode();
    }

    //TODO wl_display_get_additional_shm_formats

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Display display = (Display) o;

        return this.pointer.equals(display.pointer);
    }

    /**
     * Destroy Wayland display object.
     * <p>
     * This function emits the wl_display destroy signal, releases all the sockets added to this display, free's all the
     * globals associated with this display, free's memory of additional shared memory formats and destroy the display
     * object.
     *
     * @see #addDestroyListener(Listener)
     */
    public void destroy() {
        LibWayland.wl_display_destroy(this.pointer);
    }
}
