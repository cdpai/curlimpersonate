package cdpai.curlimpersonate.lib;

import module java.base;

final class TarGz {
    static final int BLOCK = 512;

    static boolean extractFirst(Path archive, Predicate<String> wanted, Path out) throws IOException {
        try (var in = new BufferedInputStream(new GZIPInputStream(Files.newInputStream(archive)))) {
            var header = new byte[BLOCK];
            while (in.readNBytes(header, 0, BLOCK) == BLOCK && header[0] != 0) {
                var name = field(header, 345, 155).isEmpty() ? field(header, 0, 100) : field(header, 345, 155) + "/" + field(header, 0, 100);
                var size = Long.parseLong(field(header, 124, 12).trim().isEmpty() ? "0" : field(header, 124, 12).trim(), 8);
                var regular = header[156] == '0' || header[156] == 0;
                var padded = (size + BLOCK - 1) / BLOCK * BLOCK;
                if (regular && wanted.test(name)) {
                    try (var o = Files.newOutputStream(out)) { copy(in, o, size); }
                    return true;
                }
                in.skipNBytes(padded);
            }
        }
        return false;
    }

    static void copy(InputStream in, OutputStream out, long size) throws IOException {
        var buf = new byte[65536];
        for (var left = size; left > 0; ) {
            var n = in.read(buf, 0, (int) Math.min(buf.length, left));
            if (n < 0) throw new EOFException("archive ended inside an entry");
            out.write(buf, 0, n);
            left -= n;
        }
    }

    static String field(byte[] h, int off, int len) {
        var end = off;
        while (end < off + len && h[end] != 0) end++;
        return new String(h, off, end - off, StandardCharsets.US_ASCII);
    }
}
