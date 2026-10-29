package cdpai.curlimpersonate.lib;

import module java.base;

public interface LibrarySource {
    SymbolLookup open(Arena arena) throws IOException;

    String describe();

    static LibrarySource path(Path file) { return new ExplicitPath(file); }

    static LibrarySource environmentVariable(String name) { return new EnvironmentVariable(name); }

    static LibrarySource systemLibraryPath() { return new SystemLibraryPath(); }

    static LibrarySource gitHubRelease() { return GitHubRelease.pinned(); }

    static LibrarySource gitHubRelease(String version) { return GitHubRelease.of(version); }
}
