package com.adobe.acs.commons.indesign.dynamicdeckdynamo.exception;

public class DynamicDeckDynamoException extends Exception {

    private static final long serialVersionUID = 1955355079908933046L;

    /**
     * Creates a Deck Dynamo Exception.
     */
    public DynamicDeckDynamoException() {
    }

    /**
     * Creates a Deck Dynamo Exception.
     *
     * @param message Custom message for exception.
     */
    public DynamicDeckDynamoException(String message) {
        super(message);
    }

    /**
     * Creates a Deck Dynamo Exception.
     *
     * @param throwable
     */
    public DynamicDeckDynamoException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Creates a Deck Dynamo Exception.
     *
     * @param paramString
     * @param throwable
     */
    public DynamicDeckDynamoException(String paramString, Throwable throwable) {
        super(paramString, throwable);
    }

}
