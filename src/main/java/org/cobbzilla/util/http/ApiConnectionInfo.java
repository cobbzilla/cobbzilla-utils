package org.cobbzilla.util.http;

import lombok.*;

import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@NoArgsConstructor @AllArgsConstructor @ToString(of={"baseUri", "user"})
@EqualsAndHashCode(of={"baseUri", "user", "password"})
public class ApiConnectionInfo {

    @Getter @Setter private String baseUri;
    public boolean hasBaseUri () { return baseUri != null; }

    @Getter @Setter private String user;
    public boolean hasUser () { return user != null; }

    @Getter @Setter private String password;

    public ApiConnectionInfo (String baseUri) { this.baseUri = baseUri; }

    public ApiConnectionInfo (ApiConnectionInfo other) { copy(this, other); }

    // alias for when this is used in json with snake_case naming conventions
    public String getBase_uri () { return getBaseUri(); }
    public void setBase_uri (String uri) { setBaseUri(uri); }

}
