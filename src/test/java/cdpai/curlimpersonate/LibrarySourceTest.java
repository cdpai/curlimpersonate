package cdpai.curlimpersonate;

import module java.base;
import static org.junit.jupiter.api.Assertions.*;
import cdpai.curlimpersonate.lib.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

class LibrarySourceTest {
    static final String WINDOWS_X64_DLL_SHA256 = "5fa66e3c1920a73f09b30c7138c71316ad8df84fa84407c3f3b588f229e8de6d";

    @Test void explicitPathToAMissingFileSaysWhichFile() {
        var e = assertThrows(IOException.class, () -> LibrarySource.path(Path.of("no-such-dir", "x.dll")).open(Arena.ofAuto()));
        assertTrue(e.getMessage().contains("x.dll"), e.getMessage());
    }

    @Test void unsetEnvironmentVariableSaysWhichVariable() {
        var e = assertThrows(IOException.class, () -> LibrarySource.environmentVariable("CDPAI_TEST_UNSET_VARIABLE").open(Arena.ofAuto()));
        assertTrue(e.getMessage().contains("CDPAI_TEST_UNSET_VARIABLE"), e.getMessage());
    }

    @Test void pinnedAssetNameMatchesTheReleaseNaming() {
        assertEquals("libcurl-impersonate-v2.3.0." + Platform.current().assetSuffix() + ".tar.gz", GitHubRelease.pinned().asset());
    }

    @EnabledIfSystemProperty(named = "curlimpersonate.live", matches = "true")
    @Test void gitHubReleaseDownloadsVerifiesExtractsAndLoads() throws Exception {
        var home = Files.createTempDirectory("curlimpersonate-home");
        var src = GitHubRelease.pinned().home(home);
        try (var arena = Arena.ofConfined()) {
            assertTrue(src.open(arena).find("curl_easy_impersonate").isPresent());
        }
        var lib = src.libraryFile();
        assertTrue(Files.isRegularFile(lib), lib.toString());
        try (var files = Files.list(lib.getParent())) { assertEquals(1, files.count(), "only the library is left behind"); }
        if (Platform.current() == Platform.WINDOWS_X64) {
            var sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(lib)));
            assertEquals(WINDOWS_X64_DLL_SHA256, sha);
        }
    }
}
