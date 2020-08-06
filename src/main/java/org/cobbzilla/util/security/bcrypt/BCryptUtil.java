package org.cobbzilla.util.security.bcrypt;

import lombok.extern.slf4j.Slf4j;

import static org.cobbzilla.util.daemon.ZillaRuntime.RANDOM;

@Slf4j
public class BCryptUtil {

    private static volatile Integer bcryptRounds = null;

    public synchronized static void setBcryptRounds(int rounds) {
        if (bcryptRounds != null) {
            log.warn("Cannot change bcryptRounds after initialization");
            return;
        }
        bcryptRounds = rounds < 4 ? 4 : rounds; // 4 is minimum bcrypt rounds
        log.info("setBcryptRounds: initialized with "+bcryptRounds+" rounds (param was "+rounds+")");
    }

    public static Integer getBcryptRounds() { return bcryptRounds; }

    public static String hash(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(getBcryptRounds(), RANDOM));
    }

    public static void main (String[] args) {
        int input = 0;
        int rounds = 16;
        try {
            rounds = Integer.valueOf(args[0]);
            input = 1;
        } catch (Exception ignored) {
            // whatever
        }
        setBcryptRounds(rounds);
        System.out.println(hash(args[input]));
    }
}
