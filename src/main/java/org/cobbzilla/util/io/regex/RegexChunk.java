package org.cobbzilla.util.io.regex;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor @Accessors(chain=true) @ToString
public class RegexChunk {

    @Getter @Setter private RegexChunkType type;
    @Getter @Setter private String data;
    @Getter @Setter private boolean partial = false;

    @Getter private Map<String, String> properties;

    public String getProperty (String prop) { return properties == null ? null : properties.get(prop); }

    public void setProperty (String prop, String val) {
        if (properties == null) properties = new HashMap<>();
        properties.put(prop, val);
    }

}
