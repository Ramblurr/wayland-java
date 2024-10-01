package org.freedesktop.wayland.util;

import org.freedesktop.wayland.C;
import org.freedesktop.wayland.wl_array;

import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.VarHandle;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class WlArray {
    public final MemorySegment arrayPtr;

    public WlArray(MemorySegment arrayPtr) {
        this.arrayPtr = arrayPtr;
    }

    public ByteBuffer asByteBuffer() {
        return this.arrayPtr.asSlice(0, wl_array.alloc(arrayPtr)).asByteBuffer();
    }

    /**
     * Iterates over a wl_array represented by a MemorySegment.
     *
     * @param array         The MemorySegment representing the wl_array
     * @param elementLayout The ValueLayout of each element
     * @param consumer      A function to process each element
     * @param <T>           The type of the element
     */
    public static <T> void wlArrayForEach(WlArray array, ValueLayout elementLayout, ElementConsumer<T> consumer) {
        long size = wl_array.size(array.arrayPtr);
        MemorySegment data = wl_array.data(array.arrayPtr);

        if (size == 0 || MemorySegment.NULL.equals(data)) {
            return;
        }

        long elementSize = elementLayout.byteSize();
        long elementCount = size / elementSize;

//        MemoryLayout layout = MemoryLayout.sequenceLayout(size / elementSize, elementLayout);
//        VarHandle elementHandle = layout.varHandle(MemoryLayout.PathElement.sequenceElement());
//        VarHandle elementHandle = layout.varHandle(MemoryLayout.PathElement.sequenceElement());
        MemoryLayout layout = MemoryLayout.sequenceLayout(size, elementLayout);
        VarHandle elementHandle = layout.varHandle(MemoryLayout.PathElement.sequenceElement());

        for (long i = 0; i < elementCount; i++) {
            @SuppressWarnings("unchecked")
            int wtf = (int) elementHandle.get(data, i);
            System.out.println(wtf);
//            consumer.accept(element);
        }
    }

    @FunctionalInterface
    public interface ElementConsumer<T> {
        void accept(T element);
    }

    public static <T> List<T> wlArrayForEach(MemorySegment arraySegment, ValueLayout valueLayout, Class<T> type) {
        long size = wl_array.size(arraySegment);
        MemorySegment data = wl_array.data(arraySegment);

        if (size == 0 || data.address() == 0) {
            return List.of();
        }

        int elementCount = (int) (size / valueLayout.byteSize());
        var ret = new ArrayList<T>(elementCount);
        for (int i = 0; i < elementCount; i++) {
            @SuppressWarnings("unchecked")
            var v = (T) valueLayout.varHandle().get(data, i * valueLayout.byteSize());
            System.out.println("i=" + i + " v=" + v);
            ret.add(v);
        }
        return ret;
    }

    /**
     * If it is an array of enum values, then you can get the enum using this helper
     *
     * @param a    the array
     * @param type the enum class (must be one of the wayland-scanner generated ones
     * @param <E>
     * @return
     */
    public static <E extends Enum<E>> List<E> asEnum(WlArray a, Class<E> type) {
        List<Integer> integers = WlArray.wlArrayForEach(a.arrayPtr, C.C_INT, int.class);
        List<E> ret = new ArrayList<>(integers.size());
        for (Integer i : integers) {
            ret.add(EnumUtil.of(type, i));
        }
        return ret;
    }

}
