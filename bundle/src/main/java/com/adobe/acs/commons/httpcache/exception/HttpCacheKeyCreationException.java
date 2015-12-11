package com.adobe.acs.commons.httpcache.exception;

/**
 * Custom exception representing failure conditions in creating a cache key.
 */
public class HttpCacheKeyCreationException extends HttpCacheException {
    public HttpCacheKeyCreationException() {
    }

    public HttpCacheKeyCreationException(String message) {
        super(message);
    }

    public HttpCacheKeyCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheKeyCreationException(Throwable cause) {
        super(cause);
    }

    public HttpCacheKeyCreationException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
