package org.cobbzilla.util.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;
import static org.cobbzilla.util.daemon.ZillaRuntime.empty;
import static org.cobbzilla.util.security.CryptoUtil.string_decrypt;
import static org.cobbzilla.util.security.CryptoUtil.string_encrypt;

@Slf4j @AllArgsConstructor
public class CryptoSimple implements Crypto {

    @Getter @Setter private String secretKey = null;
    public boolean hasSecretKey() { return !empty(secretKey); }

    @Override public String encrypt (String plaintext) {
        if (empty(secretKey)) die("encrypt: key was not initialized");
        if (empty(plaintext)) return null;
        return string_encrypt(plaintext, secretKey);
    }

    @Override public String decrypt (String ciphertext) {
        if (empty(secretKey)) die("decrypt: key was not initialized");
        if (empty(ciphertext)) return null;
        return string_decrypt(ciphertext, secretKey);
    }

    // todo - support stream-oriented encryption
    @Override public void encrypt(InputStream in, OutputStream out) {
        if (empty(secretKey)) die("encrypt: key was not initialized");
        try {
            final byte[] ciphertext = CryptoUtil.encrypt(in, secretKey);
            IOUtils.copy(new ByteArrayInputStream(ciphertext), out);

        } catch (Exception e) {
            die("encryption failed: "+e, e);
        }
    }

    @Override public byte[] decryptBytes(InputStream in) {
        if (empty(secretKey)) die("encrypt: key was not initialized");
        try {
            return CryptoUtil.decrypt(in, secretKey);
        } catch (Exception e) {
            return die("decryption failed: "+e, e);
        }
    }

    @Override public InputStream decryptStream(InputStream in) {
        try {
            return CryptoUtil.decryptStream(in, secretKey);
        } catch (Exception e) {
            return die("decryption failed: "+e, e);
        }
    }
}
