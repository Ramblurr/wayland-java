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
import org.freedesktop.wayland.util.*;
import org.freedesktop.wayland.wl_resource_destroy_func_t;

import java.lang.foreign.MemorySegment;
import java.util.HashSet;
import java.util.Set;

/**
 * Server side implementation of a wayland object for a specific client.
 *
 * @param <I> Type of implementation that will be used to handle client requests.
 */
public abstract class Resource<I> implements WaylandObject {

    private static final MemorySegment RESOURCE_DESTROY_FUNC;

    static {
        RESOURCE_DESTROY_FUNC = wl_resource_destroy_func_t.allocate((resourcePointer) -> {
                    final Resource<?> resource = ObjectCache.from(resourcePointer);
                    resource.notifyDestroyListeners();
                    resource.destroyListeners.clear();
                    ObjectCache.remove(resourcePointer);
                    GlobalRef.remove(resource.jObjectRef);
                },
                Memory.ARENA_AUTO
        );
    }

    public final MemorySegment wlResourcePtr;
    private final I implementation;
    private final Set<DestroyListener> destroyListeners = new HashSet<>();
    private final MemorySegment jObjectRef;

    protected Resource(final Client client,
                       final int version,
                       final int id,
                       final I implementation) {
        this.implementation = implementation;
        this.wlResourcePtr = C.wl_resource_create(
                client.pointer,
                InterfaceMeta.get(getClass()).getNativeWlInterface(),
                version,
                id);
        ObjectCache.store(this.wlResourcePtr, this);
        this.jObjectRef = GlobalRef.from(this);

        C.wl_resource_set_dispatcher(
                this.wlResourcePtr,
                Dispatcher.INSTANCE,
                jObjectRef,
                MemorySegment.NULL,
                RESOURCE_DESTROY_FUNC
        );
    }

    // TODO add static get(Pointer) method for each generated resource
    // TODO wl_resource_post_no_memory
    // TODO wl_resource_queue_event_array
    // TODO wl_resource_queue_event

    protected Resource(final MemorySegment pointer) {
        this.jObjectRef = GlobalRef.from(this);
        this.wlResourcePtr = pointer;
        this.implementation = null;
        addDestroyListener(new Listener() {
            @Override
            public void handle() {
                notifyDestroyListeners();
                Resource.this.destroyListeners.clear();
                ObjectCache.remove(Resource.this.wlResourcePtr);
                GlobalRef.remove(Resource.this.jObjectRef);
//                destroy(); // TODO double free?
            }
        });
        ObjectCache.store(pointer,
                this);
    }

    protected void addDestroyListener(final Listener listener) {
        C.wl_resource_add_destroy_listener(this.wlResourcePtr, listener.wlListenerPointer);
    }

    private void notifyDestroyListeners() {
        for (final DestroyListener listener : new HashSet<>(this.destroyListeners)) {
            listener.handle();
        }
    }

    public I getImplementation() {
        return this.implementation;
    }

    public Client getClient() {
        return Client.get(
                C.wl_resource_get_client(this.wlResourcePtr)
        );
    }

    public int getId() {
        return C.wl_resource_get_id(this.wlResourcePtr);
    }

    public int getVersion() {
        return C.wl_resource_get_version(this.wlResourcePtr);
    }

    public void register(final DestroyListener destroyListener) {
        this.destroyListeners.add(destroyListener);
    }

    public void unregister(final DestroyListener destroyListener) {
        this.destroyListeners.remove(destroyListener);
    }

    /**
     * Post an event to the client's object referred to by 'resource'.
     * 'opcode' is the event number generated from the protocol XML
     * description (the event name). The variable arguments are the event
     * parameters, in the order they appear in the protocol XML specification.
     * <p>
     * The variable arguments' types are:
     * <ul>
     * <li>type=uint: uint32_t</li>
     * <li>type=int: int32_t</li>
     * <li>type=fixed: wl_fixed_t</li>
     * <li>type=string: (const char *) to a nil-terminated string</li>
     * <li>type=array: (struct wl_array *)</li>
     * <li>type=fd: int, that is an open file descriptor</li>
     * <li>type=new_id: (struct wl_object *) or (struct wl_resource *)</li>
     * <li>type=object: (struct wl_object *) or (struct wl_resource *)</li>
     * </ul>
     *
     * @param opcode the protocol opcode
     * @param args   the protocol arguments
     */
    public void postEvent(final int opcode,
                          final Arguments args) {
        C.wl_resource_post_event_array(this.wlResourcePtr, opcode, args.pointer);
        // TODO deallocate the arguments array here
//        args.pointer.close();
    }

    /**
     * @param opcode the protocol opcode
     * @see #postEvent(int, org.freedesktop.wayland.util.Arguments)
     */
    public void postEvent(final int opcode) {
        C.wl_resource_post_event_array(this.wlResourcePtr, opcode, MemorySegment.NULL);
    }

    public void postError(final int code,
                          final String msg) {
        C.wl_resource_post_error invoker = C.wl_resource_post_error.makeInvoker(C.C_POINTER);
        invoker.apply(this.wlResourcePtr, code, Memory.ARENA_AUTO.allocateFrom(msg));
    }

    @Override
    public int hashCode() {
        return this.wlResourcePtr.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Resource)) {
            return false;
        }

        final Resource resource = (Resource) o;

        return this.wlResourcePtr.equals(resource.wlResourcePtr);
    }

    public void destroy() {
        C.wl_resource_destroy(this.wlResourcePtr);
    }

    @Override
    public MemorySegment getPointer() {
        return this.wlResourcePtr;
    }
}
