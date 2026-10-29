package cdpai.curlimpersonate;

import datapotter.datahelper.Data;

@Data
public final class Reply extends Reply_A {
    int status, httpVersion;
    String url, headers, body;
}
