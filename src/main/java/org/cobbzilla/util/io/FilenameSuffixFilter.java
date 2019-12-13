package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FilenameFilter;

@AllArgsConstructor
public class FilenameSuffixFilter implements FilenameFilter {

    @Getter @Setter private String suffix;

    @Override public boolean accept(File dir, String name) {
        return name.endsWith(suffix);
    }

}
