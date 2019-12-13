package org.cobbzilla.util.security;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomUtils;
import org.cobbzilla.util.io.FileUtil;
import org.junit.Test;

import java.io.*;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.io.FileUtil.abs;
import static org.cobbzilla.util.system.CommandShell.execScript;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CryptoUtilTest {

    @Test public void testStringEncryption () throws Exception {
        final String plaintext = randomAlphanumeric(50+RandomUtils.nextInt(100, 500));
        final String key = randomAlphanumeric(20);
        final String ciphertext = CryptoUtil.string_encrypt(plaintext, key);
        assertEquals(plaintext, CryptoUtil.string_decrypt(ciphertext, key));
    }

    @Test public void testRsa () throws Exception {
        final RsaKeyPair pair1 = RsaKeyPair.newRsaKeyPair();
        final RsaKeyPair pair2 = RsaKeyPair.newRsaKeyPair();

        final String p1data = randomAlphanumeric(1000);
        final RsaMessage p1enc = pair1.encrypt(p1data, pair2);

        final String decrypted = pair2.decrypt(p1enc, pair1.setPrivateKey(null));
        assertEquals(decrypted, p1data);

        final String sshKey = pair2.getSshPublicKey();
        assertNotNull(sshKey);
    }

    @Test public void testStream () throws Exception {
        final String password = randomAlphanumeric(30);
        final byte[] salt = RandomUtils.nextBytes(128);
        final String aad = randomAlphanumeric(400);

        final CryptStream stream = new CryptStream(password);
        final File temp = FileUtil.temp(".tmp");

        // write a huge file to disk, approx 80MB
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(temp))) {
            for (int i=0; i<100000; i++) {
                out.write(RandomUtils.nextBytes(8192));
            }
        }

        // encrypt the file, save in another file
        // if we stream correctly, we should not run out of memory
        final File enc = new File(temp.getParentFile(), temp.getName()+".enc");
        try (InputStream in = stream.wrapWrite(new BufferedInputStream(new FileInputStream(temp)), salt, aad)) {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(enc))) {
                IOUtils.copyLarge(in, out);
            }
        }

        // decrypt the file
        // if we stream correctly, we should not run out of memory
        final File dec = new File(temp.getParentFile(), temp.getName()+".dec");
        try (InputStream in = stream.wrapRead(new BufferedInputStream(new FileInputStream(enc)), salt, aad)) {
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(dec))) {
                IOUtils.copyLarge(in, out);
            }
        }

        // assert files are the same
        final String result = execScript("diff "+abs(dec)+" "+abs(temp));
        assertEquals("expected no diff", "", result);
    }

}
