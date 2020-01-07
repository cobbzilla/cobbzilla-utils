package org.cobbzilla.util.io;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.CompressorInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.system.Command;

import java.io.*;

import static java.io.File.createTempFile;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.system.CommandShell.*;

@Slf4j
public class Tarball {

    /**
     * @param tarball the tarball to unroll. Can be .tar.gz or .tar.bz2
     * @return a File representing the temp directory where the tarball was unrolled
     */
    public static TempDir unroll (File tarball) throws Exception {
        TempDir tempDirectory = new TempDir();
        try {
            unroll(tarball, tempDirectory);
            return tempDirectory;

        } catch (Exception e) {
            FileUtils.deleteDirectory(tempDirectory);
            throw e;
        }
    }

    public static void unroll(File tarball, File dir) throws IOException, ArchiveException {

        final String path = tarball.getAbsolutePath();
        final FileInputStream fileIn = new FileInputStream(tarball);
        final CompressorInputStream zipIn;

        if (path.toLowerCase().endsWith(".gz") || path.toLowerCase().endsWith(".tgz")) {
            zipIn = new GzipCompressorInputStream(fileIn);

        } else if (path.toLowerCase().endsWith(".bz2")) {
            zipIn = new BZip2CompressorInputStream(fileIn);

        } else {
            log.warn("tarball (" + path + ") was not .tar.gz, .tgz, or .tar.bz2, assuming .tar.gz");
            zipIn = new GzipCompressorInputStream(fileIn);
        }

        @Cleanup final TarArchiveInputStream tarIn
                = (TarArchiveInputStream) new ArchiveStreamFactory()
                .createArchiveInputStream("tar", zipIn);

        TarArchiveEntry entry;
        while ((entry = tarIn.getNextTarEntry()) != null) {
            String name = entry.getName();
            if (name.startsWith("./")) name = name.substring(2);
            if (name.startsWith("/")) name = name.substring(1); // "root"-based files just go into current dir
            if (name.endsWith("/")) {
                final String subdirName = name.substring(0, name.length() - 1);
                final File subdir = new File(dir, subdirName);
                if (!subdir.exists() && !subdir.mkdirs()) {
                    die("Error creating directory: " + abs(subdir));
                }
                continue;
            }

            // when "./" gets squashed to "", we skip the entry
            if (name.trim().length() == 0) continue;

            final File file = new File(dir, name);
            try (OutputStream out = new FileOutputStream(file)) {
                if (StreamUtil.copyNbytes(tarIn, out, entry.getSize()) != entry.getSize()) {
                    die("Expected to copy "+entry.getSize()+ " bytes for "+entry.getName()+" in tarball "+ path);
                }
            }
            chmod(file, Integer.toOctalString(entry.getMode()));
        }
    }

    /**
     * Roll a gzipped tarball. The tarball will be created from within the directory to be tarred (paths will be relative to .)
     * @param dir The directory to tar
     * @return The created tarball (will be a temp file)
     */
    public static File roll (File dir) throws IOException {
        return roll(createTempFile("temp-tarball-", ".tar.gz"), dir, dir);
    }

    /**
     * Roll a gzipped tarball. The tarball will be created from within the directory to be tarred (paths will be relative to .)
     * @param tarball The path to the tarball to create
     * @param dir The directory to tar
     * @return The created tarball
     */
    public static File roll (File tarball, File dir) throws IOException {
        return roll(tarball, dir, dir);
    }

    /**
     * Roll a gzipped tarball. The tarball will be created from "cwd", which must above the directory to be tarred.
     * @param tarball The path to the tarball to create
     * @param dir The directory to tar
     * @param cwd A directory that is somewhere above dir in the filesystem hierarchy
     * @return The created tarball
     */
    public static File roll (File tarball, File dir, File cwd) throws IOException {

        if (cwd == null) cwd = dir;
        final String dirAbsPath = abs(dir);
        final String cwdAbsPath = abs(cwd);

        final String dirPath;
        if (dirAbsPath.equals(cwdAbsPath)) {
            dirPath = ".";

        } else if (dirAbsPath.startsWith(cwdAbsPath)) {
            dirPath = cwdAbsPath.substring(dirAbsPath.length());

        } else {
            return die("tarball dir is not within cwd");
        }

        final CommandLine command = new CommandLine("tar")
                .addArgument("czf")
                .addArgument(tarball.getAbsolutePath())
                .addArgument(dirPath);

        okResult(exec(new Command(command).setDir(cwd)));

        return tarball;
    }

    public static boolean isTarball(File file) { return isTarball(file.getName().toLowerCase()); }

    public static boolean isTarball(String fileName) {
        return fileName.endsWith(".tar.gz")
                || fileName.endsWith(".tar.bz2")
                || fileName.endsWith(".tgz");
    }
}
