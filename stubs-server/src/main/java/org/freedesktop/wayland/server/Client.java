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
import org.freedesktop.wayland.util.ObjectCache;

import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

public class Client {

    public final MemorySegment pointer;

    Client(final MemorySegment pointer) {
        this.pointer = pointer;
    }

    /**
     * Create a client for the given file descriptor
     * <p>
     * Given a file descriptor corresponding to one end of a socket, this
     * function will create a {@link Client} and add the new client to
     * the compositors client list.  At that point, the client is
     * initialized and ready to run, as if the client had connected to the
     * servers listening socket.  When the client eventually sends
     * requests to the compositor, the {@link Client} argument to the request
     * handler will be the client returned from this function.
     * <p>
     * The other end of the socket can be passed to
     * {link WlDisplayProxy#connectToFd(int)} on the client side or used with the
     * WAYLAND_SOCKET environment variable on the client side.
     * <p>
     * On failure this function sets errno accordingly and returns NULL.
     *
     * @param display The display object
     * @param fd      The file descriptor for the socket to the client
     * @return The new client object or NULL on failure.
     */
    public static Client create(final Display display,
                                final int fd) {
        return Client.get(C.wl_client_create(display.pointer, fd));
    }

    public static Client get(final MemorySegment pointer) {
        return new Client(pointer);
    }

    /**
     * Flush pending events to the client,
     * <p>
     * Events sent to clients are queued in a buffer and written to the
     * socket later - typically when the compositor has handled all
     * requests and goes back to block in the event loop.  This function
     * flushes all queued up events for a client immediately.
     */
    public void flush() {
        C.wl_client_flush(this.pointer);
    }

    protected void addDestroyListener(final Listener listener) {
        C.wl_client_add_destroy_listener(this.pointer, listener.wlListenerPointer);
    }

    //TODO wl_client_get_object
    //TODO wl_client_post_no_memory
    //TODO wl_client_get_credentials

    /**
     * Get the display object for the given client
     * <p>
     *
     * @return The display object the client is associated with.
     */
    public Display getDisplay() {
        return Display.get(C.wl_client_get_display(this.pointer));
    }

    /**
     * Look up an object in the client name space. This looks up an object in the client object name space by its
     * object ID.
     *
     * @param id The object id
     * @return The object or null if there is not object for the given ID
     */
    public Resource<?> getObject(final int id) {
        return ObjectCache.from(C.wl_client_get_object(this.pointer, id));
    }

    /**
     * Return Unix credentials for the client
     * <p>
     * This function returns the process ID, the user ID and the group ID
     * for the given client.  The credentials come from getsockopt() with
     * SO_PEERCRED, on the client socket fd.
     * <p>
     * Be aware that for clients that a compositor forks and execs and
     * then connects using socketpair(), this function will return the
     * credentials for the compositor.  The credentials for the socketpair
     * are set at creation time in the compositor.
     */
    public ClientCredentials getCredentials() {
        try (Arena a = Arena.ofAuto()) {
            MemorySegment pid = a.allocate(C.pid_t);
            MemorySegment uid = a.allocate(C.pid_t);
            MemorySegment gid = a.allocate(C.pid_t);
            C.wl_client_get_credentials(this.pointer, pid, uid, gid);

            return new ClientCredentials(
                    pid.get(C.pid_t, 0),
                    uid.get(C.pid_t, 0),
                    gid.get(C.pid_t, 0)
            );
        }
    }

    public void destroy() {
        C.wl_client_destroy(this.pointer);
    }

    @Override
    public int hashCode() {
        return this.pointer.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Client client)) return false;
        return Objects.equals(pointer, client.pointer);
    }
}

