package org.cobbzilla.util.security;

import java.io.InputStream;
import java.io.OutputStream;

public interface Crypto {

    public String encrypt (String plaintext);

    public String decrypt (String ciphertext);

    public void encrypt(InputStream in, OutputStream out);

    public byte[] decryptBytes(InputStream in);

    public InputStream decryptStream(InputStream in);

    public String getSecretKey();

}
