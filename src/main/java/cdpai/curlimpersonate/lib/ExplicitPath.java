package cdpai.curlimpersonate.lib;

import module java.base;

final class ExplicitPath implements LibrarySource {
    final Path file;

    ExplicitPath(Path file) { this.file = file; }

    public SymbolLookup open(Arena arena) throws IOException {
        if (!Files.isRegularFile(file)) throw new FileNotFoundException(file.toString());
        return SymbolLookup.libraryLookup(file, arena);
    }

    public String describe() { return "file " + file; }
}
