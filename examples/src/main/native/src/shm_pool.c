/*
 * Copyright © 2012-2013 Jason Ekstrand.
 *
 * Permission to use, copy, modify, distribute, and sell this software and its
 * documentation for any purpose is hereby granted without fee, provided that
 * the above copyright notice appear in all copies and that both that copyright
 * notice and this permission notice appear in supporting documentation, and
 * that the name of the copyright holders not be used in advertising or
 * publicity pertaining to distribution of the software without specific,
 * written prior permission.  The copyright holders make no representations
 * about the suitability of this software for any purpose.  It is provided "as
 * is" without express or implied warranty.
 *
 * THE COPYRIGHT HOLDERS DISCLAIM ALL WARRANTIES WITH REGARD TO THIS SOFTWARE,
 * INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS, IN NO
 * EVENT SHALL THE COPYRIGHT HOLDERS BE LIABLE FOR ANY SPECIAL, INDIRECT OR
 * CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE,
 * DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE
 * OF THIS SOFTWARE.
 */
#include <jni.h>

#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <sys/mman.h>

#include "examples_ShmPool.h"

JNIEXPORT jint JNICALL
Java_examples_ShmPool_createTmpFileNative(JNIEnv * env,
        jclass clazz)
{
    static const char template[] = "/wayland-java-shm-XXXXXX";
	const char *path;
    char *name;
    int fd, flags;

	path = getenv("XDG_RUNTIME_DIR");
	if (path == NULL) {
        wl_jni_throw_IOException(env, "Cannot create temporary file: XDG_RUNTIME_DIR not set");
		return -1;
	}

	name = malloc(strlen(path) + sizeof(template));
	if (name == NULL) {
        wl_jni_throw_OutOfMemoryError(env, NULL);
        return -1;
    }

	strcpy(name, path);
	strcat(name, template);

    fd = mkstemp(name);

    free(name);

    if (fd < 0) {
        wl_jni_throw_from_errno(env, errno);
        return -1;
    }

    flags = fcntl(fd, F_GETFD);
    if (flags == -1)
        goto close_file;

    flags |= FD_CLOEXEC;
    flags = fcntl(fd, F_SETFD, flags);
    if (flags == -1)
        goto close_file;

    return fd;

close_file:
    wl_jni_throw_from_errno(env, errno);
    close(fd);
    return -1;
}

JNIEXPORT jobject JNICALL
Java_examples_ShmPool_mapNative(JNIEnv * env, jclass clazz,
        jint fd, jlong size, jboolean dupFD, jboolean readOnly)
{
    void * buffer;
    int flags, success, prot;

    if (dupFD) {
        fd = dup(fd);
        if (fd < 0) {
            wl_jni_throw_from_errno(env, errno);
            return NULL;
        }

        flags = fcntl(fd, F_GETFD);
        if (flags == -1) {
            close(fd);
            wl_jni_throw_from_errno(env, errno);
            return NULL;
        }

        flags |= FD_CLOEXEC;
        success = fcntl(fd, F_SETFD, flags);
        if (success == -1) {
            close(fd);
            wl_jni_throw_from_errno(env, errno);
            return NULL;
        }
    }

    if (readOnly) {
        prot = PROT_READ;
    } else {
        prot = PROT_READ | PROT_WRITE;
    }

    buffer = mmap(NULL, size, prot, MAP_SHARED, fd, 0);

    if (buffer == MAP_FAILED) {
        wl_jni_throw_from_errno(env, errno);

        if (dupFD)
            close(fd);

        return NULL;
    }

    return (*env)->NewDirectByteBuffer(env, buffer, size);
}

JNIEXPORT void JNICALL
Java_examples_ShmPool_unmapNative(JNIEnv * env, jclass clazz,
        jobject buffer)
{
    void * data;
    int size;

    data = (*env)->GetDirectBufferAddress(env, buffer);
    size = (*env)->GetDirectBufferCapacity(env, buffer);

    if (munmap(data, size) < 0)
        wl_jni_throw_from_errno(env, errno);
}

JNIEXPORT void JNICALL
Java_examples_ShmPool_truncateNative(JNIEnv * env, jclass clazz,
        jint fd, jlong size)
{
    if (ftruncate(fd, size) < 0)
        wl_jni_throw_from_errno(env, errno);
}

JNIEXPORT void JNICALL
Java_examples_ShmPool_closeNative(JNIEnv * env, jclass clazz,
        jint fd)
{
    if (close(fd) < 0)
        wl_jni_throw_from_errno(env, errno);
}

