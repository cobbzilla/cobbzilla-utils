package org.cobbzilla.util.jdbc;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.SQLException;

@AllArgsConstructor
public class UncheckedSqlException extends RuntimeException {

    @Getter private final SQLException sqlException;

    @Override public String getMessage() { return sqlException.getMessage(); }

    @Override public String getLocalizedMessage() { return sqlException.getLocalizedMessage(); }

    @Override public synchronized Throwable getCause() { return sqlException.getCause(); }

    @Override public synchronized Throwable initCause(Throwable throwable) { return sqlException.initCause(throwable); }

    @Override public String toString() { return sqlException.toString(); }

}
