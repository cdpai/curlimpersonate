package cdpai.curlimpersonate;

import module java.base;
import static java.lang.foreign.ValueLayout.*;

final class FileFeed implements AutoCloseable {
    static final long ABORT = 0x10000000L;
    final FileChannel ch;

    FileFeed(Path file) throws IOException { ch = FileChannel.open(file, StandardOpenOption.READ); }

    long read(MemorySegment buf, long size, long n, MemorySegment ud) {
        try { return Math.max(0, ch.read(buf.reinterpret(size * n).asByteBuffer())); }
        catch (Throwable t) { return ABORT; }
    }

    MemorySegment stub(Arena a) throws ReflectiveOperationException {
        var mh = MethodHandles.lookup().findVirtual(FileFeed.class, "read", MethodType.methodType(long.class, MemorySegment.class, long.class, long.class, MemorySegment.class)).bindTo(this);
        return Lib.L.upcallStub(mh, Sink.CB, a);
    }

    public void close() throws IOException { ch.close(); }
}
