package com.adobe.acs.commons.httpcache.exception;

/**
 * Custom exception representing failure condition in accessing repository.
 */
public class HttpCacheReposityAccessException extends HttpCacheException {
    public HttpCacheReposityAccessException() {
    }

    public HttpCacheReposityAccessException(String message) {
        super(message);
    }

    public HttpCacheReposityAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheReposityAccessException(Throwable cause) {
        super(cause);
    }

    public HttpCacheReposityAccessException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
