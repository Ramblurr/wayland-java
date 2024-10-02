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
import java.nio.ByteBuffer;

public class ShmBuffer {

    public final MemorySegment pointer;

    /**
     * Create a new underlying WlBufferResource with the constructed ShmBuffer as it's implementation.
     * <p>
     * {@code ShmBuffer} should never be stored in a compositor instead it should always be queried from a
     * {@code WlBufferResource}. Listening for the resource's destruction can be done when the buffer
     * resource is attached to a surface.
     *
     * @param client
     * @param id
     * @param width
     * @param height
     * @param stride
     * @param format
     */
    public ShmBuffer(final Client client,
                     final int id,
                     final int width,
                     final int height,
                     final int stride,
                     final int format) {
        this(LibWayland.wl_shm_buffer_create(client.pointer,
                id,
                width,
                height,
                stride,
                format));
    }

    protected ShmBuffer(final MemorySegment pointer) {
        this.pointer = pointer;
    }

    public static ShmBuffer get(final Resource<?> resource) {
        final MemorySegment wlShmBuffer = LibWayland.wl_shm_buffer_get(resource.wlResourcePtr);

        final ShmBuffer buffer;
        if (MemorySegment.NULL.equals(wlShmBuffer)) {
            buffer = null;
        } else {
            buffer = new ShmBuffer(wlShmBuffer);
        }
        return buffer;
    }

    /**
     * Mark that the given SHM buffer is about to be accessed
     * <p>
     * An SHM buffer is a memory-mapped file given by the client.
     * According to POSIX, reading from a memory-mapped region that
     * extends off the end of the file will cause a SIGBUS signal to be
     * generated. Normally this would cause the compositor to terminate.
     * In order to make the compositor robust against clients that change
     * the size of the underlying file or lie about its size, you should
     * protect access to the buffer by calling this function before
     * reading from the memory and call {@link #endAccess()}
     * afterwards. This will install a signal handler for SIGBUS which
     * will prevent the compositor from crashing.
     * <p>
     * After calling this function the signal handler will remain
     * installed for the lifetime of the compositor process. Note that
     * this function will not work properly if the compositor is also
     * installing its own handler for SIGBUS.
     * <p>
     * If a SIGBUS signal is received for an address within the range of
     * the SHM pool of the given buffer then the client will be sent an
     * error event when {@link #endAccess()} is called. If the signal
     * is for an address outside that range then the signal handler will
     * reraise the signal which would will likely cause the compositor to
     * terminate.
     * <p>
     * It is safe to nest calls to these functions as long as the nested
     * calls are all accessing the same buffer. The number of calls to
     * wl_shm_buffer_end_access must match the number of calls to
     * {@code beginAccess()}. These functions are thread-safe and it
     * is allowed to simultaneously access different buffers or the same
     * buffer from multiple threads.
     */
    public void beginAccess() {
        LibWayland.wl_shm_buffer_begin_access(this.pointer);
    }

    /**
     * Ends the access to a buffer started by {@link #beginAccess()}.
     * <p>
     * This should be called after {@link #beginAccess()} once the
     * buffer is no longer being accessed. If a SIGBUS signal was
     * generated in-between these two calls then the resource for the
     * given buffer will be sent an error.
     */
    public void endAccess() {
        LibWayland.wl_shm_buffer_end_access(this.pointer);
    }

    /**
     * Get a pointer to the memory for the SHM buffer
     * <p>
     * Returns a pointer which can be used to read the data contained in
     * the given SHM buffer.
     * <p>
     * As this buffer is memory-mapped, reading it from may generate
     * SIGBUS signals. This can happen if the client claims that the
     * buffer is larger than it is or if something truncates the
     * underlying file. To prevent this signal from causing the compositor
     * to crash you should call wl_shm_buffer_begin_access and
     * wl_shm_buffer_end_access around code that reads from the memory.
     *
     * @return a direct ByteBuffer.
     */
    public ByteBuffer getData() {
        MemorySegment data = LibWayland.wl_shm_buffer_get_data(this.pointer);
        return data.asByteBuffer();
    }

    public int getHeight() {
        return LibWayland.wl_shm_buffer_get_height(this.pointer);
    }

    public int getStride() {
        return LibWayland.wl_shm_buffer_get_stride(this.pointer);
    }

    public int getFormat() {
        return LibWayland.wl_shm_buffer_get_format(this.pointer);
    }

    public int getWidth() {
        return LibWayland.wl_shm_buffer_get_width(this.pointer);
    }

    @Override
    public int hashCode() {
        return this.pointer.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ShmBuffer shmBuffer)) {
            return false;
        }

        return this.pointer.equals(shmBuffer.pointer);

    }
}
