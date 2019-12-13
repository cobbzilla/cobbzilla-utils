package org.cobbzilla.util.javascript;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class JsEngineConfig {

    @Getter @Setter private int minEngines;
    @Getter @Setter private int maxEngines;
    @Getter @Setter private String defaultScript;

}
