package com.adobe.acs.commons.httpcache.exception;

/**
 * Custom exception representing failure condition in accessing JCR repository.
 */
public class HttpCacheRepositoryAccessException extends HttpCacheException {
    public HttpCacheRepositoryAccessException() {
    }

    public HttpCacheRepositoryAccessException(String message) {
        super(message);
    }

    public HttpCacheRepositoryAccessException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheRepositoryAccessException(Throwable cause) {
        super(cause);
    }

    public HttpCacheRepositoryAccessException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
