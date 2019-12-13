package org.cobbzilla.util.json;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.exception.ExceptionUtils;

@NoArgsConstructor @Accessors(chain=true)
public class JsonSerializableException {

    @Getter @Setter private String exceptionClass;
    @Getter @Setter private String message;
    @Getter @Setter private String stackTrace;

    public JsonSerializableException (Throwable t) {
        exceptionClass = t.getClass().getName();
        message = t.getMessage();
        stackTrace = ExceptionUtils.getStackTrace(t);
    }

    public String shortString () { return exceptionClass + ": " + getMessage(); }

    public String toString () { return shortString() + "\n" + getStackTrace(); }

}
