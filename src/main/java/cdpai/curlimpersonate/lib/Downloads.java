package cdpai.curlimpersonate.lib;

import module java.base;
import module java.net.http;

final class Downloads {
    static final HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(30)).build();

    static void fetch(URI url, Path to) throws IOException {
        var r = send(HttpRequest.newBuilder(url).timeout(Duration.ofMinutes(10)).build(), HttpResponse.BodyHandlers.ofFile(to));
        if (r.statusCode() != 200) throw new IOException(url + ": HTTP " + r.statusCode());
    }

    static String publishedSha256(String repo, String version, String asset) throws IOException {
        var api = URI.create("https://api.github.com/repos/" + repo + "/releases/tags/" + version);
        var r = send(HttpRequest.newBuilder(api).header("Accept", "application/vnd.github+json").build(), HttpResponse.BodyHandlers.ofString());
        if (r.statusCode() != 200) throw new IOException(api + ": HTTP " + r.statusCode());
        var m = Pattern.compile("\"name\"\\s*:\\s*\"" + Pattern.quote(asset) + "\".*?\"digest\"\\s*:\\s*\"sha256:([0-9a-f]{64})\"", Pattern.DOTALL).matcher(r.body());
        if (!m.find()) throw new IOException(repo + " " + version + " publishes no sha256 for " + asset);
        return m.group(1);
    }

    static String sha256(Path file) throws IOException {
        try (var in = Files.newInputStream(file)) {
            var md = MessageDigest.getInstance("SHA-256");
            var buf = new byte[65536];
            for (int n; (n = in.read(buf)) > 0; ) md.update(buf, 0, n);
            return HexFormat.of().formatHex(md.digest());
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    static <T> HttpResponse<T> send(HttpRequest req, HttpResponse.BodyHandler<T> body) throws IOException {
        try { return CLIENT.send(req, body); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new InterruptedIOException(e.getMessage()); }
    }
}
