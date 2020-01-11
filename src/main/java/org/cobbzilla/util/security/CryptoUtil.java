package org.cobbzilla.util.security;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cobbzilla.util.string.Base64;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Key;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.retry;
import static org.cobbzilla.util.string.StringUtil.UTF8cs;

@Slf4j
public class CryptoUtil {

    public static final String CONFIG_BLOCK_CIPHER = "AES/CBC/PKCS5Padding";
    public static final String CONFIG_KEY_CIPHER = "AES";

    public static final String RSA_PREFIX = "-----BEGIN RSA PRIVATE KEY-----";
    public static final String RSA_SUFFIX = "-----END RSA PRIVATE KEY-----";

    public static byte[] toBytes(InputStream data) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(data, out);
        return out.toByteArray();
    }

    public static String extractRsa (String data) {
        int startPos = data.indexOf(RSA_PREFIX);
        if (startPos == -1) return null;
        int endPos = data.indexOf(RSA_SUFFIX);
        if (endPos == -1) return null;
        return data.substring(startPos, endPos + RSA_SUFFIX.length());
    }

    public static byte[] encrypt (InputStream data, String passphrase) throws Exception {
        return encrypt(toBytes(data), passphrase);
    }

    public static byte[] encrypt (byte[] data, String passphrase) throws Exception {
        final Cipher cipher = Cipher.getInstance(CONFIG_BLOCK_CIPHER);
        final Key keySpec = new SecretKeySpec(sha256(passphrase), CONFIG_KEY_CIPHER);
        final IvParameterSpec initVector = new IvParameterSpec(new byte[cipher.getBlockSize()]);
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, initVector);
        return cipher.doFinal(data);
    }

    public static byte[] sha256(String passphrase) throws Exception {
        return ShaUtil.sha256(passphrase);
    }

    public static byte[] decrypt (InputStream data, String passphrase) throws Exception {
        return decrypt(toBytes(data), passphrase);
    }

    public static byte[] decrypt (byte[] data, String passphrase) throws Exception {
        final Cipher cipher = Cipher.getInstance(CONFIG_BLOCK_CIPHER);
        final Key keySpec = new SecretKeySpec(sha256(passphrase), CONFIG_KEY_CIPHER);
        final IvParameterSpec initVector = new IvParameterSpec(new byte[cipher.getBlockSize()]);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, initVector);
        return cipher.doFinal(data);
    }

    public static InputStream decryptStream(InputStream in, String passphrase) throws Exception {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final Key keySpec = new SecretKeySpec(sha256(passphrase), CONFIG_KEY_CIPHER);
        final IvParameterSpec initVector = new IvParameterSpec(new byte[cipher.getBlockSize()]);
        cipher.init(Cipher.DECRYPT_MODE, keySpec, initVector);
        return new CipherInputStream(in, cipher);
    }

    public static byte[] encryptOrDie(byte[] data, String passphrase) {
        try { return encrypt(data, passphrase); } catch (Exception e) {
            return die("Error encrypting: "+e, e);
        }
    }

    private static final String PADDING_SUFFIX = "__PADDING__";

    public static String pad(String data) throws Exception { return data + PADDING_SUFFIX + RandomStringUtils.random(128); }

    public static String unpad(String data) {
        if (data == null) return null;
        int paddingPos = data.indexOf(PADDING_SUFFIX);
        if (paddingPos == -1) return null;
        return data.substring(0, paddingPos);
    }

    public static String string_encrypt(String data, String key) {
        try { return Base64.encodeBytes(encryptOrDie(pad(data).getBytes(UTF8cs), key)); } catch (Exception e) {
            return die("Error encrypting: "+e, e);
        }
    }

    public static String string_decrypt(String data, String key) {
        try { return unpad(new String(decrypt(Base64.decode(data), key))); } catch (Exception e) {
            return die("Error decrypting: "+e, e);
        }
    }

    public static String generatePassword(int length, int minDistinct) {
        return retry(() -> {
            final String password = randomAlphanumeric(length);
            if (password.chars().distinct().count() >= minDistinct) return password;
            return die("minDistinct not met");
        }, 10);
    }
}
