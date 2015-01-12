package org.freedesktop.wayland.client.egl.jna;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.ptr.IntByReference;

public interface WaylandEglLibrary extends Library {
    public static final String            JNA_LIBRARY_NAME = "wayland-egl";
    public static final NativeLibrary     JNA_NATIVE_LIB   = NativeLibrary.getInstance(WaylandEglLibrary.JNA_LIBRARY_NAME);
    public static final WaylandEglLibrary INSTANCE         = (WaylandEglLibrary) Native.loadLibrary(WaylandEglLibrary.JNA_LIBRARY_NAME,
                                                                                                    WaylandEglLibrary.class);

    long wl_egl_window_create(long surface,
                              int width,
                              int height);

    void wl_egl_window_destroy(long egl_window);

    void wl_egl_window_resize(long egl_window,
                              int width,
                              int height,
                              int dx,
                              int dy);

    void wl_egl_window_get_attached_size(long egl_window,
                                         IntByReference width,
                                         IntByReference height);


}

