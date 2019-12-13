package org.cobbzilla.util.error;

import java.util.concurrent.atomic.AtomicReference;

public interface HasGeneralErrorHandler {

    AtomicReference<GeneralErrorHandler> getErrorHandler ();

    default <T> T error(String message) { return getErrorHandler().get().handleError(message); }
    default <T> T error(String message, Exception e) { return getErrorHandler().get().handleError(message, e); }

}
