package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Pattern;

@AllArgsConstructor
public class FilenameRegexFilter implements FilenameFilter {

    private final Pattern pattern;

    public FilenameRegexFilter (String p) { pattern = Pattern.compile(p); }

    @Override public boolean accept(File dir, String name) { return pattern.matcher(name).matches(); }

}
