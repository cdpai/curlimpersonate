# cdpai-curlimpersonate

A small Java (FFM, JDK 25) binding to [curl-impersonate](https://github.com/lexiforest/curl-impersonate)'s `libcurl-impersonate`, so a Java process can send HTTP requests with a real browser's TLS and HTTP/2 fingerprint (Chrome 152 by default).

```java
var reply = Http.chrome152().get("https://example.com/", List.of("Accept: */*"), "name=value");
```

For OkHttp callers, `cdpai.curlimpersonate.okhttp.CurlInterceptor` sends each request through curl-impersonate instead of OkHttp's own connection (OkHttp is an optional dependency):

```java
var client = new OkHttpClient.Builder().addInterceptor(CurlInterceptor.chrome152()).build();
```

Where the native library comes from is a `LibrarySource`. The default, `LibrarySource.gitHubRelease()`, downloads the official libcurl-impersonate release for this platform from github.com/lexiforest/curl-impersonate (pinned to v2.3.0, SHA-256 checked) into `~/cdpai/curlimpersonate/<version>/<platform>/` on first use and loads it from there afterwards. Other sources: `LibrarySource.path(file)`, `environmentVariable(name)`, `systemLibraryPath()`, `gitHubRelease(version)` (checked against the digest GitHub publishes), or your own implementation. Choose one before the first request:

```java
NativeLibrary.use(LibrarySource.path(Path.of("/opt/curl-impersonate/libcurl-impersonate.so")));
```

`-Dcurlimpersonate.dll=<path>` is a shortcut for `LibrarySource.path`. `Http.problem()` returns why the library cannot load (or null), so a caller can fall back to a plain client.

Tests: `mvn test` (local server); `mvn test -Dcurlimpersonate.live=true` also checks the fingerprint against tls.peet.ws.
