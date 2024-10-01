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
import org.freedesktop.wayland.util.GlobalRef;
import org.freedesktop.wayland.util.Memory;
import org.freedesktop.wayland.util.ObjectCache;
import org.slf4j.LoggerFactory;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.HashSet;
import java.util.Set;

public class EventLoop {
    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventLoop.class);

    private static final MemorySegment WL_EVENT_LOOP_FD_FUNC;
    private static final MemorySegment WL_EVENT_LOOP_TIMER_FUNC;
    private static final MemorySegment WL_EVENT_LOOP_SIGNAL_FUNC;
    private static MemorySegment WL_EVENT_LOOP_IDLE_FUNC;

    static {
        try {
            // TODO correct arena?
            WL_EVENT_LOOP_FD_FUNC = Linker.nativeLinker().upcallStub(
                    MethodHandles
                            .lookup()
                            .findStatic(EventLoop.class,
                                    "eventLoopFdCallback",
                                    MethodType.methodType(int.class, int.class, int.class, MemorySegment.class)),
                    FunctionDescriptor.of(C.C_INT, C.C_INT, C.C_INT, C.C_POINTER),
                    Memory.ARENA_AUTO
            );

            WL_EVENT_LOOP_SIGNAL_FUNC =
                    Linker.nativeLinker().upcallStub(
                            MethodHandles
                                    .lookup()
                                    .findStatic(EventLoop.class,
                                            "eventLoopSignalCallback",
                                            MethodType.methodType(int.class, int.class, MemorySegment.class)),
                            FunctionDescriptor.of(C.C_INT, C.C_INT, C.C_POINTER),
                            Memory.ARENA_AUTO
                    );

            WL_EVENT_LOOP_TIMER_FUNC = Linker.nativeLinker().upcallStub(
                    MethodHandles
                            .lookup()
                            .findStatic(EventLoop.class,
                                    "eventLoopTimerCallback",
                                    MethodType.methodType(int.class, MemorySegment.class)),
                    FunctionDescriptor.of(C.C_INT, C.C_POINTER),
                    Memory.ARENA_AUTO
            );

        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    public static int eventLoopFdCallback(int fd, int mask, MemorySegment nativeData) {
        try {
            var handler = (FileDescriptorEventHandler) GlobalRef.reify(nativeData).get();
            return handler.handle(fd, mask);
        } catch (Throwable t) {
            // this is mandatory otherwise the JVM will crash
            LOG.error("event loop fd callback threw exception", t);
            return 0;
        }
    }

    public static int eventLoopSignalCallback(int signalNumber, MemorySegment nativeData) {
        try {
            var handler = (SignalEventHandler) GlobalRef.reify(nativeData).get();
            return handler.handle(signalNumber);
        } catch (Throwable t) {
            // this is mandatory otherwise the JVM will crash
            LOG.error("event loop signal callback threw exception", t);
            return 0;
        }
    }

    public static int eventLoopTimerCallback(MemorySegment nativeData) {
        try {
            var handler = (TimerEventHandler) GlobalRef.reify(nativeData).get();
            return handler.handle();
        } catch (Throwable t) {
            // this is mandatory otherwise the JVM will crash
            LOG.error("event loop timer callback threw exception", t);
            return 0;
        }
    }

    public final MemorySegment pointer;
    private final Set<DestroyListener> destroyListeners = new HashSet<>();

    private EventLoop(final MemorySegment pointer) {
        this.pointer = pointer;
        C.wl_event_loop_add_destroy_listener(this.pointer,
                Listener.create(() -> {
                    notifyDestroyListeners();
                    EventLoop.this.destroyListeners.clear();
                    ObjectCache.remove(EventLoop.this.pointer);
                }).wlListenerPointer);
        ObjectCache.store(this.pointer, this);
    }

    private void notifyDestroyListeners() {
        for (final DestroyListener listener : new HashSet<>(this.destroyListeners)) {
            listener.handle();
        }
    }

    public static EventLoop create() {
        return EventLoop.get(C.wl_event_loop_create());
    }

    public static EventLoop get(final MemorySegment eventLoopPointer) {
        if (MemorySegment.NULL.equals(eventLoopPointer)) {
            return null;
        }
        EventLoop eventLoop = ObjectCache.from(eventLoopPointer);
        if (eventLoop == null) {
            eventLoop = new EventLoop(eventLoopPointer);
        }
        return eventLoop;
    }

    public EventSource addFileDescriptor(final int fd,
                                         final int mask,
                                         final FileDescriptorEventHandler handler) {
        MemorySegment jObjectRef = GlobalRef.from(handler);
        MemorySegment eventSourcePtr = C.wl_event_loop_add_fd(
                this.pointer,
                fd,
                mask,
                WL_EVENT_LOOP_FD_FUNC,
                jObjectRef
        );
        var eventSource = new EventSource(jObjectRef, eventSourcePtr);
//        eventSources.add(eventSource);
        return eventSource;
    }

    public EventSource addTimer(final TimerEventHandler handler) {
        MemorySegment jObjectRef = GlobalRef.from(handler);
        MemorySegment eventSourcePtr = C.wl_event_loop_add_timer(
                this.pointer,
                WL_EVENT_LOOP_TIMER_FUNC,
                jObjectRef
        );
        var eventSource = new EventSource(jObjectRef, eventSourcePtr);
//        eventSources.add(eventSource);
        return eventSource;
    }

    public EventSource addSignal(final int signalNumber, final SignalEventHandler handler) {
        MemorySegment jObjectRef = GlobalRef.from(handler);
        MemorySegment eventSourcePtr = C.wl_event_loop_add_signal(
                this.pointer,
                signalNumber,
                WL_EVENT_LOOP_SIGNAL_FUNC,
                jObjectRef
        );

        var eventSource = new EventSource(jObjectRef, eventSourcePtr);
//        eventSources.add(eventSource);
        return eventSource;
    }

    public EventSource addIdle(final IdleHandler handler) {
        MemorySegment jObjectRef = GlobalRef.from(handler);
        MemorySegment eventSourcePtr = C.wl_event_loop_add_idle(
                this.pointer,
                WL_EVENT_LOOP_IDLE_FUNC,
                jObjectRef
        );

        var eventSource = new EventSource(jObjectRef, eventSourcePtr);
//        eventSources.add(eventSource);
        return eventSource;
    }

    public int dispatch(final int timeout) {
        return C.wl_event_loop_dispatch(this.pointer, timeout);
    }

    public void dispatchIdle() {
        C.wl_event_loop_dispatch_idle(this.pointer);
    }

    public int getFileDescriptor() {
        return C.wl_event_loop_get_fd(this.pointer);
    }

    public void register(final DestroyListener destroyListener) {
        this.destroyListeners.add(destroyListener);
    }

    public void unregister(final DestroyListener destroyListener) {
        this.destroyListeners.remove(destroyListener);
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
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final EventLoop eventLoop = (EventLoop) o;

        return this.pointer.equals(eventLoop.pointer);
    }

    public void destroy() {
        C.wl_event_loop_destroy(this.pointer);
        ObjectCache.remove(this.pointer);
    }

    public interface FileDescriptorEventHandler {
        int handle(int fd, int mask);
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

