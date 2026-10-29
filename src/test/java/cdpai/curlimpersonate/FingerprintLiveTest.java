package cdpai.curlimpersonate;

import module java.base;
import module java.net.http;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "curlimpersonate.live", matches = "true")
class FingerprintLiveTest {
    static final String URL = "https://tls.peet.ws/api/clean";
    static final String CHROME_JA4 = "t13d1517h2_8daaf6152771";

    static String ja4(String json) {
        var m = java.util.regex.Pattern.compile("\"ja4\":\s*\"([^\"]+)\"").matcher(json);
        assertTrue(m.find(), json);
        return m.group(1);
    }

    @Test void plainJavaHasAnotherFingerprintThanChrome() throws Exception {
        var java = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(URL)).build(), HttpResponse.BodyHandlers.ofString()).body();
        assertFalse(ja4(java).startsWith(CHROME_JA4), ja4(java));
    }

    @Test void bindingHasTheChromeFingerprintOverHttp2() throws Exception {
        var r = Http.chrome152().get(URL, List.of(), null);
        assertEquals(200, r.status());
        assertEquals(Http.HTTP2, r.httpVersion());
        assertTrue(ja4(r.body()).startsWith(CHROME_JA4), ja4(r.body()));
    }
}
