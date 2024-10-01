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
 *
 */
package org.freedesktop.wayland.examples;

import org.freedesktop.wayland.client.*;
import org.freedesktop.wayland.shared.WlPointerButtonState;
import org.freedesktop.wayland.shared.WlShellSurfaceResize;
import org.freedesktop.wayland.shared.XdgToplevelWmCapabilities;
import org.freedesktop.wayland.util.Fixed;
import org.freedesktop.wayland.util.WlArray;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Objects;

import static org.freedesktop.wayland.shared.WlShmFormat.XRGB8888;

public class Window implements WlSurfaceEventsV3,
        XdgSurfaceEventsV6,
        WlPointerEventsV3,
        WlRegionEvents, XdgToplevelEventsV5 {

    private static final int BTN_LEFT = 0x110;
    private static final int BTN_RIGHT = 0x111;

    //    private final WlShellSurfaceProxy shellSurfaceProxy;
    private final XdgSurfaceProxy xdgSurfaceProxy;

    private final WlSurfaceProxy surfaceProxy;
    private final Display display;
    private final WlPointerProxy pointerProxy;
    private final XdgToplevelProxy xdgToplevelProxy;

    private WlCallbackProxy callbackProxy;
    private BufferPool bufferPool;

    private boolean needsBufferPoolUpdate;

    private boolean waitForConfigure = false;

    private int width;
    private int height;

    private int pendingWidth;
    private int pendingHeight;

    private int edges;

    private int pointerX;
    private int pointerY;
    private boolean maximized;
    private boolean fullscreen;

    public Window(final Display display,
                  final int width,
                  final int height) throws IOException {
        this.width = width;
        this.height = height;

        this.display = display;
        this.bufferPool = createBufferPool(this.display, 2);
        this.surfaceProxy = this.display
                .getCompositorProxy()
                .createSurface(this);
        Objects.requireNonNull(this.display.getXdgWmBaseProxy());
        this.xdgSurfaceProxy = this.display.getXdgWmBaseProxy()
                .getXdgSurface(this, this.surfaceProxy);
        this.pointerProxy = this.display
                .getSeatProxy()
                .getPointer(this);

        this.surfaceProxy.damage(0,
                0,
                this.width,
                this.height);


        Objects.requireNonNull(this.xdgSurfaceProxy);
        this.xdgToplevelProxy = this.xdgSurfaceProxy.getToplevel(this);
        this.xdgToplevelProxy.setTitle("simple-shm");
        this.xdgToplevelProxy.setAppId("org.freedesktop.wayland.examples.simple-shm");
        this.surfaceProxy.commit();
        this.waitForConfigure = true;
    }

    private BufferPool createBufferPool(final Display display,
                                        final int size) throws IOException {
        return new BufferPoolFactory(display).create(this.width,
                this.height,
                size,
                XRGB8888);
    }

    @Override
    public void enter(final WlPointerProxy emitter,
                      final int serial,
                      @Nonnull final WlSurfaceProxy surface,
                      @Nonnull final Fixed surfaceX,
                      @Nonnull final Fixed surfaceY) {
//        System.out.println(String.format("Pointer - enter : serial=%d, surface=%s, x=%s, y=%s",
//                                         serial,
//                                         surface,
//                                         surfaceX,
//                                         surfaceY));
    }

    @Override
    public void leave(final WlPointerProxy emitter,
                      final int serial,
                      @Nonnull final WlSurfaceProxy surface) {
//        System.out.println(String.format("Pointer - leave : serial=%d, surface=%s",
//                                         serial,
//                                         surface));
    }

    @Override
    public void motion(final WlPointerProxy emitter,
                       final int time,
                       @Nonnull final Fixed surfaceX,
                       @Nonnull final Fixed surfaceY) {
//        System.out.println(String.format("Pointer - motion : time=%d, x=%s, y=%s",
//                                         time,
//                                         surfaceX,
//                                         surfaceY));
        this.pointerX = surfaceX.asInt();
        this.pointerY = surfaceY.asInt();
    }

    @Override
    public void button(final WlPointerProxy emitter,
                       final int serial,
                       final int time,
                       final int button,
                       final int state) {
//        System.out.println(String.format("Pointer - button : serial=%d, time=%d, button=%d, state=%d",
//                                         serial,
//                                         time,
//                                         button,
//                                         state));

        final boolean buttonPressed = state == WlPointerButtonState.PRESSED.value;
//        if (buttonPressed && button == BTN_LEFT) {
//            this.shellSurfaceProxy.move(this.display.getSeatProxy(), serial);
//        } else if (buttonPressed && button == BTN_RIGHT) {
//            this.shellSurfaceProxy.resize(this.display.getSeatProxy(), serial, edge().value);
//        }
    }

    private WlShellSurfaceResize edge() {
        boolean bottom = this.pointerY > (this.height / 2);
        boolean right = this.pointerX > (this.width / 2);

        if (bottom && right) {
            return WlShellSurfaceResize.BOTTOM_RIGHT;
        } else if (bottom) {
            return WlShellSurfaceResize.BOTTOM_LEFT;
        } else if (right) {
            return WlShellSurfaceResize.TOP_RIGHT;
        } else {
            return WlShellSurfaceResize.TOP_LEFT;
        }
    }

    @Override
    public void axis(final WlPointerProxy emitter,
                     final int time,
                     final int axis,
                     @Nonnull final Fixed value) {
//        System.out.println(String.format("Pointer - axis : time=%d, axis=%d, value=%s",
//                                         time,
//                                         axis,
//                                         value));
    }

    @Override
    public void configure(XdgSurfaceProxy emitter, int serial) {
        this.redraw(0);
    }


//    @Override
//    public void ping(final WlShellSurfaceProxy emitter,
//                     final int serial) {
//        System.out.println(String.format("shell surface - ping : serial=%d",
//                                         serial));

//        emitter.pong(serial);
//    }
//
//    @Override
//    public void configure(final WlShellSurfaceProxy emitter,
//                          final int edges,
//                          int width,
//                          int height) {
//        this.edges = edges;
//        this.pendingWidth = width;
//        this.pendingHeight = height;
//        this.needsBufferPoolUpdate = true;
//    }

//    @Override
//    public void popupDone(final WlShellSurfaceProxy emitter) {
//    }

    @Override
    public void enter(final WlSurfaceProxy emitter,
                      @Nonnull final WlOutputProxy output) {

    }

    @Override
    public void leave(final WlSurfaceProxy emitter,
                      @Nonnull final WlOutputProxy output) {

    }

    public void destroy() {
        this.xdgSurfaceProxy.destroy();
        this.surfaceProxy.destroy();
        this.pointerProxy.destroy();
        if (callbackProxy != null) {
            this.callbackProxy.destroy();
        }
        this.bufferPool.destroy();
    }

    private int abs(final int i) {
        return i < 0 ? -i : i;
    }

    private void paintPixels(final Buffer buffer,
                             final int time) {
        final int halfh = buffer.getHeight() / 2;
        final int halfw = buffer.getWidth() / 2;
        int ir;
        int or;
        final IntBuffer image = buffer.getByteBuffer()
                .asIntBuffer();

        /* squared radii thresholds */
        or = (halfw < halfh ? halfw : halfh) - 8;
        ir = or - 32;
        or = or * or;
        ir = ir * ir;

        image.clear();
        image.position(0);
        for (int y = 0; y < buffer.getHeight(); y++) {
            final int y2 = (y - halfh) * (y - halfh);

            for (int x = 0; x < buffer.getWidth(); x++) {
                int v;

                final int r2 = (x - halfw) * (x - halfw) + y2;

                if (r2 < ir) {
                    v = (r2 / 32 + time / 64) * 0x0080401;
                } else if (r2 < or) {
                    v = (y + time / 32) * 0x0080401;
                } else {
                    v = (x + time / 16) * 0x0080401;
                }
                v &= 0x00ffffff;

                if (abs(x - y) > 6 && abs(x + y - buffer.getHeight()) > 6) {
                    v |= 0xff000000;
                }

                image.put(v);
            }
        }
    }

    public void redraw(final int time) {

        WlBufferProxy wlBufferProxy = this.bufferPool.popBuffer();
        Buffer buffer = (Buffer) wlBufferProxy.getImplementation();
        int dx = 0;
        int dy = 0;

        if (this.needsBufferPoolUpdate) {
            if (this.edges == WlShellSurfaceResize.NONE.value ||
                    this.edges == WlShellSurfaceResize.BOTTOM_RIGHT.value ||
                    this.edges == WlShellSurfaceResize.BOTTOM.value ||
                    this.edges == WlShellSurfaceResize.RIGHT.value) {
                dx = 0;
                dy = 0;
            } else if (this.edges == WlShellSurfaceResize.TOP.value ||
                    this.edges == WlShellSurfaceResize.TOP_RIGHT.value) {
                dx = 0;
                dy = this.height - this.pendingHeight;
            } else if (this.edges == WlShellSurfaceResize.LEFT.value ||
                    this.edges == WlShellSurfaceResize.BOTTOM_LEFT.value) {
                dx = this.width - this.pendingWidth;
                dy = 0;
            } else if (this.edges == WlShellSurfaceResize.TOP_LEFT.value) {
                dx = this.width - this.pendingWidth;
                dy = this.height - this.pendingHeight;
            }

            try {
                this.width = this.pendingWidth;
                this.height = this.pendingHeight;
                this.needsBufferPoolUpdate = false;

                //FIXME properly implement bufferpool destruction.
                BufferPool oldBufferPool = bufferPool;
                this.bufferPool = createBufferPool(display,
                        2);
                wlBufferProxy = this.bufferPool.popBuffer();
                buffer = (Buffer) wlBufferProxy.getImplementation();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        paintPixels(buffer, time);

        this.surfaceProxy.attach(wlBufferProxy,
                dx,
                dy);
        this.surfaceProxy.damage(0,
                0,
                buffer.getWidth(),
                buffer.getHeight());
        //cleanup the previous frame callback
        if (this.callbackProxy != null) {
            this.callbackProxy.destroy();
        }
        //allocate a new frame callback
        this.callbackProxy = this.surfaceProxy.frame(new WlCallbackEvents() {
            @Override
            public void done(final WlCallbackProxy emitter,
                             final int time) {
                redraw(time);
            }
        });

        this.surfaceProxy.commit();
    }

    @Override
    public void configure(XdgToplevelProxy emitter, int width, int height, @Nonnull WlArray states) {
        this.fullscreen = false;
        this.maximized = false;
        this.pendingWidth = width;
        this.pendingHeight = height;
        this.needsBufferPoolUpdate = true;

    }

    @Override
    public void close(XdgToplevelProxy emitter) {

    }

    @Override
    public void configureBounds(XdgToplevelProxy emitter, int width, int height) {
    }

    @Override
    public void wmCapabilities(XdgToplevelProxy emitter, @Nonnull WlArray capabilities) {
        List<XdgToplevelWmCapabilities> caps = WlArray.asEnum(capabilities, XdgToplevelWmCapabilities.class);
        for (XdgToplevelWmCapabilities cap : caps) {
            System.out.println("got cap " + cap);
        }
    }
}

