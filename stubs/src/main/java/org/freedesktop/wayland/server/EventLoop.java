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
package org.freedesktop.wayland.server;

import com.github.zubnix.jaccall.Pointer;
import com.github.zubnix.jaccall.Ptr;
import com.github.zubnix.jaccall.Unsigned;
import org.freedesktop.wayland.HasNative;
import org.freedesktop.wayland.server.jaccall.WaylandServerCore;
import org.freedesktop.wayland.server.jaccall.wl_event_loop_fd_func_t;
import org.freedesktop.wayland.server.jaccall.wl_event_loop_idle_func_t;
import org.freedesktop.wayland.server.jaccall.wl_event_loop_signal_func_t;
import org.freedesktop.wayland.server.jaccall.wl_event_loop_timer_func_t;
import org.freedesktop.wayland.util.ObjectCache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static com.github.zubnix.jaccall.Pointer.wrap;
import static org.freedesktop.wayland.server.jaccall.Pointerwl_event_loop_fd_func_t.nref;
import static org.freedesktop.wayland.server.jaccall.Pointerwl_event_loop_idle_func_t.nref;
import static org.freedesktop.wayland.server.jaccall.Pointerwl_event_loop_signal_func_t.nref;
import static org.freedesktop.wayland.server.jaccall.Pointerwl_event_loop_timer_func_t.nref;

public class EventLoop implements HasNative<Pointer<?>> {

    //the following three maps are used to implement proper garbage collection in regards to event source and
    //event callback:

    public static final  int                       EVENT_READABLE            = 0x01;
    public static final  int                       EVENT_WRITABLE            = 0x02;
    public static final  int                       EVENT_HANGUP              = 0x04;
    public static final  int                       EVENT_ERROR               = 0x08;
    //This map maps different pointers with same address but separate instance to a single instance.
    private static final Map<Pointer, Pointer>     HANDLER_REF_CACHE         = new WeakHashMap<Pointer, Pointer>();
    //This map maps a single pointer instance to the java object that will be used as handler object.
    private static final Map<Pointer, Object>      HANDLER_REFS              = new WeakHashMap<Pointer, Object>();
    //this map is used to link handler object lifecycle to an event source lifecycle.
    private static final Map<EventSource, Pointer> EVENT_SOURCE_HANDLER_REFS = new WeakHashMap<EventSource, Pointer>();

    private static final wl_event_loop_fd_func_t WL_EVENT_LOOP_FD_FUNC = new wl_event_loop_fd_func_t() {
        @Override
        public int $(final int fd,
                     @Unsigned final int mask,
                     @Ptr(void.class) final long data) {
            final FileDescriptorEventHandler handler = (FileDescriptorEventHandler) HANDLER_REFS.get(wrap(data));
            return handler.handle(fd,
                                  mask);
        }
    };

    private static final wl_event_loop_timer_func_t WL_EVENT_LOOP_TIMER_FUNC = new wl_event_loop_timer_func_t() {
        @Override
        public int $(@Ptr(void.class) final long data) {
            final TimerEventHandler handler = (TimerEventHandler) HANDLER_REFS.get(wrap(data));
            return handler.handle();
        }
    };

    private static final wl_event_loop_signal_func_t WL_EVENT_LOOP_SIGNAL_FUNC = new wl_event_loop_signal_func_t() {
        @Override
        public int $(final int signal_number,
                     @Ptr(void.class) final long data) {
            final SignalEventHandler handler = (SignalEventHandler) HANDLER_REFS.get(wrap(data));
            return handler.handle(signal_number);
        }
    };

    private static final wl_event_loop_idle_func_t WL_EVENT_LOOP_IDLE_FUNC = new wl_event_loop_idle_func_t() {
        @Override
        public void $(@Ptr(void.class) final long data) {
            final IdleHandler handler = (IdleHandler) HANDLER_REFS.get(wrap(data));
            handler.handle();
        }
    };
    private final Pointer pointer;
    private final Set<DestroyListener> destroyListeners = new HashSet<DestroyListener>();
    private boolean valid;

    protected EventLoop(final Pointer pointer) {
        this.pointer = pointer;
        this.valid = true;
        addDestroyListener(new Listener() {
            @Override
            public void handle() {
                notifyDestroyListeners();
                EventLoop.this.destroyListeners.clear();
                EventLoop.this.valid = false;
                ObjectCache.remove(EventLoop.this.getNative());
                free();
            }
        });
        ObjectCache.store(getNative(),
                          this);
    }

    protected void addDestroyListener(final Listener listener) {
        WaylandServerCore.INSTANCE()
                         .wl_event_loop_add_destroy_listener(getNative().address,
                                                             listener.getNative().address);
    }

    private void notifyDestroyListeners() {
        for (final DestroyListener listener : new HashSet<DestroyListener>(this.destroyListeners)) {
            listener.handle();
        }
    }

    public Pointer getNative() {
        return this.pointer;
    }

    public static EventLoop create() {
        return EventLoop.get(wrap(WaylandServerCore.INSTANCE()
                                                   .wl_event_loop_create()));
    }

    public static EventLoop get(final Pointer<?> pointer) {
        if (pointer == null) {
            return null;
        }
        EventLoop eventLoop = ObjectCache.from(pointer);
        if (eventLoop == null) {
            eventLoop = new EventLoop(pointer);
        }
        return eventLoop;
    }

    public EventSource addFileDescriptor(final int fd,
                                         final int mask,
                                         final FileDescriptorEventHandler handler) {
        final Pointer handlerRef = getHandlerRef(handler);
        if (!HANDLER_REFS.containsKey(handlerRef)) {
            //handler will be garbage collected once event source is collected.
            HANDLER_REFS.put(handlerRef,
                             handler);
        }

        final EventSource eventSource = EventSource.create(wrap(WaylandServerCore.INSTANCE()
                                                                                 .wl_event_loop_add_fd(getNative().address,
                                                                                                       fd,
                                                                                                       mask,
                                                                                                       nref(WL_EVENT_LOOP_FD_FUNC).address,
                                                                                                       handlerRef.address)));
        EVENT_SOURCE_HANDLER_REFS.put(eventSource,
                                      handlerRef);
        return eventSource;
    }

    private Pointer getHandlerRef(final Object handler) {
        final Pointer handlerRefKey = wrap(handler.hashCode());

        Pointer handlerRef = HANDLER_REF_CACHE.get(handlerRefKey);
        if (handlerRef == null) {
            handlerRef = handlerRefKey;
            HANDLER_REF_CACHE.put(handlerRefKey,
                                  handlerRef);
        }
        return handlerRef;
    }

    public EventSource addTimer(final TimerEventHandler handler) {
        final Pointer handlerRef = getHandlerRef(handler);
        if (!HANDLER_REFS.containsKey(handlerRef)) {
            //handler will be garbage collected once event source is collected.
            HANDLER_REFS.put(handlerRef,
                             handler);
        }
        final EventSource eventSource = EventSource.create(wrap(WaylandServerCore.INSTANCE()
                                                                                 .wl_event_loop_add_timer(getNative().address,
                                                                                                          nref(WL_EVENT_LOOP_TIMER_FUNC).address,
                                                                                                          handlerRef.address)));
        EVENT_SOURCE_HANDLER_REFS.put(eventSource,
                                      handlerRef);
        return eventSource;
    }

    public EventSource addSignal(final int signalNumber,
                                 final SignalEventHandler handler) {

        final Pointer handlerRef = getHandlerRef(handler);
        if (!HANDLER_REFS.containsKey(handlerRef)) {
            //handler will be garbage collected once event source is collected.
            HANDLER_REFS.put(handlerRef,
                             handler);
        }
        final EventSource eventSource = EventSource.create(wrap(WaylandServerCore.INSTANCE()
                                                                                 .wl_event_loop_add_signal(getNative().address,
                                                                                                           signalNumber,
                                                                                                           nref(WL_EVENT_LOOP_SIGNAL_FUNC).address,
                                                                                                           handlerRef.address)));
        EVENT_SOURCE_HANDLER_REFS.put(eventSource,
                                      handlerRef);
        return eventSource;
    }

    public EventSource addIdle(final IdleHandler handler) {

        final Pointer handlerRef = getHandlerRef(handler);
        if (!HANDLER_REFS.containsKey(handlerRef)) {
            //handler will be garbage collected once event source is collected.
            HANDLER_REFS.put(handlerRef,
                             handler);
        }
        final EventSource eventSource = EventSource.create(wrap(WaylandServerCore.INSTANCE()
                                                                                 .wl_event_loop_add_idle(getNative().address,
                                                                                                         nref(WL_EVENT_LOOP_IDLE_FUNC).address,
                                                                                                         handlerRef.address)));
        EVENT_SOURCE_HANDLER_REFS.put(eventSource,
                                      handlerRef);
        return eventSource;
    }

    public int dispatch(final int timeout) {
        return WaylandServerCore.INSTANCE()
                                .wl_event_loop_dispatch(getNative().address,
                                                        timeout);
    }

    public void dispatchIdle() {
        WaylandServerCore.INSTANCE()
                         .wl_event_loop_dispatch_idle(getNative().address);
    }

    public int getFileDescriptor() {
        return WaylandServerCore.INSTANCE()
                                .wl_event_loop_get_fd(getNative().address);
    }

    public void register(final DestroyListener destroyListener) {
        this.destroyListeners.add(destroyListener);
    }

    public void unregister(final DestroyListener destroyListener) {
        this.destroyListeners.remove(destroyListener);
    }

    @Override
    public int hashCode() {
        return getNative().hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EventLoop eventLoop = (EventLoop) o;

        return getNative().equals(eventLoop.getNative());
    }

    public void destroy() {
        this.valid = false;
        ObjectCache.remove(getNative());
        WaylandServerCore.INSTANCE()
                         .free(getNative().address);
    }

    public interface FileDescriptorEventHandler {
        int handle(int fd,
                   int mask);
    }

    public interface TimerEventHandler {
        int handle();
    }

    public interface SignalEventHandler {
        int handle(int signalNumber);
    }

    public interface IdleHandler {
        void handle();
    }
}

