package org.cobbzilla.util.io;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.io.FileFilter;

@AllArgsConstructor
public class FileSuffixFilter implements FileFilter {

    @Getter @Setter private String suffix;

    @Override public boolean accept(File pathname) { return pathname.getName().endsWith(suffix); }

}
