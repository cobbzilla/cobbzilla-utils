package org.cobbzilla.util.daemon;

/**
 * A generic interface for error reporting services like Errbit and Airbrake
 */
public interface ErrorApi {

    void report(Exception e);
    void report(String s);
    void report(String s, Exception e);

}
