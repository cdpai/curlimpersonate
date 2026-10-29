package cdpai.curlimpersonate;

import module java.base;
import static java.lang.foreign.ValueLayout.*;

final class Easy implements AutoCloseable {
    final Arena arena = Arena.ofConfined();
    final MemorySegment h;
    MemorySegment slist = MemorySegment.NULL;
    final MemorySegment err = arena.allocate(256);

    Easy(String target) throws Throwable {
        h = (MemorySegment) Lib.INIT.invoke();
        if (h.equals(MemorySegment.NULL)) throw new IllegalStateException("curl_easy_init failed");
        ok("curl_easy_impersonate " + target, (int) Lib.IMPERSONATE.invoke(h, arena.allocateFrom(target), 1));
        setP(Opt.ERRORBUFFER, err);
    }

    void ok(String what, int rc) throws Throwable {
        if (rc != Opt.OK) throw new IllegalStateException(what + " failed: " + Lib.strerror(rc) + " (" + rc + ") " + err.getString(0));
    }

    Easy setS(int opt, String v) throws Throwable { ok("setopt " + opt, (int) Lib.SET_PTR.invoke(h, opt, arena.allocateFrom(v))); return this; }

    Easy setP(int opt, MemorySegment v) throws Throwable { ok("setopt " + opt, (int) Lib.SET_PTR.invoke(h, opt, v)); return this; }

    Easy setI(int opt, int v) throws Throwable { ok("setopt " + opt, (int) Lib.SET_INT.invoke(h, opt, v)); return this; }

    Easy setO(int opt, long v) throws Throwable { ok("setopt " + opt, (int) Lib.SET_LONG.invoke(h, opt, v)); return this; }

    Easy headers(List<String> lines) throws Throwable {
        for (var l : lines) slist = (MemorySegment) Lib.SLIST_APPEND.invoke(slist, arena.allocateFrom(l));
        return lines.isEmpty() ? this : setP(Opt.HTTPHEADER, slist);
    }

    void perform() throws Throwable { ok("request", (int) Lib.PERFORM.invoke(h)); }

    int infoInt(int info) throws Throwable {
        var out = arena.allocate(JAVA_INT);
        ok("getinfo " + info, (int) Lib.GET_PTR.invoke(h, info, out));
        return out.get(JAVA_INT, 0);
    }

    String infoStr(int info) throws Throwable {
        var out = arena.allocate(ADDRESS);
        ok("getinfo " + info, (int) Lib.GET_PTR.invoke(h, info, out));
        return out.get(ADDRESS, 0).reinterpret(4096).getString(0);
    }

    public void close() {
        try {
            Lib.CLEANUP.invoke(h);
            if (!slist.equals(MemorySegment.NULL)) Lib.SLIST_FREE.invoke(slist);
        } catch (Throwable t) { throw new IllegalStateException(t); }
        arena.close();
    }
}
