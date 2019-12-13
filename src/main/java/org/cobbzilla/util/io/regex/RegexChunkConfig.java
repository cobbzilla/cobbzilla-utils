package org.cobbzilla.util.io.regex;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.cobbzilla.util.collection.NameAndValue;

import java.util.regex.Pattern;

public class RegexChunkConfig {

    @Getter @Setter private String chunkStartRegex;
    @Getter @Setter private String chunkEndRegex;
    public boolean hasChunkEndRegex () { return chunkEndRegex != null && chunkEndRegex.length() > 0; }

    private Pattern initPattern(String regex) { return Pattern.compile(regex); }

    @JsonIgnore @Getter(lazy=true) private final Pattern chunkStartPattern = initPattern(getChunkStartRegex());
    @JsonIgnore @Getter(lazy=true) private final Pattern chunkEndPattern = hasChunkEndRegex() ? initPattern(getChunkEndRegex()) : null;

    @Getter @Setter private NameAndValue[] chunkProperties;
    public boolean hasChunkProperties () { return chunkProperties != null && chunkProperties.length > 0; }

}
