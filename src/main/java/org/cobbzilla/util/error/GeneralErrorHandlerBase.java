package org.cobbzilla.util.error;

import java.util.concurrent.atomic.AtomicReference;

public class GeneralErrorHandlerBase implements GeneralErrorHandler {
    public static final GeneralErrorHandlerBase instance = new GeneralErrorHandlerBase();

    public static AtomicReference<GeneralErrorHandler> defaultErrorHandler() {
        return new AtomicReference<>(GeneralErrorHandlerBase.instance);
    }
}
