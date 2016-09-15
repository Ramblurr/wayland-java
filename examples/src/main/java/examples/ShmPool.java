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
package examples;

import org.freedesktop.jaccall.JNI;
import org.freedesktop.jaccall.Pointer;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ShmPool implements Closeable {
    private int        fd;
    private int        size;
    private ByteBuffer buffer;

    public ShmPool(final int size) throws IOException {
        this.fd = createTmpFileNative();
        this.size = size;
        try {
            truncateNative(getFd(),
                           getSize());
            this.buffer = map(getFd(),
                              getSize());
        }
        catch (final IOException e) {
            closeNative(getFd());
            throw e;
        }
    }

    private static int createTmpFileNative() {
        String template = "/wayland-java-shm-XXXXXX";
        String path     = System.getenv("XDG_RUNTIME_DIR");
        if (path == null) {
            throw new IllegalStateException("Cannot create temporary file: XDG_RUNTIME_DIR not set");
        }

        final Pointer<String> name = Pointer.nref(path + template);
        int                   fd   = Libc.mkstemp(name.address);

        int F_GETFD = 1;
        int flags = Libc.fcntl(fd,
                               F_GETFD,
                               0);
        if (-1 == flags) {
            Libc.close(fd);
            throw new RuntimeException("error");
        }

        int FD_CLOEXEC = 1;
        flags |= FD_CLOEXEC;
        int F_SETFD = 2;
        final int ret = Libc.fcntl(fd,
                                   F_SETFD,
                                   flags);
        if (-1 == ret) {
            Libc.close(fd);
            throw new RuntimeException("error");
        }

        return fd;
    }

    private static void truncateNative(int fd,
                                       int size) {
        Libc.ftruncate(fd,
                       size);
    }

    public int getFd() {
        return fd;
    }

    public int getSize() {
        return size;
    }

    private static ByteBuffer map(final int fd,
                                  final int size) throws IOException {
        final ByteBuffer tmpBuff = mapNative(fd,
                                             size);
        tmpBuff.order(ByteOrder.nativeOrder());
        return tmpBuff;
    }

    private static void closeNative(int fd) {
        Libc.close(fd);
    }

    private static ByteBuffer mapNative(int fd,
                                        int size) {
        int PROT_READ  = 0x01;
        int PROT_WRITE = 0x02;

        int prot       = PROT_READ | PROT_WRITE;
        int MAP_SHARED = 0x001;
        long bufferPointer = Libc.mmap(0L,
                                       size,
                                       prot,
                                       MAP_SHARED,
                                       fd,
                                       0);
        return JNI.wrap(bufferPointer,
                        size);
    }

    public int getFileDescriptor() {
        return this.fd;
    }

    public long size() {
        return this.size;
    }

    @Override
    public void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override
    public void close() throws IOException {
        if (this.buffer != null) {
            unmapNative(asByteBuffer());
            closeNative(getFd());
            this.fd = -1;
            this.size = 0;
            this.buffer = null;
        }
    }

    private static void unmapNative(ByteBuffer buffer) {
        buffer.capacity();
        Libc.munmap(JNI.unwrap(buffer),
                    buffer.capacity());
    }

    public ByteBuffer asByteBuffer() {
        if (this.buffer == null) {
            throw new IllegalStateException("ShmPool is closed");
        }

        return this.buffer;
    }
}

