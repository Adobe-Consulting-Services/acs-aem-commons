package com.adobe.acs.commons.httpcache.exception;

/**
 *
 */
public class HttpCacheEngineException extends HttpCacheException {
    public HttpCacheEngineException() {
    }

    public HttpCacheEngineException(String message) {
        super(message);
    }

    public HttpCacheEngineException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheEngineException(Throwable cause) {
        super(cause);
    }

    public HttpCacheEngineException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
