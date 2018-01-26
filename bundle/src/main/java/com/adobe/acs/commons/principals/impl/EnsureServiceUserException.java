package com.adobe.acs.commons.principals.impl;

/**
 * Exception used when Ensure Service User activities fail.
 */
public final class EnsureServiceUserException extends Exception {
    public EnsureServiceUserException(String message) {
        super(message);
    }

    public EnsureServiceUserException(String message, Exception e) {
        super(message, e);
    }
}