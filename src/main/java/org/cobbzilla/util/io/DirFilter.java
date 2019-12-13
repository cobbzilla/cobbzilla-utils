package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

import static org.cobbzilla.util.daemon.ZillaRuntime.empty;

@NoArgsConstructor @AllArgsConstructor
public class DirFilter implements FileFilter {

    public static final DirFilter instance = new DirFilter();

    @Getter @Setter private String regex;

    @Getter(lazy=true) private final Pattern pattern = initPattern();
    private Pattern initPattern() { return Pattern.compile(regex); }

    @Override public boolean accept(File pathname) {
        return pathname.isDirectory() && (empty(regex) || getPattern().matcher(pathname.getName()).matches());
    }

}
