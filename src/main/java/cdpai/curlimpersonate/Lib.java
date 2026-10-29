package cdpai.curlimpersonate;

import module java.base;
import static java.lang.foreign.ValueLayout.*;

final class Lib {
    static final Linker L = Linker.nativeLinker();
    static final SymbolLookup LOOKUP = NativeLibrary.open(Arena.global());
    static final MethodHandle
        GLOBAL_INIT = fn("curl_global_init", FunctionDescriptor.of(JAVA_INT, JAVA_INT)),
        INIT = fn("curl_easy_init", FunctionDescriptor.of(ADDRESS)),
        CLEANUP = fn("curl_easy_cleanup", FunctionDescriptor.ofVoid(ADDRESS)),
        PERFORM = fn("curl_easy_perform", FunctionDescriptor.of(JAVA_INT, ADDRESS)),
        IMPERSONATE = fn("curl_easy_impersonate", FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)),
        STRERROR = fn("curl_easy_strerror", FunctionDescriptor.of(ADDRESS, JAVA_INT)),
        SLIST_APPEND = fn("curl_slist_append", FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS)),
        SLIST_FREE = fn("curl_slist_free_all", FunctionDescriptor.ofVoid(ADDRESS)),
        SET_INT = var("curl_easy_setopt", JAVA_INT),
        SET_LONG = var("curl_easy_setopt", JAVA_LONG),
        SET_PTR = var("curl_easy_setopt", ADDRESS),
        GET_PTR = var("curl_easy_getinfo", ADDRESS);

    static {
        try { var rc = (int) GLOBAL_INIT.invokeExact(3); if (rc != 0) throw new IllegalStateException("curl_global_init " + rc); }
        catch (Throwable t) { throw new ExceptionInInitializerError(t); }
    }

    static MethodHandle fn(String name, FunctionDescriptor d) {
        return L.downcallHandle(LOOKUP.find(name).orElseThrow(() -> new IllegalStateException("not exported by libcurl-impersonate: " + name)), d);
    }

    static MethodHandle var(String name, MemoryLayout arg) {
        return L.downcallHandle(LOOKUP.find(name).orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, arg), Linker.Option.firstVariadicArg(2));
    }

    static String strerror(int rc) throws Throwable {
        return ((MemorySegment) STRERROR.invokeExact(rc)).reinterpret(200).getString(0);
    }

    private Lib() { }
}
