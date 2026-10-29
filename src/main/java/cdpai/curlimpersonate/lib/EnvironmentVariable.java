package cdpai.curlimpersonate.lib;

import module java.base;

final class EnvironmentVariable implements LibrarySource {
    final String name;

    EnvironmentVariable(String name) { this.name = name; }

    public SymbolLookup open(Arena arena) throws IOException {
        var value = System.getenv(name);
        if (value == null || value.isBlank()) throw new FileNotFoundException("environment variable " + name + " is not set");
        return new ExplicitPath(Path.of(value)).open(arena);
    }

    public String describe() { return "environment variable " + name; }
}
