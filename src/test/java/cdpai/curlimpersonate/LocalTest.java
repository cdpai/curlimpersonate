package cdpai.curlimpersonate;

import module java.base;
import module jdk.httpserver;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

class LocalTest {
    static HttpServer srv;
    static String base;

    @BeforeAll static void up() throws IOException {
        srv = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        srv.createContext("/echo", ex -> {
            var body = ex.getRequestBody().readAllBytes();
            var sha = HexFormat.of().formatHex(sha256(body));
            var h = ex.getRequestHeaders();
            var out = ("method=" + ex.getRequestMethod() + "\ncookie=" + h.getFirst("Cookie") + "\nua=" + h.getFirst("User-Agent") + "\nall=" + h.keySet() + "\nx=" + h.getFirst("X-Test") + "\nlen=" + body.length + "\nsha=" + sha + "\n").getBytes();
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        srv.createContext("/bounce", ex -> {
            ex.getResponseHeaders().add("Set-Cookie", "EARNED=yes; Path=/");
            ex.getResponseHeaders().add("Location", "/echo");
            ex.sendResponseHeaders(302, -1);
            ex.close();
        });
        srv.createContext("/zipped", ex -> {
            var wants = String.valueOf(ex.getRequestHeaders().getFirst("Accept-Encoding"));
            var raw = "decoded ok".getBytes();
            var bos = new ByteArrayOutputStream();
            try (var gz = new java.util.zip.GZIPOutputStream(bos)) { gz.write(raw); }
            var out = wants.contains("gzip") ? bos.toByteArray() : raw;
            if (wants.contains("gzip")) ex.getResponseHeaders().add("Content-Encoding", "gzip");
            ex.sendResponseHeaders(200, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        srv.start();
        base = "http://127.0.0.1:" + srv.getAddress().getPort() + "/echo";
    }

    @AfterAll static void down() { srv.stop(0); }

    static byte[] sha256(byte[] b) { try { return MessageDigest.getInstance("SHA-256").digest(b); } catch (Exception e) { throw new IllegalStateException(e); } }

    @Test void getSendsCookieFromMemoryAndChromeHeaders() throws Exception {
        var r = Http.chrome152().get(base, List.of("X-Test: yes"), "a=1; b=2");
        assertEquals(200, r.status());
        assertTrue(r.body().contains("method=GET") && r.body().contains("cookie=a=1; b=2") && r.body().contains("x=yes"), r.body());
        assertTrue(r.body().contains("Chrome/152"), r.body());
    }

    @Test void compressedAnswersComeBackDecoded() throws Exception {
        assertEquals("decoded ok", Http.chrome152().get(base.replace("/echo", "/zipped"), List.of(), null).body());
    }

    @Test void ownHeadersReplaceAndEmptyOnesRemoveTheBrowserDefaults() throws Exception {
        var plain = Http.chrome152().get(base, List.of(), null).body();
        assertTrue(plain.contains("Upgrade-insecure-requests") && plain.contains("Sec-fetch-user"), plain);
        var r = Http.chrome152().get(base, List.of("Accept: */*", "Sec-Fetch-Mode: cors", "Upgrade-Insecure-Requests:", "Sec-Fetch-User:"), null).body();
        assertFalse(r.contains("Upgrade-insecure-requests") || r.contains("Sec-fetch-user"), r);
        assertTrue(r.contains("Accept") && r.contains("Sec-fetch-mode"), r);
    }

    @Test void cookieEngineCarriesACookieEarnedMidRedirectToTheNextHop() throws Exception {
        var url = base.replace("/echo", "/bounce");
        var off = Http.chrome152().get(url, List.of(), "a=1");
        assertFalse(off.body().contains("EARNED=yes"), off.body());
        var on = Http.chrome152().withCookieEngine().get(url, List.of(), "a=1");
        assertTrue(on.body().contains("EARNED=yes") && on.body().contains("a=1"), on.body());
        assertTrue(on.headers().toLowerCase().contains("set-cookie: earned=yes"), on.headers());
    }

    @Test void postBytes() throws Exception {
        var data = "hello=world&n=1".getBytes();
        var r = Http.chrome152().post(base, List.of(), null, data);
        assertTrue(r.body().contains("method=POST") && r.body().contains("len=" + data.length) && r.body().contains("sha=" + HexFormat.of().formatHex(sha256(data))), r.body());
    }

    @Test void postFileStreamsTheWholeFile() throws Exception {
        var f = Files.createTempFile("curlimpersonate", ".bin");
        try {
            var data = new byte[5_000_003];
            new Random(7).nextBytes(data);
            Files.write(f, data);
            var r = Http.chrome152().postFile(base, List.of("Content-Type: application/octet-stream"), null, f);
            assertTrue(r.body().contains("len=" + data.length) && r.body().contains("sha=" + HexFormat.of().formatHex(sha256(data))), r.body());
        } finally { Files.deleteIfExists(f); }
    }
}
