package cdpai.curlimpersonate;

import module java.base;
import static java.lang.foreign.ValueLayout.*;

final class Sink {
    static final FunctionDescriptor CB = FunctionDescriptor.of(JAVA_LONG, ADDRESS, JAVA_LONG, JAVA_LONG, ADDRESS);
    final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

    long write(MemorySegment p, long size, long n, MemorySegment ud) {
        try { bytes.write(p.reinterpret(size * n).toArray(JAVA_BYTE)); return size * n; }
        catch (Throwable t) { return 0; }
    }

    MemorySegment stub(Arena a) throws ReflectiveOperationException {
        var mh = MethodHandles.lookup().findVirtual(Sink.class, "write", MethodType.methodType(long.class, MemorySegment.class, long.class, long.class, MemorySegment.class)).bindTo(this);
        return Lib.L.upcallStub(mh, CB, a);
    }
}
