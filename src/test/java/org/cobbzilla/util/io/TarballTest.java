package org.cobbzilla.util.io;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.cobbzilla.util.security.ShaUtil;
import org.cobbzilla.util.string.StringUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.cobbzilla.util.io.FileUtil.getDefaultTempDir;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TarballTest {

    private File tempDir;

    @Before public void createTempDir () throws Exception { tempDir = Files.createTempDir(); }

    @After public void deleteTempDir () throws Exception { FileUtils.deleteDirectory(tempDir); }

    @Test public void testUnrollAndReroll () throws Exception {

        // copy tarball from resources to temp file
        final File tarball = StreamUtil.stream2temp(StreamUtil.loadResourceAsStream(StringUtil.packagePath(getClass()) + "/test.tar.gz"));

        // unroll the tarball
        Tarball.unroll(tarball, tempDir);

        // list files in tempDir, we expect file1 and subdir
        validateUnrolledTarball(tempDir);

        final File newTar = File.createTempFile("temp", ".tar", getDefaultTempDir());
        Tarball.roll(newTar, this.tempDir);

        // reset tempdir
        deleteTempDir(); createTempDir();
        Tarball.unroll(tarball, tempDir);

        // re-validate, should still pass
        validateUnrolledTarball(tempDir);
    }

    protected void validateUnrolledTarball(File unrolledDir) throws IOException {
        final File[] files = unrolledDir.listFiles();
        assertEquals(2, files.length);

        int fileIndex, subdirIndex;
        if (files[0].getName().equals("file1.txt")) {
            fileIndex = 0;
            subdirIndex = 1;
        } else {
            subdirIndex = 0;
            fileIndex = 1;
        }

        String shasum;

        assertEquals("file1.txt", files[fileIndex].getName());
        assertEquals(4160, files[fileIndex].length());
        shasum = StringUtil.tohex(ShaUtil.sha256(FileUtil.toString(files[fileIndex])));
        assertEquals("62b74f00961184e2b448adfcf38ee6186d94b15ea6a364917d72645d8705a30c", shasum);

        assertEquals("subdir", files[subdirIndex].getName());
        assertTrue("subdir", files[subdirIndex].isDirectory());

        final File[] subdirFiles = files[subdirIndex].listFiles();
        assertEquals(1, subdirFiles.length);
        assertEquals("subfile.txt", subdirFiles[0].getName());
        assertEquals(65072, subdirFiles[0].length());
        shasum = StringUtil.tohex(ShaUtil.sha256(FileUtil.toString(subdirFiles[0])));
        assertEquals("4ded9b8ff2cb3b94ff0bb15b8e17aff795c7f64bd088d5d70acb38ad286e2d12", shasum);
    }

}
