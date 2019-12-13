package org.cobbzilla.util.error;

import org.cobbzilla.util.string.StringUtil;

import java.util.List;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public interface GeneralErrorHandler {
    default <T> T handleError(String message) { return die(message); }
    default <T> T handleError(String message, Exception e) { return die(message, e); }
    default <T> T handleError(List<String> validationErrors) { return die("validation errors: "+ StringUtil.toString(validationErrors)); }
}
