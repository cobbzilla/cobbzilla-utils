package org.cobbzilla.util.http;

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.http.auth.AuthScheme;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.auth.DigestScheme;
import org.apache.http.impl.auth.KerberosScheme;

import static org.cobbzilla.util.reflect.ReflectionUtil.instantiate;

public enum HttpAuthType {

    basic (BasicScheme.class),
    digest (DigestScheme.class),
    kerberos (KerberosScheme.class);

    private final Class<? extends AuthScheme> scheme;
    HttpAuthType(Class<? extends AuthScheme> scheme) { this.scheme = scheme; }

    public AuthScheme newScheme () { return instantiate(scheme); }

    @JsonCreator public static HttpAuthType create(String value) { return valueOf(value.toLowerCase()); }

}
