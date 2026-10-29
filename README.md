# cdpai-curlimpersonate

A small Java (FFM, JDK 25) binding to [curl-impersonate](https://github.com/lexiforest/curl-impersonate)'s `libcurl-impersonate`, so a Java process can send HTTP requests with a real browser's TLS and HTTP/2 fingerprint (Chrome 152 by default).

```java
var reply = Http.chrome152().get("https://example.com/", List.of("Accept: */*"), "name=value");
```

For OkHttp callers, `cdpai.curlimpersonate.okhttp.CurlInterceptor` sends each request through curl-impersonate instead of OkHttp's own connection (OkHttp is an optional dependency):

```java
var client = new OkHttpClient.Builder().addInterceptor(CurlInterceptor.chrome152()).build();
```

The native library is found from `-Dcurlimpersonate.dll=<path>`, then the `CURLIMPERSONATE_DLL` environment variable, then the system library path. `Http.problem()` returns why it cannot load (or null), so a caller can fall back to a plain client.

Tests: `mvn test` (local server); `mvn test -Dcurlimpersonate.live=true` also checks the fingerprint against tls.peet.ws.
