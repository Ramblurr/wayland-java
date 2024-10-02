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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.newsclub.net.unix.AFUNIXSocketChannel;
import org.newsclub.net.unix.AFUNIXSocketPair;
import org.newsclub.net.unix.FileDescriptorCast;
import org.slf4j.LoggerFactory;
import sun.misc.Signal;

import java.io.IOException;

public class EventLoopTest {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(EventLoopTest.class);


    public static int getFd(AFUNIXSocketChannel channel) throws IOException {
        return FileDescriptorCast.using(channel.getFileDescriptor()).as(Integer.class);
    }


    public class HandlerDataHolder {
        public int fd_or_signal_number = -1;
        public int nCalls = 0;
        public int lastMask = -1;
        public EventSource source1 = null;
        public EventSource source2 = null;
    }

    @Test
    public void test_event_loop_post_dispatch_check() throws IOException {
        HandlerDataHolder thing = new HandlerDataHolder();
        EventLoop eventLoop = EventLoop.create();
        try (AFUNIXSocketPair<AFUNIXSocketChannel> pair = AFUNIXSocketPair.open()) {
            var r = pair.getFirst();
            var r_fd = getFd(r);
            var source = eventLoop.addFileDescriptor(r_fd,
                    LibWayland.WL_EVENT_READABLE(),
                    (fd, mask) -> {
                        thing.fd_or_signal_number = fd;
                        thing.nCalls += 1;
                        thing.lastMask = mask;
                        return 0;
                    }
            );
            source.check();
            eventLoop.dispatch(0);
            Assertions.assertEquals(r_fd, thing.fd_or_signal_number);
            Assertions.assertEquals(1, thing.nCalls);
            Assertions.assertEquals(0, thing.lastMask);
        }
    }

    @Test
    @Disabled
    // This test is disabled because it results in the jvm process being killed with
    //    Process finished with exit code 138 (interrupted by signal 10:SIGUSR1)
    // I guess the jvm signal handling is taking precedence over the native?
    // the wayland docs say:
    //      This function blocks the normal delivery of the given signal in the calling thread, and creates a "watch"
    //      for it. Signal delivery no longer happens asynchronously, but by wl_event_loop_dispatch() calling the
    //      dispatch callback function func.
    //      It is the caller's responsibility to ensure that all other threads have also blocked the signal.
    //
    // The jvm runs a lot of different threads, that we don't control, so I'm not sure this feature will ever work
    // on the jvm
    public void test_event_loop_signal() throws IOException {
        var thing = new HandlerDataHolder();
        EventLoop eventLoop = EventLoop.create();
        var signal = new Signal("USR1");
        eventLoop.addSignal(signal.getNumber(), (signalNumber) -> {
            thing.fd_or_signal_number = signalNumber;
            thing.nCalls += 1;
            return 1;
        });
        var pid = ProcessHandle.current().pid();
        LOG.info("PID: {} Signal: {}", pid, signal.getNumber());
        Runtime.getRuntime().exec("kill -s " + signal.getNumber() + " " + pid);
        eventLoop.dispatch(0);
        Assertions.assertEquals(signal.getNumber(), thing.fd_or_signal_number);
    }

    @Test
    public void test_event_loop_timer() {
        var thing = new HandlerDataHolder();
        EventLoop eventLoop = EventLoop.create();
        var source = eventLoop.addTimer(() -> {
            thing.fd_or_signal_number = 42;
            thing.nCalls += 1;
            return 1;
        });

        source.updateTimer(10);
        eventLoop.dispatch(0);
        Assertions.assertEquals(0, thing.nCalls);

        eventLoop.dispatch(20);
        Assertions.assertEquals(42, thing.fd_or_signal_number);
        Assertions.assertEquals(1, thing.nCalls);
    }

    @Test
    public void test_event_loop_timer_updates() throws InterruptedException {
        var thing = new HandlerDataHolder();
        EventLoop.TimerEventHandler handler1 = () -> {
            thing.nCalls += 1;
            thing.source1.updateTimer(1000);
            return 1;
        };
        EventLoop.TimerEventHandler handler2 = () -> {
            thing.nCalls += 1;
            thing.source2.updateTimer(1000);
            return 1;
        };
        EventLoop eventLoop = EventLoop.create();
        var source1 = eventLoop.addTimer(handler1);
        source1.updateTimer(10);

        var source2 = eventLoop.addTimer(handler2);
        source2.updateTimer(10);

        thing.source1 = source1;
        thing.source2 = source2;

        // wait so the both timers should have been fired by the time we continue
        Thread.sleep(15);

        var start = System.currentTimeMillis();
        eventLoop.dispatch(20);
        var end = System.currentTimeMillis();
        Assertions.assertEquals(2, thing.nCalls);
//        LOG.info("diff: {}", (end - start));
        Assertions.assertTrue((end - start) < 1000);
    }

    public static boolean DESTROY_A = false;
    public static boolean DESTROY_B = false;

    @Test
    public void test_event_loop_destroy() {
        var eventLoop = EventLoop.create();
        DestroyListener listenerA = () -> {
            EventLoopTest.DESTROY_A = true;
        };
        eventLoop.register(listenerA);
        eventLoop.register(() -> {
            EventLoopTest.DESTROY_B = true;
        });
        eventLoop.unregister(listenerA);
        eventLoop.destroy();
        Assertions.assertFalse(DESTROY_A);
        Assertions.assertTrue(DESTROY_B);
    }
}
