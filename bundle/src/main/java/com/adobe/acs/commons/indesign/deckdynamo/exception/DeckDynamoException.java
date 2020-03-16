package com.adobe.acs.commons.indesign.deckdynamo.exception;

public class DeckDynamoException extends Exception {

    private static final long serialVersionUID = 1955355079908933046L;

    /**
     * Creates a Deck Dynamo Exception.
     */
    public DeckDynamoException() {
    }

    /**
     * Creates a Deck Dynamo Exception.
     *
     * @param message Custom message for exception.
     */
    public DeckDynamoException(String message) {
        super(message);
    }

    /**
     * Creates a Deck Dynamo Exception.
     *
     * @param throwable
     */
    public DeckDynamoException(Throwable throwable) {
        super(throwable);
    }

    /**
     * Creates a Deck Dynamo Exception.
     *
     * @param paramString
     * @param throwable
     */
    public DeckDynamoException(String paramString, Throwable throwable) {
        super(paramString, throwable);
    }

}
