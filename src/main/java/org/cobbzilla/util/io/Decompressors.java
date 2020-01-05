package org.cobbzilla.util.io;

import lombok.Cleanup;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.poi.openxml4j.util.ZipSecureFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.io.Tarball.isTarball;

// zip-related code adapted from: https://stackoverflow.com/a/10634536/1251543
public class Decompressors {

    public static TempDir unroll (File infile) throws Exception {
        if (isTarball(infile)) {
            return Tarball.unroll(infile);
        } else if (isZipFile(infile.getName())) {
            final TempDir tempDir = new TempDir();
            extract(infile, tempDir);
            return tempDir;
        } else {
            return die("unroll: unsupported file: "+infile);
        }
    }

    public static boolean isZipFile(String name) {
        return name.toLowerCase().endsWith(".zip");
    }

    public static boolean isDecompressible(File file) { return isDecompressible(file.getName()); }

    public static boolean isDecompressible(String name) {
        return isTarball(name) || isZipFile(name);
    }

    private static void extractFile(InputStream in, File outdir, String name) throws IOException {
        @Cleanup final FileOutputStream out = new FileOutputStream(new File(outdir, name));
        StreamUtil.copyLarge(in, out);
    }

    private static void mkdirs(File outdir, String path) {
        final File d = new File(outdir, path);
        if (!d.exists() && !d.mkdirs()) die("mkdirs("+abs(outdir)+", "+path+"): error creating "+abs(d));
    }

    private static String dirpart(String name) {
        final int s = name.lastIndexOf( File.separatorChar );
        return s == -1 ? null : name.substring( 0, s );
    }

    /***
     * Extract zipfile to outdir with complete directory structure
     * Uses ZipSecureFile to avoid zip-bombs
     * @param zipfile Input .zip file
     * @param outdir Output directory
     */
    public static void extract(File zipfile, File outdir) throws IOException {
        @Cleanup final ZipSecureFile zip = new ZipSecureFile(zipfile);
        final Enumeration<ZipArchiveEntry> entries = zip.getEntriesInPhysicalOrder();
        while (entries.hasMoreElements()) {
            final ZipArchiveEntry entry = entries.nextElement();
            final String name = entry.getName();
            if (entry.isDirectory()) {
                mkdirs(outdir,name);
                continue;
            }
            /* this part is necessary because file entry can come before
             * directory entry where is file located
             * i.e.:
             *   /foo/foo.txt
             *   /foo/
             */
            final String dir = dirpart(name);
            if (dir != null) mkdirs(outdir,dir);

            extractFile(zip.getInputStream(entry), outdir, name);
        }
    }
}
