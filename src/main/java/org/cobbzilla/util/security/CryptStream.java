package org.cobbzilla.util.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.security.spec.AlgorithmParameterSpec;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.security.ShaUtil.sha256;

@AllArgsConstructor
public class CryptStream {

    public static final int GCM_TAG_SIZE = 128;
    public static final int BUFFER_SIZE = 8192;

    private final String password;

    @Getter(lazy=true) private final SecretKeySpec secretKey = new SecretKeySpec(sha256(password), "AES");

    static { Security.addProvider(new BouncyCastleProvider()); }

    private static Cipher newCipher() {
        try {
            return Cipher.getInstance("AES/GCM/NoPadding", "BC");
        } catch (Exception e) {
            return die("newCipher: "+e);
        }
    }

    private AlgorithmParameterSpec getIv(byte[] salt) {
        if (salt.length != GCM_TAG_SIZE) return die("getIv: expected "+GCM_TAG_SIZE+" salt bytes for GCM tag");
        return new GCMParameterSpec(GCM_TAG_SIZE, salt);
    }

    protected Cipher getEncryptionCipher(byte[] salt, String aad) {
        try {
            final Cipher cipher = newCipher();
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey(), getIv(salt));
            cipher.updateAAD(aad.getBytes());
            return cipher;
        } catch (Exception e) {
            return die("getEncryptionCipher: "+e);
        }
    }

    protected Cipher getDecryptionCipher(byte[] salt, String aad) {
        try {
            final Cipher cipher = newCipher();
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), getIv(salt));
            cipher.updateAAD(aad.getBytes());
            return cipher;
        } catch (Exception e) {
            return die("getDecryptionCipher: "+e);
        }
    }

    public InputStream wrapRead(InputStream in, byte[] salt, String aad) throws IOException {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in, BUFFER_SIZE);
        }
        return new CipherInputStream(in, getDecryptionCipher(salt, aad));
    }

    public InputStream wrapWrite(InputStream in, byte[] salt, String aad) throws IOException {
        if (!(in instanceof BufferedInputStream)) {
            in = new BufferedInputStream(in, BUFFER_SIZE);
        }
        return new CipherInputStream(in, getEncryptionCipher(salt, aad));
    }

}
