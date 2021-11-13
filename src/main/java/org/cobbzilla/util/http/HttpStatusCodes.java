package org.cobbzilla.util.http;

public class HttpStatusCodes {

    public static final int UNPROCESSABLE_ENTITY = 422;
    public static final int INVALID = UNPROCESSABLE_ENTITY;
    public static final int OK = 200;
    public static final int CREATED = 201;
    public static final int ACCEPTED = 202;
    public static final int NON_AUTHORITATIVE_INFO = 203;
    public static final int NO_CONTENT = 204;
    public static final int FOUND = 302;
    public static final int UNAUTHORIZED = 401;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int PRECONDITION_FAILED = 412;
    public static final int UNSUPPORTED_MEDIA_TYPE = 415;
    public static final int TOO_MANY_REQUESTS = 429;
    public static final int SERVER_ERROR = 500;
    public static final int SERVER_UNAVAILABLE = 503;
    public static final int GATEWAY_TIMEOUT = 504;

    public static final String SC_UNPROCESSABLE_ENTITY = "422";
    public static final String SC_INVALID = SC_UNPROCESSABLE_ENTITY;
    public static final String SC_OK = "200";
    public static final String SC_CREATED = "201";
    public static final String SC_ACCEPTED = "202";
    public static final String SC_NON_AUTHORITATIVE_INFO = "203";
    public static final String SC_NO_CONTENT = "204";
    public static final String SC_FOUND = "302";
    public static final String SC_UNAUTHORIZED = "401";
    public static final String SC_FORBIDDEN = "403";
    public static final String SC_NOT_FOUND = "404";
    public static final String SC_PRECONDITION_FAILED = "412";
    public static final String SC_UNSUPPORTED_MEDIA_TYPE = "415";
    public static final String SC_TOO_MANY_REQUESTS = "429";
    public static final String SC_SERVER_ERROR = "500";
    public static final String SC_SERVER_UNAVAILABLE = "503";
    public static final String SC_GATEWAY_TIMEOUT = "504";

    public static boolean isOk(int status) { return is2xx(status); }
    public static boolean is1xx(int status) { return (status / 100) == 1; }
    public static boolean is2xx(int status) { return (status / 100) == 2; }
    public static boolean is3xx(int status) { return (status / 100) == 3; }
    public static boolean is4xx(int status) { return (status / 100) == 4; }
    public static boolean is5xx(int status) { return (status / 100) == 5; }

}
