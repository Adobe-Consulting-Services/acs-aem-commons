package com.adobe.acs.commons.httpcache.exception;

/**
 * Custom exception representing failure conditions in accessing data streams.
 */
public class HttpCacheDataStreamException extends HttpCacheException {
    public HttpCacheDataStreamException() {
    }

    public HttpCacheDataStreamException(String message) {
        super(message);
    }

    public HttpCacheDataStreamException(String message, Throwable cause) {
        super(message, cause);
    }

    public HttpCacheDataStreamException(Throwable cause) {
        super(cause);
    }

    public HttpCacheDataStreamException(String message, Throwable cause, boolean enableSuppression, boolean
            writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
