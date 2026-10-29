package cdpai.curlimpersonate.lib;

import module java.base;

public enum Platform {
    WINDOWS_X64("x86_64-win32", "libcurl-impersonate.dll"),
    WINDOWS_ARM64("arm64-win32", "libcurl-impersonate.dll"),
    LINUX_X64("x86_64-linux-gnu", "libcurl-impersonate.so"),
    LINUX_ARM64("aarch64-linux-gnu", "libcurl-impersonate.so"),
    MAC_X64("x86_64-macos", "libcurl-impersonate.dylib"),
    MAC_ARM64("arm64-macos", "libcurl-impersonate.dylib");

    final String assetSuffix, libraryFile;

    Platform(String assetSuffix, String libraryFile) { this.assetSuffix = assetSuffix; this.libraryFile = libraryFile; }

    public String assetSuffix() { return assetSuffix; }

    public String libraryFile() { return libraryFile; }

    boolean isLibrary(String entryName) {
        var name = entryName.substring(entryName.lastIndexOf('/') + 1);
        return name.matches("libcurl-impersonate(\\.\\d+)*\\.(dll|dylib)|libcurl-impersonate\\.so(\\.\\d+)*");
    }

    public static Platform current() {
        var os = System.getProperty("os.name").toLowerCase();
        var arm = System.getProperty("os.arch").toLowerCase().matches("aarch64|arm64");
        if (os.contains("win")) return arm ? WINDOWS_ARM64 : WINDOWS_X64;
        if (os.contains("mac")) return arm ? MAC_ARM64 : MAC_X64;
        if (os.contains("linux")) return arm ? LINUX_ARM64 : LINUX_X64;
        throw new IllegalStateException("no curl-impersonate build for " + os + " " + System.getProperty("os.arch"));
    }
}
