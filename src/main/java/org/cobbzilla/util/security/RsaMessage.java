package org.cobbzilla.util.security;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import static org.cobbzilla.util.reflect.ReflectionUtil.copy;

@NoArgsConstructor @Accessors(chain=true)
public class RsaMessage {

    public RsaMessage (RsaMessage other) { copy(this, other); }

    @Getter @Setter private String publicKey;
    @Getter @Setter private String symKey;
    @Getter @Setter private String data;
    @Getter @Setter private String signature;

}
