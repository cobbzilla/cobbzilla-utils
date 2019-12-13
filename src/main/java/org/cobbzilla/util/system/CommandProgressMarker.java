package org.cobbzilla.util.system;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.regex.Pattern;

@NoArgsConstructor @AllArgsConstructor @Accessors(chain=true)
public class CommandProgressMarker {

    @Getter @Setter private int percent;
    @Getter @Setter private Pattern pattern;
    @Getter @Setter private String line;

}
