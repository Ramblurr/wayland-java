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
import org.freedesktop.wayland.shared.WlSeatCapability;
import org.freedesktop.wayland.util.EnumUtil;

import javax.annotation.Nonnull;
import java.util.EnumSet;

public class Display {

    private final WlDisplayProxy displayProxy;
    private final WlRegistryProxy registryProxy;

    private int shmFormats = 0;

    private WlCompositorProxy compositorProxy;
    private WlShmProxy shmProxy;
    private WlSeatProxy seatProxy;
    private XdgWmBaseProxy xdgWmBaseProxy;


    public Display() {
        this.displayProxy = WlDisplayProxy.connect("wayland-1");
        this.registryProxy = this.displayProxy.getRegistry(new WlRegistryEvents() {
            @Override
            public void global(final WlRegistryProxy emitter,
                               final int name,
                               @Nonnull final String interfaceName,
                               final int version) {
                Display.this.global(emitter,
                        name,
                        interfaceName,
                        version);
            }

            @Override
            public void globalRemove(final WlRegistryProxy emitter,
                                     final int name) {
                Display.this.globalRemove(emitter,
                        name);
            }
        });
        this.displayProxy.roundtrip();

        if (this.shmProxy == null) {
            throw new NullPointerException("wl_shm not found!");
        }

        /*
         * Why do we need two roundtrips here?
         *
         * wl_display_get_registry() sends a request to the server, to which
         * the server replies by emitting the wl_registry.global events.
         * The first wl_display_roundtrip() sends wl_display.sync. The server
         * first processes the wl_display.get_registry which includes sending
         * the global events, and then processes the sync. Therefore when the
         * sync (roundtrip) returns, we are guaranteed to have received and
         * processed all the global events.
         *
         * While we are inside the first wl_display_roundtrip(), incoming
         * events are dispatched, which causes registry_handle_global() to
         * be called for each global. One of these globals is wl_shm.
         * registry_handle_global() sends wl_registry.bind request for the
         * wl_shm global. However, wl_registry.bind request is sent after
         * the first wl_display.sync, so the reply to the sync comes before
         * the initial events of the wl_shm object.
         *
         * The initial events that get sent as a reply to binding to wl_shm
         * include wl_shm.format. These tell us which pixel formats are
         * supported, and we need them before we can create buffers. They
         * don't change at runtime, so we receive them as part of init.
         *
         * When the reply to the first sync comes, the server may or may not
         * have sent the initial wl_shm events. Therefore we need the second
         * wl_display_roundtrip() call here.
         *
         * The server processes the wl_registry.bind for wl_shm first, and
         * the second wl_display.sync next. During our second call to
         * wl_display_roundtrip() the initial wl_shm events are received and
         * processed. Finally, when the reply to the second wl_display.sync
         * arrives, it guarantees we have processed all wl_shm initial events.
         *
         * This sequence contains two examples on how wl_display_roundtrip()
         * can be used to guarantee, that all reply events to a request
         * have been received and processed. This is a general Wayland
         * technique.
         */


        this.displayProxy.roundtrip();
    }

    private void global(final WlRegistryProxy emitter,
                        final int name,
                        final String interfaceName,
                        final int version) {
        System.out.printf("Loading: name=%d ifaceName=%s version=%d%n", name, interfaceName, version);
        if (WlCompositorProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.compositorProxy = this.registryProxy.<WlCompositorEvents, WlCompositorProxy>bind(name,
                    WlCompositorProxy.class,
                    WlCompositorEventsV3.VERSION,
                    new WlCompositorEventsV3() {
                    });
        } else if (WlShmProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.shmProxy = this.registryProxy.bind(name,
                    WlShmProxy.class,
                    WlShmEvents.VERSION,
                    new WlShmEvents() {
                        @Override
                        public void format(final WlShmProxy emitter,
                                           final int format) {
                            Display.this.shmFormats |= (1 << format);
                        }
                    });
        } else if (XdgWmBaseProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.xdgWmBaseProxy = this.registryProxy.bind(name,
                    XdgWmBaseProxy.class,
                    XdgWmBaseEventsV6.VERSION,
                    new XdgWmBaseEventsV6() {
                        @Override
                        public void ping(XdgWmBaseProxy emitter, int serial) {
                            emitter.pong(serial);
                        }
                    });
        } else if (WlSeatProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.seatProxy = this.registryProxy.<WlSeatEvents, WlSeatProxy>bind(name,
                    WlSeatProxy.class,
                    WlSeatEventsV3.VERSION,
                    new WlSeatEventsV3() {
                        @Override
                        public void capabilities(final WlSeatProxy emitter, final int capabilities) {
                            EnumSet<WlSeatCapability> decode = EnumUtil.decode(WlSeatCapability.class, capabilities);
                            for (WlSeatCapability capability : decode) {
                                System.out.println("got seat capability: " + capability);
                            }
                        }

                        @Override
                        public void name(final WlSeatProxy emitter,
                                         @Nonnull final String name) {
                            System.out.println("Got seat with name " + name);
                        }
                    });
        }
    }

    private void globalRemove(final WlRegistryProxy wlRegistryProxy,
                              final int i) {

    }

    public void destroy() {
        if (this.shmProxy != null) {
            this.shmProxy.destroy();
        }
        if (this.xdgWmBaseProxy != null) {
            this.xdgWmBaseProxy.destroy();
        }

        this.compositorProxy.destroy();
        this.registryProxy.destroy();
        this.displayProxy.flush();
        this.displayProxy.disconnect();
    }

    public WlDisplayProxy getDisplayProxy() {
        return this.displayProxy;
    }

    public WlShmProxy getShmProxy() {
        return this.shmProxy;
    }

    public WlCompositorProxy getCompositorProxy() {
        return this.compositorProxy;
    }

    public WlSeatProxy getSeatProxy() {
        return this.seatProxy;
    }

    public XdgWmBaseProxy getXdgWmBaseProxy() {
        return this.xdgWmBaseProxy;
    }
}

