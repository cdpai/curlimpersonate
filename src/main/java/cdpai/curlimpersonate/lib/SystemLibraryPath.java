package cdpai.curlimpersonate.lib;

import module java.base;

final class SystemLibraryPath implements LibrarySource {
    public SymbolLookup open(Arena arena) throws IOException {
        try { return SymbolLookup.libraryLookup(Platform.current().libraryFile(), arena); }
        catch (IllegalArgumentException e) { throw new FileNotFoundException(Platform.current().libraryFile() + " is not on the system library path"); }
    }

    public String describe() { return "system library path"; }
}
