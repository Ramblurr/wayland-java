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

package org.freedesktop.wayland.examples;

import org.freedesktop.wayland.client.WlCompositorEvents;
import org.freedesktop.wayland.client.WlCompositorEventsV3;
import org.freedesktop.wayland.client.WlCompositorProxy;
import org.freedesktop.wayland.client.WlDisplayProxy;
import org.freedesktop.wayland.client.WlRegistryEvents;
import org.freedesktop.wayland.client.WlRegistryProxy;
import org.freedesktop.wayland.client.WlSeatEvents;
import org.freedesktop.wayland.client.WlSeatEventsV3;
import org.freedesktop.wayland.client.WlSeatProxy;
import org.freedesktop.wayland.client.WlShellEvents;
import org.freedesktop.wayland.client.WlShellProxy;
import org.freedesktop.wayland.client.WlShmEvents;
import org.freedesktop.wayland.client.WlShmProxy;

import javax.annotation.Nonnull;

public class Display {

    private final WlDisplayProxy  displayProxy;
    private final WlRegistryProxy registryProxy;

    private int shmFormats = 0;

    private WlCompositorProxy compositorProxy;
    private WlShmProxy        shmProxy;
    private WlSeatProxy       seatProxy;
    private WlShellProxy      shellProxy;


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

        this.displayProxy.roundtrip();
    }

    private void global(final WlRegistryProxy emitter,
                        final int name,
                        final String interfaceName,
                        final int version) {
        if (WlCompositorProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.compositorProxy = this.registryProxy.<WlCompositorEvents, WlCompositorProxy>bind(name,
                    WlCompositorProxy.class,
                    WlCompositorEventsV3.VERSION,
                    new WlCompositorEventsV3() {
                    });
        }
        else if (WlShmProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.shmProxy = this.registryProxy.<WlShmEvents, WlShmProxy>bind(name,
                    WlShmProxy.class,
                    WlShmEvents.VERSION,
                    new WlShmEvents() {
                        @Override
                        public void format(final WlShmProxy emitter,
                                           final int format) {
                            Display.this.shmFormats |= (1 << format);
                        }
                    });
        }
        else if (WlShellProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.shellProxy = this.registryProxy.<WlShellEvents, WlShellProxy>bind(name,
                    WlShellProxy.class,
                    WlShellEvents.VERSION,
                    new WlShellEvents() {
                    });
        }
        else if (WlSeatProxy.INTERFACE_NAME.equals(interfaceName)) {
            this.seatProxy = this.registryProxy.<WlSeatEvents, WlSeatProxy>bind(name,
                    WlSeatProxy.class,
                    WlSeatEventsV3.VERSION,
                    new WlSeatEventsV3() {
                        @Override
                        public void capabilities(final WlSeatProxy emitter,
                                                 final int capabilities) {

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
        if (this.shellProxy != null) {
            this.shellProxy.destroy();
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

    public WlShellProxy getShellProxy() {
        return this.shellProxy;
    }
}

