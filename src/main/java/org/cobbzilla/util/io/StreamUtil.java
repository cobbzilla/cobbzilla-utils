package org.cobbzilla.util.io;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.cobbzilla.util.string.StringUtil;

import java.io.*;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.io.FileUtil.*;
import static org.cobbzilla.util.reflect.ReflectionUtil.closeQuietly;

@Slf4j
public class StreamUtil {

    public static final String SUFFIX = ".tmp";
    public static final String PREFIX = "stream2file";
    public static final String CLASSPATH_PROTOCOL = "classpath://";

    public static File stream2temp (String path) { return stream2file(loadResourceAsStream(path), true, path); }
    public static File stream2temp (InputStream in) { return stream2file(in, true); }

    public static File stream2file (InputStream in) { return stream2file(in, false); }
    public static File stream2file (InputStream in, boolean deleteOnExit) { return stream2file(in, deleteOnExit, SUFFIX); }

    public static File stream2file (InputStream in, boolean deleteOnExit, String pathOrSuffix) {
        try {
            return stream2file(in, mktemp(deleteOnExit, pathOrSuffix));
        } catch (IOException e) {
            return die("stream2file: "+e, e);
        }
    }

    public static File mktemp(boolean deleteOnExit, String pathOrSuffix) throws IOException {
        final String basename = empty(pathOrSuffix) ? "" : basename(pathOrSuffix);
        final File file = File.createTempFile(
                !basename.contains(".") || basename.length() < 7 ? basename.replace('.', '_')+"_"+PREFIX : basename.split("\\.")[0],
                empty(pathOrSuffix) ? SUFFIX : extensionOrName(pathOrSuffix),
                getDefaultTempDir());
        if (deleteOnExit) file.deleteOnExit();
        return file;
    }

    public static File stream2file(InputStream in, File file) throws IOException {
        try (OutputStream out = new FileOutputStream(file)) {
            IOUtils.copy(in, out);
        }
        return file;
    }

    public static ByteArrayInputStream toStream(String s) throws UnsupportedEncodingException {
        return new ByteArrayInputStream(s.getBytes(StringUtil.UTF8));
    }

    public static String toString(InputStream in) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        return out.toString();
    }

    public static String toStringOrDie(InputStream in) {
        try { return toString(in); } catch (Exception e) { return die("toStringOrDie: "+e, e); }
    }

    public static InputStream loadResourceAsStream(String path) {
        return loadResourceAsStream(path, StreamUtil.class);
    }

    public static InputStream loadResourceAsStream(String path, Class clazz) {
        InputStream in = clazz.getClassLoader().getResourceAsStream(path);
        if (in == null) throw new IllegalArgumentException("Resource not found: " + path);
        return in;
    }

    public static File loadResourceAsFile (String path) throws IOException {
        final File tmp = File.createTempFile("resource", extensionOrName(path), getDefaultTempDir());
        return loadResourceAsFile(path, StreamUtil.class, tmp);
    }

    public static File loadResourceAsFile (String path, Class clazz) throws IOException {
        final File tmp = File.createTempFile("resource", ".tmp", getDefaultTempDir());
        return loadResourceAsFile(path, clazz, tmp);
    }

    public static File loadResourceAsFile(String path, File file) throws IOException {
        return loadResourceAsFile(path, StreamUtil.class, file);
    }

    public static File loadResourceAsFile(String path, Class clazz, File file) throws IOException {
        if (file.isDirectory()) file = new File(file, new File(path).getName());
        @Cleanup final FileOutputStream out = new FileOutputStream(file);
        IOUtils.copy(loadResourceAsStream(path, clazz), out);
        return file;
    }

    public static String stream2string(String path) { return loadResourceAsStringOrDie(path); }

    public static String stream2string(String path, String defaultValue) {
        try {
            return loadResourceAsStringOrDie(path);
        } catch (Exception e) {
            log.info("stream2string: path not found ("+path+": "+e+"), returning defaultValue");
            return defaultValue;
        }
    }

    public static byte[] stream2bytes(String path) { return loadResourceAsBytesOrDie(path); }

    public static byte[] stream2bytes(String path, byte[] defaultValue) {
        try {
            return loadResourceAsBytesOrDie(path);
        } catch (Exception e) {
            log.info("stream2bytes: path not found ("+path+": "+e+"), returning defaultValue");
            return defaultValue;
        }
    }

    public static byte[] loadResourceAsBytesOrDie(String path) {
        try {
            @Cleanup final InputStream in = loadResourceAsStream(path);
            if (in == null) return die("stream2bytes: not found: "+path);
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            IOUtils.copyLarge(in, out);
            return out.toByteArray();
        } catch (Exception e) {
            return die("loadResourceAsBytesOrDie: error copying bytes: "+e, e);
        }
    }

    public static String loadResourceAsStringOrDie(String path) {
        try {
            return loadResourceAsString(path, StreamUtil.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("cannot load resource: "+path+": "+e, e);
        }
    }

    public static String loadResourceAsString(String path) throws IOException {
        return loadResourceAsString(path, StreamUtil.class);
    }

    public static String loadResourceAsString(String path, Class clazz) throws IOException {
        @Cleanup final InputStream in = loadResourceAsStream(path, clazz);
        @Cleanup final ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(in, out);
        return out.toString(StringUtil.UTF8);
    }

    public static Reader loadResourceAsReader(String resourcePath, Class clazz) throws IOException {
        return new InputStreamReader(loadResourceAsStream(resourcePath, clazz));
    }

    public static final int DEFAULT_BUFFER_SIZE = 32 * 1024;

    public static long copyLarge(InputStream input, OutputStream output) throws IOException {
        return copyLarge(input, output, DEFAULT_BUFFER_SIZE);
    }

    public static long copyLarge(InputStream input, OutputStream output, int bufferSize) throws IOException {
        byte[] buffer = new byte[bufferSize];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    /**
     * Copy the first n bytes from input to output
     * @return the number of bytes actually copied (might be less than n if EOF was reached)
     */
    public static long copyNbytes(InputStream input, OutputStream output, long n) throws IOException {
        byte[] buffer = new byte[(n > DEFAULT_BUFFER_SIZE) ? DEFAULT_BUFFER_SIZE : (int) n];
        long copied = 0;
        int read = 0;
        while (copied < n && -1 != (read = input.read(buffer, 0, (int) (n - copied > buffer.length ? buffer.length : n - copied)))) {
            output.write(buffer, 0, read);
            copied += read;
        }
        return copied;
    }

    // incredibly inefficient. do not use frequently. meant for command-line tools that call it no more than a few times
    public static String readLineFromStdin() {
        final String line;
        final BufferedReader r = stdin();
        try { line = r.readLine(); } catch (Exception e) {
            return die("Error reading from stdin: " + e);
        }
        return line == null ? null : line.trim();
    }

    public static String readLineFromStdin(String prompt) {
        System.out.print(prompt);
        return readLineFromStdin();
    }

    public static String fromClasspathOrFilesystem(String path) {
        try {
            return stream2string(path);
        } catch (Exception e) {
            try {
                return FileUtil.toStringOrDie(path);
            } catch (Exception e2) {
                return die("path not found: "+path);
            }
        }
    }

    public static String fromClasspathOrString(String path) {
        final boolean isClasspath = path.startsWith(CLASSPATH_PROTOCOL);
        if (isClasspath) {
            path = path.substring(CLASSPATH_PROTOCOL.length());
            return stream2string(path);
        }
        return path;
    }

    // adapted from https://stackoverflow.com/a/2993908/1251543
    public static TempDir copyClasspathDirectory(String path) {
        final TempDir tempDir = new TempDir();
        final URL resourceUrl = StreamUtil.class.getClassLoader().getResource(path);
        if (resourceUrl == null) return die("copyClasspathDirectory: root resource not found");

        final URLConnection urlConnection;
        final JarFile jarFile;

        try {
            urlConnection = resourceUrl.openConnection();
            if (urlConnection instanceof JarURLConnection) {
                final JarURLConnection jarConnection = (JarURLConnection) urlConnection;
                jarFile = jarConnection.getJarFile();
                final Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    final JarEntry entry = entries.nextElement();
                    if (entry.getName().startsWith(jarConnection.getEntryName())) {
                        String fileName = StringUtils.removeStart(entry.getName(), jarConnection.getEntryName());
                        if (!entry.isDirectory()) {
                            InputStream entryInputStream = null;
                            try {
                                entryInputStream = jarFile.getInputStream(entry);
                                stream2file(entryInputStream, new File(tempDir, fileName));
                            } finally {
                                closeQuietly(entryInputStream);
                            }
                        } else {
                            mkdirOrDie(new File(tempDir, fileName));
                        }
                    }
                }
            } else {
                FileUtils.copyDirectory(new File(resourceUrl.getPath()), tempDir);
            }
        } catch (Exception e) {
            return die("copyClasspathDirectory: error copying resources: "+shortError(e), e);
        }

        return tempDir;
    }

}
