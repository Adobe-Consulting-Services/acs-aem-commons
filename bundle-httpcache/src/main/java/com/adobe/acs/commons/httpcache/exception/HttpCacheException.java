package com.adobe.acs.commons.httpcache.exception;

/**
 *
 */
public class HttpCacheException extends Exception {

    public HttpCacheException() {
    }

    public HttpCacheException(String message) {
        super(message);
    }

    public HttpCacheException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheException(Throwable cause) {
        super(cause);
    }

    public HttpCacheException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
