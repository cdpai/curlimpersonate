package cdpai.curlimpersonate.lib;

import module java.base;
import module java.net.http;

public final class GitHubRelease implements LibrarySource {
    static final String REPO = "lexiforest/curl-impersonate", PINNED = "v2.3.0";
    static final Map<Platform, String> PINNED_SHA256 = Map.of(
        Platform.WINDOWS_X64, "1ab495ca3ae0aab96e8800f7faadaae6971550e48266254ccb37a061ec66ece2",
        Platform.WINDOWS_ARM64, "63b958eade7ffd62eb514e53e0055f0881f583b048f7a2b8f3eebaf4fa69edda",
        Platform.LINUX_X64, "b287a1ffc2a5add27fa54f724b9915caaf79a1d965a1e730ebed4b4528a86cf2",
        Platform.LINUX_ARM64, "1b2ab0089581ea0bdf6bfeddfcf5914fdf70be0958eacc373a040c2db8452da9",
        Platform.MAC_X64, "0ac304126efef6de2cbebedd3e22f38cf2cddf76408db0e02e1d8583c4c9960f",
        Platform.MAC_ARM64, "7bee77efd2917c606647397d75d6b218e3bcd1234adf8bfbf28e2a7bd9cec8b4");

    final String version;
    final Platform platform;
    final Path home;

    GitHubRelease(String version, Platform platform, Path home) { this.version = version; this.platform = platform; this.home = home; }

    public static GitHubRelease pinned() { return of(PINNED); }

    public static GitHubRelease of(String version) { return new GitHubRelease(version, Platform.current(), defaultHome()); }

    public GitHubRelease home(Path home) { return new GitHubRelease(version, platform, home); }

    public static Path defaultHome() { return Path.of(System.getProperty("user.home"), "cdpai", "curlimpersonate"); }

    public String asset() { return "libcurl-impersonate-" + version + "." + platform.assetSuffix() + ".tar.gz"; }

    public Path libraryFile() { return home.resolve(version).resolve(platform.assetSuffix()).resolve(platform.libraryFile()); }

    public SymbolLookup open(Arena arena) throws IOException {
        var lib = libraryFile();
        if (!Files.isRegularFile(lib)) install(lib);
        return SymbolLookup.libraryLookup(lib, arena);
    }

    public String describe() { return REPO + " " + version + " (" + platform.assetSuffix() + ") cached in " + libraryFile().getParent(); }

    synchronized void install(Path lib) throws IOException {
        if (Files.isRegularFile(lib)) return;
        Files.createDirectories(lib.getParent());
        var archive = Files.createTempFile(lib.getParent(), "download-", ".tar.gz");
        var part = lib.resolveSibling(lib.getFileName() + "." + ProcessHandle.current().pid() + ".part");
        try {
            var url = "https://github.com/" + REPO + "/releases/download/" + version + "/" + asset();
            Downloads.fetch(URI.create(url), archive);
            var expected = version.equals(PINNED) ? PINNED_SHA256.get(platform) : Downloads.publishedSha256(REPO, version, asset());
            var actual = Downloads.sha256(archive);
            if (!actual.equalsIgnoreCase(expected)) throw new IOException(asset() + ": sha256 " + actual + ", expected " + expected);
            if (!TarGz.extractFirst(archive, platform::isLibrary, part)) throw new IOException(asset() + " holds no libcurl-impersonate library");
            Files.move(part, lib, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(archive);
            Files.deleteIfExists(part);
        }
    }
}
