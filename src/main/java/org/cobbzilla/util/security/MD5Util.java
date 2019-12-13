package org.cobbzilla.util.security;

import org.cobbzilla.util.string.StringUtil;
import org.slf4j.Logger;

import java.io.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class MD5Util {

    private MD5Util () {}

    public static byte[] getMD5 ( byte[] bytes ) {
        return getMD5(bytes, 0, bytes.length);
    }
    public static byte[] getMD5 ( byte[] bytes, int start, int len ) {
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update( bytes, start, len );
            return md5.digest();
        } catch (NoSuchAlgorithmException e) {
            return die("Error calculating MD5: " + e);
        }
    }

    public static String md5hex (Logger log, File file) throws IOException {
        int BUFSIZ = 4096;
        try (FileInputStream fin = new FileInputStream(file)) {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BufferedInputStream in = new BufferedInputStream(fin);
            byte[] buf = new byte[BUFSIZ];
            int bytesRead = in.read(buf);
            while (bytesRead != -1) {
                md5.update(buf, 0, bytesRead);
                bytesRead = in.read(buf);
            }
            return StringUtil.tohex(md5.digest());

        } catch (NoSuchAlgorithmException e) {
            return die("Error calculating MD5: " + e);
        }

    }

    public static String md5hex ( String s ) {
        byte[] bytes = getMD5(s.getBytes());
        return StringUtil.tohex(bytes);
    }

    public static String md5hex (MessageDigest md) {
        return StringUtil.tohex(md.digest());
    }

    public static String md5hex (byte[] data) {
        return md5hex(data, 0, data.length);
    }
    public static String md5hex (byte[] data, int start, int len) {
        byte[] bytes = getMD5(data, start, len);
        return StringUtil.tohex(bytes);
    }

    public static final String[] HEX_DIGITS = {"0", "1", "2", "3",
            "4", "5", "6", "7",
            "8", "9", "a", "b",
            "c", "d", "e", "f"};


    public static MD5InputStream getMD5InputStream (InputStream in) {
        try {
            return new MD5InputStream(in);
        } catch (NoSuchAlgorithmException e) {
            return die("Bad algorithm: " + e);
        }
    }

    public static final class MD5InputStream extends DigestInputStream {

        public MD5InputStream(InputStream stream) throws NoSuchAlgorithmException {
            super(stream, MessageDigest.getInstance("MD5"));
        }

        public String md5hex () {
            return MD5Util.md5hex(getMessageDigest());
        }
    }
}
