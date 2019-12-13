package org.cobbzilla.util.collection.multi;

import java.util.Map;

public interface MultiResultDriver {

    MultiResult getResult ();

    // called before trying calculate result
    void before ();

    void exec (Object task);

    // called if calculation was a success
    void success (String message);

    // called if calculation failed
    void failure (String message, Exception e);

    // called at the end (should via finally block)
    void after ();

    // allows the caller/user to stash things for use during execution
    Map<String, Object> getContext();
    void setContext(Map<String, Object> context);

    int getMaxConcurrent();
    long getTimeout();

}
