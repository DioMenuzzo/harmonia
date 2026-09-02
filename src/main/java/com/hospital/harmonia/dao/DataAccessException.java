package com.hospital.harmonia.dao;

/**
 * Unchecked exception thrown by the DAO layer when a database operation
 * fails (connection error, constraint violation, malformed SQL, etc).
 *
 * DAOs catch the checked {@link java.sql.SQLException} internally, log it
 * (see logback.xml for where logs are written) and rethrow it wrapped in
 * this type instead of a generic RuntimeException, so that callers -- and
 * tests -- can catch a specific, self-documenting exception if they need to
 * react to a persistence failure differently from other runtime errors
 * (e.g. an {@link IllegalArgumentException} coming from a validation rule).
 */
public class DataAccessException extends RuntimeException {

    public DataAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
