package org.cobbzilla.util.http.main;

import org.cobbzilla.util.http.HttpRequestBean;
import org.cobbzilla.util.http.HttpResponseBean;
import org.cobbzilla.util.http.HttpUtil;
import org.cobbzilla.util.main.BaseMain;

import static org.cobbzilla.util.daemon.ZillaRuntime.readStdin;
import static org.cobbzilla.util.json.JsonUtil.json;

public class HttpMain extends BaseMain<HttpMainOptions> {

    public static void main (String[] args) { main(HttpMain.class, args); }

    @Override protected void run() throws Exception {
        final HttpRequestBean request = json(readStdin(), HttpRequestBean.class);
        if (request == null) die("nothing read from stdin");
        final HttpResponseBean response = HttpUtil.getResponse(request);
        if (response.isOk()) {
            out(response.getEntityString());
        } else {
            err(json(response.toMap()));
        }
    }

}
