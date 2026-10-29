package cdpai.curlimpersonate;

import module java.base;

public final class Http {
    public static final String CHROME_152 = "chrome152";
    public static final int HTTP2 = 3;
    final String target;
    final int timeoutMs;
    final boolean cookieEngine;

    public Http(String target, int timeoutSeconds) { this(target, timeoutSeconds, false); }

    Http(String target, int timeoutSeconds, boolean cookieEngine) { this.target = target; this.timeoutMs = timeoutSeconds * 1000; this.cookieEngine = cookieEngine; }

    /** Same client, with curl's in-memory cookie engine on: a Set-Cookie met part-way along a redirect chain is sent on the next hop. Nothing is kept between calls. */
    public Http withCookieEngine() { return new Http(target, timeoutMs / 1000, true); }

    public static Http chrome152() { return new Http(CHROME_152, 60); }

    /** Null when the native library loads on this machine, else why not (missing DLL, wrong platform). Lets a caller fall back instead of dying at the first request. */
    public static String problem() {
        try { return Lib.L == null ? "no native linker" : null; }
        catch (Throwable t) { return String.valueOf(t.getCause() != null ? t.getCause() : t); }
    }

    public Reply get(String url, List<String> headers, String cookie) throws Exception { return send("GET", url, headers, cookie, null, null).reply(); }

    public Reply post(String url, List<String> headers, String cookie, byte[] body) throws Exception { return send("POST", url, headers, cookie, body, null).reply(); }

    public Reply postFile(String url, List<String> headers, String cookie, Path file) throws Exception { return send("POST", url, headers, cookie, null, file).reply(); }

    public Reply request(String method, String url, List<String> headers, String cookie, byte[] body) throws Exception { return send(method, url, headers, cookie, body, null).reply(); }

    public Exchange exchange(String method, String url, List<String> headers, String cookie, byte[] body) throws Exception { return send(method, url, headers, cookie, body, null); }

    Exchange send(String method, String url, List<String> headers, String cookie, byte[] body, Path file) throws Exception {
        var out = new Sink();
        var head = new Sink();
        try (var e = new Easy(target)) {
            e.setS(Opt.URL, url).setS(Opt.ACCEPT_ENCODING, "gzip, deflate, br, zstd").setI(Opt.NOSIGNAL, 1).setI(Opt.FOLLOWLOCATION, 1).setI(Opt.TIMEOUT_MS, timeoutMs).setI(Opt.CONNECTTIMEOUT_MS, 30000).headers(headers);
            if (!method.equals("GET") && !method.equals("POST")) e.setS(Opt.CUSTOMREQUEST, method);
            if (cookieEngine) e.setS(Opt.COOKIEFILE, "");
            if (cookie != null && !cookie.isEmpty()) e.setS(Opt.COOKIE, cookie);
            e.setP(Opt.WRITEFUNCTION, out.stub(e.arena)).setP(Opt.HEADERFUNCTION, head.stub(e.arena));
            if (body != null) {
                var seg = e.arena.allocate(Math.max(1, body.length));
                MemorySegment.copy(body, 0, seg, java.lang.foreign.ValueLayout.JAVA_BYTE, 0, body.length);
                e.setI(Opt.POST, 1).setO(Opt.POSTFIELDSIZE_LARGE, body.length).setP(Opt.POSTFIELDS, seg);
            }
            if (file != null) {
                try (var feed = new FileFeed(file)) {
                    e.setI(Opt.POST, 1).setO(Opt.POSTFIELDSIZE_LARGE, Files.size(file)).setP(Opt.READFUNCTION, feed.stub(e.arena));
                    e.perform();
                    return reply(e, out, head);
                }
            }
            e.perform();
            return reply(e, out, head);
        } catch (Exception | Error x) { throw x; }
        catch (Throwable t) { throw new IllegalStateException(t); }
    }

    static Exchange reply(Easy e, Sink body, Sink head) throws Throwable {
        return new Exchange(new Reply().status(e.infoInt(Opt.INFO_RESPONSE_CODE)).httpVersion(e.infoInt(Opt.INFO_HTTP_VERSION)).url(e.infoStr(Opt.INFO_EFFECTIVE_URL))
            .headers(head.bytes.toString(StandardCharsets.ISO_8859_1)).body(body.bytes.toString(StandardCharsets.UTF_8)), body.bytes.toByteArray());
    }
}
