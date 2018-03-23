package com.adobe.acs.commons.util.impl.exception;

public class CacheMBeanException extends Exception{
    public CacheMBeanException() {
    }

    public CacheMBeanException(String message) {
        super(message);
    }

    public CacheMBeanException(String message, Throwable cause) {
        super(message, cause);
    }
}
