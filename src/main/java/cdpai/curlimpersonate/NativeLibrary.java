package cdpai.curlimpersonate;

import module java.base;
import cdpai.curlimpersonate.lib.LibrarySource;

public final class NativeLibrary {
    public static final String PROPERTY = "curlimpersonate.dll";
    static volatile LibrarySource chosen;
    static volatile LibrarySource loadedFrom;

    public static void use(LibrarySource source) {
        if (loadedFrom != null) throw new IllegalStateException("curl-impersonate is already loaded from " + loadedFrom.describe());
        chosen = source;
    }

    public static LibrarySource source() {
        var s = chosen;
        if (s != null) return s;
        var p = System.getProperty(PROPERTY);
        return p != null && !p.isBlank() ? LibrarySource.path(Path.of(p)) : LibrarySource.gitHubRelease();
    }

    static SymbolLookup open(Arena arena) {
        var s = source();
        loadedFrom = s;
        try { return s.open(arena); }
        catch (IOException e) { throw new IllegalStateException("curl-impersonate from " + s.describe() + ": " + e.getMessage(), e); }
    }

    private NativeLibrary() { }
}
