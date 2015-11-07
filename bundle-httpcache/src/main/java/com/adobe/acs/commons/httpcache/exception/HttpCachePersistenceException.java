package com.adobe.acs.commons.httpcache.exception;

/**
 * Custom exception representing failure conditions in persisting cached items.
 */
public class HttpCachePersistenceException extends HttpCacheException {
    public HttpCachePersistenceException() {
    }

    public HttpCachePersistenceException(String message) {
        super(message);
    }

    public HttpCachePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCachePersistenceException(Throwable cause) {
        super(cause);
    }

    public HttpCachePersistenceException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
