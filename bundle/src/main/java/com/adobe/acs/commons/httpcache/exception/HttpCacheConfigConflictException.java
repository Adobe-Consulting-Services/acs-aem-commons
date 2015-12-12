package com.adobe.acs.commons.httpcache.exception;

/**
 * Custom exception representing a conflict in resolving cache config.
 */
public class HttpCacheConfigConflictException extends HttpCacheException {
    public HttpCacheConfigConflictException() {
    }

    public HttpCacheConfigConflictException(String message) {
        super(message);
    }

    public HttpCacheConfigConflictException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheConfigConflictException(Throwable cause) {
        super(cause);
    }

    public HttpCacheConfigConflictException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
