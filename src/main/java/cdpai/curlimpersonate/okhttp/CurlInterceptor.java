package cdpai.curlimpersonate.okhttp;

import module java.base;
import cdpai.curlimpersonate.Http;
import okhttp3.*;
import okio.Buffer;

public final class CurlInterceptor implements Interceptor {
    final Http http;
    final boolean keepUserAgent;

    public CurlInterceptor(Http http, boolean keepUserAgent) { this.http = http; this.keepUserAgent = keepUserAgent; }

    public static CurlInterceptor chrome152() { return new CurlInterceptor(Http.chrome152(), false); }

    @Override public Response intercept(Chain chain) throws IOException {
        var req = chain.request();
        var headers = new ArrayList<String>();
        var cookie = (String) null;
        for (var name : req.headers().names()) for (var v : req.headers(name)) {
            if (name.equalsIgnoreCase("Cookie")) cookie = cookie == null ? v : cookie + "; " + v;
            else if (keepUserAgent || !name.equalsIgnoreCase("User-Agent")) headers.add(name + ": " + v);
        }
        var bytes = (byte[]) null;
        if (req.body() != null) {
            var buf = new Buffer();
            req.body().writeTo(buf);
            bytes = buf.readByteArray();
            var type = req.body().contentType();
            if (type != null && req.header("Content-Type") == null) headers.add("Content-Type: " + type);
        }
        try {
            return RawResponse.of(req, http.exchange(req.method(), req.url().toString(), headers, cookie, bytes));
        } catch (IOException e) { throw e; }
        catch (Exception e) { throw new IOException(e.getMessage(), e); }
    }
}
