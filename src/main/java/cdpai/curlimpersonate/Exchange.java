package cdpai.curlimpersonate;

public final class Exchange {
    final Reply reply;
    final byte[] body;

    Exchange(Reply reply, byte[] body) { this.reply = reply; this.body = body; }

    public Reply reply() { return reply; }

    public byte[] body() { return body; }
}
