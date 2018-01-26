package com.adobe.acs.commons.users.impl;

/**
 * Exception used when Ensure Service User activities fail.
 */
public final class EnsureAuthorizableException extends Exception {
    public EnsureAuthorizableException(String message) {
        super(message);
    }

    public EnsureAuthorizableException(String message, Exception e) {
        super(message, e);
    }
}