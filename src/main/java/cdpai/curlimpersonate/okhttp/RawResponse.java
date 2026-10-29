package cdpai.curlimpersonate.okhttp;

import module java.base;
import cdpai.curlimpersonate.Http;
import cdpai.curlimpersonate.Exchange;
import okhttp3.*;

final class RawResponse {
    static final Set<String> DROPPED = Set.of("content-encoding", "content-length", "transfer-encoding");

    static Response of(Request req, Exchange x) {
        var r = x.reply();
        var blocks = r.headers().split("\r\n\r\n");
        var last = blocks.length == 0 ? "" : blocks[blocks.length - 1];
        var lines = last.split("\r\n");
        var headers = new Headers.Builder();
        for (var i = 1; i < lines.length; i++) {
            var c = lines[i].indexOf(':');
            if (c <= 0) continue;
            var name = lines[i].substring(0, c).trim();
            if (!DROPPED.contains(name.toLowerCase())) headers.addUnsafeNonAscii(name, lines[i].substring(c + 1).trim());
        }
        var type = headers.get("Content-Type");
        return new Response.Builder()
            .request(req.newBuilder().url(r.url()).build())
            .protocol(r.httpVersion() == Http.HTTP2 ? Protocol.HTTP_2 : Protocol.HTTP_1_1)
            .code(r.status())
            .message(lines.length > 0 ? message(lines[0]) : "")
            .headers(headers.build())
            .body(ResponseBody.create(x.body(), type == null ? null : MediaType.parse(type)))
            .build();
    }

    static String message(String statusLine) {
        var parts = statusLine.split(" ", 3);
        return parts.length == 3 ? parts[2] : "";
    }
}
