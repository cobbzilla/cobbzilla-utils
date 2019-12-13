package org.cobbzilla.util.system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor @AllArgsConstructor
public class ConnectionInfo {

    @Getter @Setter private String host;
    @Getter @Setter private Integer port;
    public boolean hasPort () { return port != null; }

    @Getter @Setter private String username;
    @Getter @Setter private String password;

    // convenience methods
    public String getUser () { return getUsername(); }
    public void setUser (String user) { setUsername(user); }

    public ConnectionInfo (String host, Integer port) {
        this(host, port, null, null);
    }
}
