package org.cobbzilla.util.io;

import java.io.File;
import java.io.FileFilter;

public class RegularFileFilter implements FileFilter {

    public static final RegularFileFilter instance = new RegularFileFilter();

    @Override public boolean accept(File pathname) { return pathname.isFile(); }

}
