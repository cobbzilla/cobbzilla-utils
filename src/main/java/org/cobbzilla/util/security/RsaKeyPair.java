package org.cobbzilla.util.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.cobbzilla.util.io.TempDir;
import org.cobbzilla.util.string.Base64;
import org.cobbzilla.util.system.CommandResult;

import java.io.File;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.cobbzilla.util.daemon.ZillaRuntime.*;
import static org.cobbzilla.util.io.FileUtil.*;
import static org.cobbzilla.util.string.StringUtil.safeShellArg;
import static org.cobbzilla.util.system.Bytes.KB;
import static org.cobbzilla.util.system.CommandShell.exec;
import static org.cobbzilla.util.system.CommandShell.execScript;

@NoArgsConstructor @Accessors(chain=true) @EqualsAndHashCode(of={"publicKey"}) @Slf4j
public class RsaKeyPair {

    public static final int DEFAULT_EXPIRATION_DAYS = 30;
    public static final int MAX_RETRIES = 5;

    @Getter @Setter private String publicKey;

    @JsonIgnore @Getter(lazy=true) private final String sshPublicKey = initSshPublicKey();
    private String initSshPublicKey() {
        try {
            @Cleanup final TempDir temp = new TempDir();
            secureFile(temp, "key", getPrivateKey());
            final String sshKey = execScript("cd "+abs(temp)+" && ssh-keygen -f key -y");
            return sshKey.startsWith("ssh-rsa ") ? sshKey : die("error: "+sshKey);
        } catch (Exception e) {
            return die("initSshPublicKey: "+e.getMessage());
        }
    }

    public static boolean isValidSshPublicKey (String key) {
        // sanity checks, avoid writing large files to disk
        if (empty(key) || key.length() < 200 || key.length() > 8*KB) return false;
        try {
            @Cleanup final TempDir temp = new TempDir();
            final File f = FileUtil.toFile(temp+"/key.pub", key);
            final CommandResult result = exec("ssh-keygen -l -f " + abs(f));
            return result.isZeroExitStatus() && result.getStdout().length() > 0;
        } catch (Exception e) {
            log.warn("isValidSshPublicKey: "+shortError(e));
            return false;
        }
    }

    @JsonIgnore @Getter @Setter private String privateKey;
    public boolean hasPrivateKey () { return !empty(privateKey); }

    @JsonIgnore @Getter(lazy=true) private final String sshPrivateKey = initSshPrivateKey();

    private String initSshPrivateKey() {
        try {
            @Cleanup final TempDir temp = new TempDir();
            secureFile(temp, "key", getPrivateKey());
            final String sshKey = execScript("cd "+abs(temp)+" && openssl rsa -in key");
            return sshKey.startsWith("-----BEGIN RSA PRIVATE KEY-----\n") ? sshKey : die("error: "+sshKey);
        } catch (Exception e) {
            return die("initSshPrivateKey: "+e.getMessage());
        }
    }

    public static RsaKeyPair newRsaKeyPair() {
        @Cleanup("delete") final TempDir temp = newRsaKeyDir();
        return new RsaKeyPair()
                .setPrivateKey(toStringOrDie(new File(temp, PRIVATE_KEY_FILE)))
                .setPublicKey(toStringOrDie(new File(temp, PUBLIC_KEY_FILE)));
    }

    public static TempDir newRsaKeyDir() {
        return newRsaKeyDir(DEFAULT_EXPIRATION_DAYS, getDefaultSubject(), 0, MAX_RETRIES);
    }

    public static TempDir newRsaKeyDir(int daysUntilExpiration) {
        return newRsaKeyDir(daysUntilExpiration, getDefaultSubject(), 1, MAX_RETRIES);
    }

    public static TempDir newRsaKeyDir(int daysUntilExpiration, String subject) {
        return newRsaKeyDir(daysUntilExpiration, subject, 1, MAX_RETRIES);
    }

    public static final String PRIVATE_KEY_FILE = "rsa.key";
    public static final String PUBLIC_KEY_FILE = "rsa.cert";
    private static final String PARAM_SUBJECT = "@@SUBJECT@@";
    private static final String PARAM_DAYS = "@@days@@";

    private static final String CMD_NEW_KEY
            = "openssl req -nodes -x509 -sha256 -newkey rsa:4096 -keyout "+PRIVATE_KEY_FILE+" -out "+PUBLIC_KEY_FILE+" "
            + "-days "+PARAM_DAYS+ " -subj \""+PARAM_SUBJECT+"\"";

    public static String getDefaultSubject () {
        return "/C=AQ/ST=Ross Ice Shelf/O=cobbzilla.org/CN=key."+randomAlphanumeric(10)+".cobbzilla.org";
    }

    private static TempDir newRsaKeyDir(int daysUntilExpiration, String subject, int attempt, int maxKeyTries) {

        if (attempt > maxKeyTries) return die("newRsaKeyDir: too many failures");

        final TempDir temp = new TempDir();
        try {
            final String keyCommand = CMD_NEW_KEY
                    .replace(PARAM_DAYS, "" + daysUntilExpiration)
                    .replace(PARAM_SUBJECT, "" + safeShellArg(subject));
            execScript("cd "+abs(temp)+" && "+ keyCommand);
            final File keyFile = new File(temp, PRIVATE_KEY_FILE);
            final File certFile = new File(temp, PUBLIC_KEY_FILE);
            if (!keyFile.exists() || keyFile.length() == 0 || !certFile.exists() || certFile.length() == 0) {
                if (!temp.delete()) log.warn("newRsaKeyPair: error deleting: "+abs(temp));
                return die("newRsaKeyPair: key not created");
            }

            // verify the key actually works
            final String rand = randomAlphanumeric(200);
            final RsaKeyPair key = new RsaKeyPair()
                    .setPrivateKey(toStringOrDie(keyFile))
                    .setPublicKey(toStringOrDie(certFile));
            if (!key.decrypt(key.encrypt(rand, key), key).equals(rand)) {
                log.warn("newRsaKeyDir: bad key, regenerating");
                return newRsaKeyDir(daysUntilExpiration, subject, attempt+1, maxKeyTries);
            }
        } catch (Exception e) {
            log.warn("newRsaKeyDir: error creating/checking key, regenerating: "+e);
            return newRsaKeyDir(daysUntilExpiration, subject, attempt+1, maxKeyTries);
        }
        return temp;
    }

    public RsaMessage encrypt(String data, RsaKeyPair recipient) {
        Exception lastEx = null;
        for (int i=0; i<MAX_RETRIES; i++) {
            try {
                @Cleanup("delete") final TempDir temp = new TempDir();
                secureFile(temp, "data", data);
                secureFile(temp, "recipient.crt", recipient.getPublicKey());
                secureFile(temp, "sender.key", getPrivateKey());
                secureFile(temp, "sender.crt", getPublicKey());
                execScript("cd "+abs(temp)+" && " +
                        // generate random symmetric key
                        "openssl rand -out secret.key 32 && " +

                        // encrypt data with symmetric key
                        // disable PBKDF2, not supported on mac osx
//                        "openssl aes-256-cbc -salt -pbkdf2 -in data -out data.enc -pass file:secret.key && " +
                        "openssl aes-256-cbc -salt -in data -out data.enc -pass file:secret.key && " +

                        // encrypt sym key with recipient's public key
                        "openssl rsautl -encrypt -oaep -pubin -certin -keyform PEM -inkey recipient.crt -in secret.key -out secret.key.enc && " +

                        // sign with sender's private key
                        "openssl dgst -sha256 -sign sender.key -out data.sig data");

                return new RsaMessage()
                        .setPublicKey(getPublicKey())
                        .setSymKey(Base64.readB64(temp, "secret.key.enc"))
                        .setData(Base64.readB64(temp, "data.enc"))
                        .setSignature(Base64.readB64(temp, "data.sig"));
            } catch (Exception e) {
                lastEx = e;
                log.warn("encrypt: "+shortError(e));
            }
        }
        return die("encrypt: "+lastEx);
    }

    public String decrypt(RsaMessage message, RsaKeyPair sender) {

        @Cleanup("delete") final TempDir temp = new TempDir();
        try {
            secureFile(temp, "sender.crt", sender.getPublicKey());
            secureFile(temp, "recipient.key", getPrivateKey());
            secureFileB64(temp, "data.enc", message.getData());
            secureFileB64(temp, "secret.key.enc", message.getSymKey());
            secureFileB64(temp, "data.sig", message.getSignature());

            execScript("cd "+abs(temp)+" && " +
                    // decrypt symmetric key with recipient's private key
                    "openssl rsautl -decrypt -oaep -inkey recipient.key -in secret.key.enc -out secret.key && " +

                    // decrypt data with symmetric key
                    // disable PBKDF2, not supported on mac osx
//                    "openssl aes-256-cbc -d -salt -pbkdf2 -in data.enc -out data -pass file:secret.key && " +
                    "openssl aes-256-cbc -d -salt -in data.enc -out data -pass file:secret.key && " +

                    // verify signature with sender's public key
                    "openssl dgst -sha256 -verify <(openssl x509 -in sender.crt -pubkey -noout) -signature data.sig data");
            return toStringOrDie(new File(temp, "data"));
        } catch (Exception e) {
            return die("decrypt: "+e);
        }
    }
}