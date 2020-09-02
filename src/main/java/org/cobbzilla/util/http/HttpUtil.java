package org.cobbzilla.util.http;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.cobbzilla.util.collection.NameAndValue;
import org.cobbzilla.util.string.StringUtil;
import org.cobbzilla.util.system.CommandResult;
import org.cobbzilla.util.system.CommandShell;
import org.cobbzilla.util.system.Sleep;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.google.common.net.HttpHeaders.CONTENT_DISPOSITION;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.apache.http.HttpHeaders.*;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.http.HttpContentTypes.MULTIPART_FORM_DATA;
import static org.cobbzilla.util.http.HttpContentTypes.contentType;
import static org.cobbzilla.util.http.HttpMethods.*;
import static org.cobbzilla.util.http.HttpStatusCodes.NO_CONTENT;
import static org.cobbzilla.util.http.URIUtil.getFileExt;
import static org.cobbzilla.util.io.FileUtil.getDefaultTempDir;
import static org.cobbzilla.util.json.JsonUtil.COMPACT_MAPPER;
import static org.cobbzilla.util.json.JsonUtil.json;
import static org.cobbzilla.util.security.CryptStream.BUFFER_SIZE;
import static org.cobbzilla.util.string.StringUtil.CRLF;
import static org.cobbzilla.util.string.StringUtil.urlEncode;
import static org.cobbzilla.util.system.Sleep.sleep;
import static org.cobbzilla.util.time.TimeUtil.DATE_FORMAT_LAST_MODIFIED;

@Slf4j
public class HttpUtil {

    public static final String CHUNKED_ENCODING = "chunked";
    public static final String USER_AGENT_CURL = "curl/7.64.1";

    public static Map<String, String> queryParams(URL url) throws UnsupportedEncodingException {
        return queryParams(url, StringUtil.UTF8);
    }

    // from: http://stackoverflow.com/a/13592567
    public static Map<String, String> queryParams(URL url, String encoding) throws UnsupportedEncodingException {
        final Map<String, String> query_pairs = new LinkedHashMap<>();
        final String query = url.getQuery();
        final String[] pairs = query.split("&");
        for (String pair : pairs) {
            final int idx = pair.indexOf("=");
            query_pairs.put(URLDecoder.decode(pair.substring(0, idx), encoding), URLDecoder.decode(pair.substring(idx + 1), encoding));
        }
        return query_pairs;
    }

    public static InputStream getUrlInputStream(String url) throws IOException { return get(url); }

    public static InputStream get (String urlString) throws IOException { return get(urlString, null); }

    public static InputStream get (String urlString, long connectTimeout) throws IOException {
        return get(urlString, null, null, connectTimeout);
    }

    public static InputStream get (String urlString, Map<String, String> headers) throws IOException {
        return get(urlString, headers, null);
    }

    public static InputStream get (String urlString, Map<String, String> headers, Map<String, String> headers2) throws IOException {
        return get(urlString, headers, headers2, null);
    }

    public static InputStream get (String urlString, Map<String, String> headers, Map<String, String> headers2, Long connectTimeout) throws IOException {
        final URL url = new URL(urlString);
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (connectTimeout != null) urlConnection.setConnectTimeout(connectTimeout.intValue());
        if (headers != null) addHeaders(urlConnection, headers);
        if (headers2 != null) addHeaders(urlConnection, headers2);
        return urlConnection.getInputStream();
    }

    public static InputStream post (String urlString, InputStream data, String multipartFileName, Map<String, String> headers, Map<String, String> headers2) throws IOException {
        return upload(urlString, POST, multipartFileName, data, headers, headers2);
    }

    public static InputStream put (String urlString, InputStream data, String multipartFileName, Map<String, String> headers, Map<String, String> headers2) throws IOException {
        return upload(urlString, PUT, multipartFileName, data, headers, headers2);
    }

    public static InputStream upload (String urlString, String method, String multipartFileName, InputStream data, Map<String, String> headers, Map<String, String> headers2) throws IOException {
        final URL url = new URL(urlString);
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod(method);
        if (headers != null) addHeaders(urlConnection, headers);
        if (headers2 != null) addHeaders(urlConnection, headers2);
        if (data != null) {
            urlConnection.setDoOutput(true);
            OutputStream upload = null;
            try {
                if (multipartFileName != null) {
                    final String boundary = randomAlphanumeric(10)+"_"+now();
                    urlConnection.setRequestProperty(CONTENT_TYPE, MULTIPART_FORM_DATA+"; boundary="+boundary);
                    urlConnection.setRequestProperty(TRANSFER_ENCODING, CHUNKED_ENCODING);
                    urlConnection.setChunkedStreamingMode(4096);

                    final HttpEntity entity = MultipartEntityBuilder.create()
                            .setBoundary(boundary)
                            .setLaxMode()
                            .addBinaryBody(multipartFileName, data, ContentType.APPLICATION_OCTET_STREAM, multipartFileName)
                            .build();

                    upload = urlConnection.getOutputStream();
                    entity.writeTo(upload);

                } else {
                    upload = urlConnection.getOutputStream();
                    IOUtils.copyLarge(data, upload);
                }
            } finally {
                if (upload != null) upload.close();
            }
        }
        return urlConnection.getInputStream();
    }

    public static void addHeaders(HttpURLConnection urlConnection, Map<String, String> headers) {
        for (Map.Entry<String, String> h : headers.entrySet()) {
            urlConnection.setRequestProperty(h.getKey(), h.getValue());
        }
    }

    public static HttpResponseBean upload (String url,
                                           File file,
                                           Map<String, String> headers) throws IOException {
        @Cleanup final CloseableHttpClient client = HttpClients.createDefault();
        final HttpPost method = new HttpPost(url);
        final FileBody fileBody = new FileBody(file);
        MultipartEntityBuilder builder = MultipartEntityBuilder.create().addPart("file", fileBody);
        method.setEntity(builder.build());

        if (headers != null) {
            for (Map.Entry<String, String> header : headers.entrySet()) {
                method.addHeader(new BasicHeader(header.getKey(), header.getValue()));
            }
        }

        @Cleanup final CloseableHttpResponse response = client.execute(method);

        final HttpResponseBean responseBean = new HttpResponseBean()
                .setEntityBytes(EntityUtils.toByteArray(response.getEntity()))
                .setHttpHeaders(response.getAllHeaders())
                .setStatus(response.getStatusLine().getStatusCode());
        return responseBean;
    }

    public static final int DEFAULT_RETRIES = 3;

    public static File url2file (String url) throws IOException {
        return url2file(url, null, DEFAULT_RETRIES);
    }
    public static File url2file (String url, String file) throws IOException {
        return url2file(url, file == null ? null : new File(file), DEFAULT_RETRIES);
    }
    public static File url2file (String url, File file) throws IOException {
        return url2file(url, file, DEFAULT_RETRIES);
    }
    public static File url2file (String url, File file, int retries) throws IOException {
        if (file == null) file = File.createTempFile("url2file-", getFileExt((url)), getDefaultTempDir());
        IOException lastException = null;
        long sleep = 100;
        for (int i=0; i<retries; i++) {
            try {
                @Cleanup final InputStream in = get(url);
                @Cleanup final OutputStream out = new FileOutputStream(file);
                IOUtils.copy(in, out);
                lastException = null;
                break;
            } catch (IOException e) {
                lastException = e;
                sleep(sleep, "waiting to possibly retry after IOException: "+lastException);
                sleep *= 5;
            }
        }
        if (lastException != null) throw lastException;
        return file;
    }

    public static String url2string (String url) throws IOException {
        @Cleanup final InputStream in = get(url);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        return out.toString();
    }

    public static String url2string (String url, long connectTimeout) throws IOException {
        @Cleanup final InputStream in = get(url, connectTimeout);
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        return out.toString();
    }

    public static HttpResponseBean getResponse(HttpRequestBean requestBean) throws IOException {
        final HttpClientBuilder clientBuilder = requestBean.initClientBuilder(HttpClients.custom());
        @Cleanup final CloseableHttpClient client = clientBuilder.build();
        return getResponse(requestBean, client);
    }

    public static HttpResponseBean getResponse(HttpRequestBean requestBean, HttpClient client) throws IOException {

        if (requestBean.hasStream()) return getStreamResponse(requestBean);

        final HttpResponseBean bean = new HttpResponseBean();

        final HttpUriRequest request = initHttpRequest(requestBean);

        if (requestBean.hasHeaders()) {
            for (NameAndValue header : requestBean.getHeaders()) {
                request.setHeader(header.getName(), header.getValue());
            }
        }

        final HttpResponse response = client.execute(request);

        for (Header header : response.getAllHeaders()) {
            bean.addHeader(header.getName(), header.getValue());
        }

        bean.setStatus(response.getStatusLine().getStatusCode());
        if (response.getStatusLine().getStatusCode() != NO_CONTENT && response.getEntity() != null) {
            bean.setContentLength(response.getEntity().getContentLength());
            final Header contentType = response.getEntity().getContentType();
            if (contentType != null) {
                bean.setContentType(contentType.getValue());
            }
            @Cleanup final InputStream content = response.getEntity().getContent();
            bean.setEntity(content);
        }

        return bean;
    }

    public static HttpResponseBean getStreamResponse(HttpRequestBean request) {
        if (!request.hasStream()) return die("getStreamResponse: request stream was not set");
        try {
            final String boundary = hexnow();
            request.withHeader(CONTENT_TYPE, "multipart/form-data; boundary=" + boundary);

            @Cleanup("disconnect") final HttpURLConnection connection = (HttpURLConnection) new URL(request.getUri()).openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod(request.getMethod());
            if (!request.hasContentLength() || (request.hasContentLength() && request.getContentLength() > BUFFER_SIZE)) {
                connection.setChunkedStreamingMode(BUFFER_SIZE);
            }
            for (NameAndValue header : request.getHeaders()) {
                connection.setRequestProperty(header.getName(), header.getValue());
            }

            @Cleanup final OutputStream output = connection.getOutputStream();
            @Cleanup final PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, Charset.defaultCharset()), true);
            writer.append("--").append(boundary).append(CRLF);
            final String filename = request.getEntity();
            addStreamHeader(writer, CONTENT_DISPOSITION, "form-data; name=\"file\"; filename=\""+ filename +"\"");
            addStreamHeader(writer, CONTENT_TYPE, contentType(filename));
            writer.append(CRLF).flush();
            IOUtils.copy(request.getEntityInputStream(), output);
            output.flush();
            writer.append(CRLF);
            writer.append("--").append(boundary).append("--").append(CRLF).flush();

            final HttpResponseBean response = new HttpResponseBean()
                    .setStatus(connection.getResponseCode())
                    .setHttpHeaders(connection.getHeaderFields());
            if (!request.discardResponseEntity()) {
                try {
                    response.setEntity(connection.getInputStream());
                } catch (IOException ioe) {
                    response.setEntity(connection.getErrorStream());
                }
            }
            return response;
        } catch (Exception e) {
            return die("getStreamResponse: "+e, e);
        }
    }

    private static PrintWriter addStreamHeader(PrintWriter writer, String name, String value) {
        writer.append(name).append(": ").append(value).append(CRLF);
        return writer;
    }

    public static HttpResponseBean getResponse(String urlString) throws IOException {

        final HttpResponseBean bean = new HttpResponseBean();
        @Cleanup final CloseableHttpClient client = HttpClients.createDefault();
        final HttpResponse response = client.execute(new HttpGet(urlString.trim()));

        for (Header header : response.getAllHeaders()) {
            bean.addHeader(header.getName(), header.getValue());
        }

        bean.setStatus(response.getStatusLine().getStatusCode());
        if (response.getEntity() != null) {
            final Header contentType = response.getEntity().getContentType();
            if (contentType != null) bean.setContentType(contentType.getValue());

            bean.setContentLength(response.getEntity().getContentLength());
            bean.setEntity(response.getEntity().getContent());
        }

        return bean;
    }

    public static HttpUriRequest initHttpRequest(HttpRequestBean requestBean) {
        try {
            final HttpUriRequest request;
            switch (requestBean.getMethod()) {
                case HEAD:
                    request = new HttpHead(requestBean.getUri());
                    break;

                case GET:
                    request = new HttpGet(requestBean.getUri());
                    break;

                case POST:
                    request = new HttpPost(requestBean.getUri());
                    break;

                case PUT:
                    request = new HttpPut(requestBean.getUri());
                    break;

                case PATCH:
                    request = new HttpPatch(requestBean.getUri());
                    break;

                case DELETE:
                    request = new HttpDelete(requestBean.getUri());
                    break;

                default:
                    return die("Invalid request method: " + requestBean.getMethod());
            }

            if (requestBean.hasData() && request instanceof HttpEntityEnclosingRequestBase) {
                setData(requestBean.getEntity(), (HttpEntityEnclosingRequestBase) request);
            }

            return request;

        } catch (UnsupportedEncodingException e) {
            return die("initHttpRequest: " + e, e);
        }
    }

    private static void setData(Object data, HttpEntityEnclosingRequestBase request) throws UnsupportedEncodingException {
        if (data == null) return;
        if (data instanceof String) {
            request.setEntity(new StringEntity((String) data));
        } else if (data instanceof InputStream) {
            request.setEntity(new InputStreamEntity((InputStream) data));
        } else {
            throw new IllegalArgumentException("Unsupported request entity type: "+data.getClass().getName());
        }
    }

    public static String getContentType(HttpResponse response) {
        final Header contentTypeHeader = response.getFirstHeader(CONTENT_TYPE);
        return (contentTypeHeader == null) ? null : contentTypeHeader.getValue();
    }

    public static boolean isOk(String url) { return isOk(url, URIUtil.getHost(url)); }

    public static boolean isOk(String url, String host) {
        final CommandLine command = new CommandLine("curl")
                .addArgument("--insecure") // since we are requested via the IP address, the cert will not match
                .addArgument("--header").addArgument("Host: " + host) // pass FQDN via Host header
                .addArgument("--silent")
                .addArgument("--location")                              // follow redirects
                .addArgument("--write-out").addArgument("%{http_code}") // just print status code
                .addArgument("--output").addArgument("/dev/null")       // and ignore data
                .addArgument(url);
        try {
            final CommandResult result = CommandShell.exec(command);
            final String statusCode = result.getStdout();
            return result.isZeroExitStatus() && statusCode != null && statusCode.trim().startsWith("2");

        } catch (IOException e) {
            log.warn("isOk: Error fetching " + url + " with Host header=" + host + ": " + e);
            return false;
        }
    }

    public static boolean isOk(String url, String host, int maxTries, long sleepUnit) {
        long sleep = sleepUnit;
        for (int i = 0; i < maxTries; i++) {
            if (i > 0) {
                Sleep.sleep(sleep);
                sleep *= 2;
            }
            if (isOk(url, host)) return true;
        }
        return false;
    }

    public static HttpMeta getHeadMetadata(HttpRequestBean request) throws IOException {
        final HttpResponseBean headResponse = HttpUtil.getResponse(new HttpRequestBean(request).setMethod(HEAD));
        if (!headResponse.isOk()) return die("HTTP HEAD response was not 200: "+headResponse);

        final HttpMeta meta = new HttpMeta(request.getUri());

        final String lastModString = headResponse.getFirstHeaderValue(LAST_MODIFIED);
        if (lastModString != null) meta.setLastModified(DATE_FORMAT_LAST_MODIFIED.parseMillis(lastModString));

        final String etag = headResponse.getFirstHeaderValue(ETAG);
        if (etag != null) meta.setEtag(etag);

        return meta;
    }

    public static final byte[] CHUNK_SEP = "\r\n".getBytes();
    public static final int CHUNK_EXTRA_BYTES = 2 * CHUNK_SEP.length;
    public static final byte[] CHUNK_END = "0\r\n".getBytes();

    public static byte[] makeHttpChunk(byte[] buffer, int bytesRead) {
        final byte[] httpChunkLengthBytes = Integer.toHexString(bytesRead).getBytes();
        final byte[] httpChunk = new byte[bytesRead + httpChunkLengthBytes.length + CHUNK_EXTRA_BYTES];
        System.arraycopy(httpChunkLengthBytes, 0, httpChunk, 0, httpChunkLengthBytes.length);
        System.arraycopy(buffer, 0, httpChunk, httpChunkLengthBytes.length, bytesRead);
        System.arraycopy(CHUNK_SEP, 0, httpChunk, httpChunkLengthBytes.length+bytesRead, CHUNK_SEP.length);
        return httpChunk;
    }

    // adapted from https://github.com/stuartpb/user-agent-is-browser
    public static boolean isBrowser (String ua) {
        final boolean browser = !empty(ua) && !ua.equals("NONE") && !ua.equals("UNKNOWN") && (
                ua.startsWith("Mozilla/")
                // Older versions of Opera
                || ua.startsWith("Opera/")
                // Down the rabbit hole...
                || ua.startsWith("Lynx/")
                || ua.startsWith("Links ")
                || ua.startsWith("Elinks ") || ua.startsWith("ELinks ")
                || ua.startsWith("ELinks/")
                || ua.startsWith("Midori/")
                || ua.startsWith("w3m/")
                || ua.startsWith("Webkit/")
                || ua.startsWith("Vimprobable/")
                || ua.startsWith("Dooble/")
                || ua.startsWith("Dillo/")
                || ua.startsWith("Surf/")
                || ua.startsWith("NetSurf/")
                || ua.startsWith("Galaxy/")
                || ua.startsWith("Cyberdog/")
                || ua.startsWith("iCab/")
                || ua.startsWith("IBrowse/")
                || ua.startsWith("IBM WebExplorer /")
                || ua.startsWith("AmigaVoyager/")
                || ua.startsWith("HotJava/")
                || ua.startsWith("retawq/")
                || ua.startsWith("uzbl ") || ua.startsWith("Uzbl ")
                || ua.startsWith("NCSA Mosaic/") || ua.startsWith("NCSA_Mosaic/")
                // And, finally, we test to see if they"re using *the first browser ever*.
                || ua.equals("WorldWideweb (NEXT)")
        );
        if (log.isDebugEnabled()) log.debug("isBrowser("+ua+") returning "+browser);
        return browser;
    }

    public static String chaseRedirects(String url) { return chaseRedirects(url, 5); }

    public static String chaseRedirects(String url, int maxDepth) {
        log.info("chaseRedirects("+url+") starting...");
        // strip tracking params
        url = cleanParams(url);
        String lastHost;
        try {
            lastHost = URIUtil.getScheme(url) + "://" + URIUtil.getHost(url);
            HttpRequestBean requestBean = curlHead(url);
            final HttpClientBuilder clientBuilder = requestBean.initClientBuilder(HttpClients.custom().disableRedirectHandling());
            @Cleanup final CloseableHttpClient client = clientBuilder.build();

            HttpResponseBean responseBean = HttpUtil.getResponse(requestBean, client);
            if (log.isDebugEnabled()) log.debug("follow("+url+"): HEAD "+url+" returned "+json(responseBean, COMPACT_MAPPER));
            if (responseBean.isOk()) {
                // check for Link headers
                final Collection<String> links = responseBean.getHeaderValues("Link");
                if (!empty(links)) {
                    // find the longest link that has rel=shortlink
                    String longestLink = null;
                    for (String link : links) {
                        if (!link.contains("rel=shortlink")) continue;
                        if (link.indexOf("<") == 0) {
                            final int close = link.indexOf(">");
                            if (close == -1) continue;
                            final String linkUrl = link.substring(1, close);
                            if (longestLink == null || linkUrl.length() > longestLink.length()) {
                                longestLink = linkUrl;
                            }
                        }
                    }
                    if (longestLink != null) return cleanParams(longestLink.startsWith("/") ? lastHost + longestLink : longestLink);
                }
                return url;
            }

            // standard redirect chasing...
            int depth = 0;
            String nextUrl;
            while (depth < maxDepth && responseBean.is3xx() && responseBean.hasHeader(HttpHeaders.LOCATION)) {
                depth++;
                nextUrl = cleanParams(responseBean.getFirstHeaderValue(HttpHeaders.LOCATION));
                if (log.isDebugEnabled()) log.debug("follow("+url+"): found nextUrl="+nextUrl);
                if (nextUrl == null) break;
                if (nextUrl.startsWith("/")) {
                    nextUrl = lastHost + nextUrl;
                } else {
                    lastHost = URIUtil.getScheme(nextUrl) + "://" + URIUtil.getHost(nextUrl);
                }
                url = nextUrl;
                requestBean = curlHead(url);
                responseBean = HttpUtil.getResponse(requestBean, client);
            }
        } catch (Exception e) {
            if (log.isErrorEnabled()) log.error("follow("+url+"): error following: "+shortError(e));
        }
        return url;
    }

    public static String cleanParams(String url) {
        final int qPos = url.indexOf("?");
        if (qPos != -1) {
            final Map<String, String> params = URIUtil.queryParams(url);
            if (!params.isEmpty()) {
                final StringBuilder b = new StringBuilder();
                for (Map.Entry<String, String> param : params.entrySet()) {
                    if (!isBlockedParam(param.getKey())) {
                        if (b.length() > 0) b.append("&");
                        b.append(param.getKey()).append("=").append(urlEncode(param.getValue()));
                    }
                }
                url = url.substring(0, qPos+1) + b.toString();
            }
        }
        return url;
    }

    public static final String[] BLOCKED_PARAMS = {"dfaid", "cmp", "cid", "dclid"};

    private static boolean isBlockedParam(String key) {
        return key.startsWith("utm_") || ArrayUtils.contains(BLOCKED_PARAMS, key);
    }

    public static HttpRequestBean curlHead(String url) {
        return new HttpRequestBean(HttpMethods.HEAD, url)
                .setHeader(ACCEPT, "*/*")
                .setHeader(USER_AGENT, USER_AGENT_CURL);
    }

}
