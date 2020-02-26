package org.cobbzilla.util.main;

import lombok.Getter;
import lombok.Setter;

import java.util.regex.Pattern;

public class FileHeader {

    @Getter @Setter private String ext;
    @Getter @Setter private String header;
    @Getter @Setter private String regex;

    @Getter(lazy=true) private final Pattern pattern = Pattern.compile(getRegex(), Pattern.MULTILINE);

}
