package org.cobbzilla.util.system;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Sleep {

    /**
     * sleep for 100ms and throw an exception if interrupted
     */
    public static void sleep () { sleep(100); }

    /**
     * sleep and throw an exception if interrupted
     * @param millis how long to sleep
     */
    public static void sleep (long millis) { sleep(millis, "no reason for sleep given"); }

    /**
     * sleep and throw an exception if interrupted
     * @param millis how long to sleep
     * @param reason something to add to the log statement if we are interrupted
     */
    public static void sleep (long millis, String reason) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new IllegalStateException("sleep interrupted (" + reason + ")");
        }
    }

    /**
     * A nap is something that you might get interrupted doing.
     * @param millis how long to nap
     * @return true if you napped without being interrupted, false if you were interrupted
     */
    public static boolean nap (long millis) { return nap(millis, "no reason for nap given"); }

    /**
     * A nap is something that you might get interrupted doing.
     * @param millis how long to nap
     * @param reason something to add to the log statement if we are interrupted
     * @return true if you napped without being interrupted, false if you were interrupted
     */
    public static boolean nap (long millis, String reason) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            log.info("nap ("+reason+"): interrupted");
            return false;
        }
    }
}
