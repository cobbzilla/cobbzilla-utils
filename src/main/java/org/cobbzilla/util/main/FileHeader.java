package org.cobbzilla.util.main;

import lombok.Getter;
import lombok.Setter;

import java.util.regex.Pattern;

public class FileHeader {

    @Getter @Setter private String ext;
    @Getter @Setter private String header;
    @Getter @Setter private String regex;
    @Getter(lazy=true) private final Pattern pattern = Pattern.compile(getRegex()+"\n", Pattern.MULTILINE | Pattern.DOTALL);

    @Getter @Setter private String prefix;
    public boolean hasPrefix () { return prefix != null; }
    @Getter(lazy=true) private final Pattern prefixPattern = Pattern.compile(getPrefix()+"\n", Pattern.MULTILINE | Pattern.DOTALL);

}
