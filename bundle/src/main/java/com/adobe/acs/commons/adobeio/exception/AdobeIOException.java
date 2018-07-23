package com.adobe.acs.commons.adobeio.exception;

public class AdobeIOException extends Exception {

    public AdobeIOException(String message) {
        super(message);
    }

    public AdobeIOException(String message, Throwable cause) {
        super(message, cause);
    }
}
