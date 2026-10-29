package cdpai.curlimpersonate;

import module java.base;
import module jdk.httpserver;
import static org.junit.jupiter.api.Assertions.*;
import cdpai.curlimpersonate.okhttp.CurlInterceptor;
import okhttp3.*;
import org.junit.jupiter.api.*;

class OkHttpAdapterTest {
    static HttpServer srv;
    static String base;
    static final byte[] BIN = new byte[]{0, 1, 2, (byte) 0xFF, (byte) 0xC3, 0x28, 10, 13};

    @BeforeAll static void up() throws IOException {
        srv = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        srv.createContext("/echo", ex -> {
            var body = new String(ex.getRequestBody().readAllBytes());
            var h = ex.getRequestHeaders();
            var out = ("method=" + ex.getRequestMethod() + "\ncookie=" + h.getFirst("Cookie") + "\nua=" + h.getFirst("User-Agent") + "\nx=" + h.getFirst("X-Test") + "\ntype=" + h.getFirst("Content-Type") + "\nbody=" + body + "\n").getBytes();
            ex.getResponseHeaders().add("X-Back", "yes");
            ex.sendResponseHeaders(201, out.length);
            ex.getResponseBody().write(out);
            ex.close();
        });
        srv.createContext("/bin", ex -> {
            var bos = new ByteArrayOutputStream();
            try (var gz = new java.util.zip.GZIPOutputStream(bos)) { gz.write(BIN); }
            ex.getResponseHeaders().add("Content-Encoding", "gzip");
            ex.getResponseHeaders().add("Content-Type", "application/octet-stream");
            ex.sendResponseHeaders(200, bos.size());
            ex.getResponseBody().write(bos.toByteArray());
            ex.close();
        });
        srv.start();
        base = "http://127.0.0.1:" + srv.getAddress().getPort();
    }

    @AfterAll static void down() { srv.stop(0); }

    static OkHttpClient client() { return new OkHttpClient.Builder().addInterceptor(CurlInterceptor.chrome152()).build(); }

    @Test void getCarriesHeadersCookieAndChromeAgent() throws Exception {
        var req = new Request.Builder().url(base + "/echo").header("X-Test", "yes").header("Cookie", "a=1").header("User-Agent", "okhttp-caller").build();
        try (var r = client().newCall(req).execute()) {
            var body = r.body().string();
            assertEquals(201, r.code());
            assertEquals("yes", r.header("X-Back"));
            assertTrue(body.contains("method=GET") && body.contains("cookie=a=1") && body.contains("x=yes"), body);
            assertTrue(body.contains("Chrome/152") && !body.contains("okhttp-caller"), body);
        }
    }

    @Test void postAndPutSendBodyAndType() throws Exception {
        for (var method : List.of("POST", "PUT")) {
            var req = new Request.Builder().url(base + "/echo").method(method, RequestBody.create("{\"k\":1}", MediaType.parse("application/json"))).build();
            try (var r = client().newCall(req).execute()) {
                var body = r.body().string();
                assertTrue(body.contains("method=" + method) && body.contains("body={\"k\":1}") && body.contains("type=application/json"), body);
            }
        }
    }

    @Test void compressedBinaryComesBackByteForByte() throws Exception {
        try (var r = client().newCall(new Request.Builder().url(base + "/bin").build()).execute()) {
            assertArrayEquals(BIN, r.body().bytes());
            assertNull(r.header("Content-Encoding"));
        }
    }
}
