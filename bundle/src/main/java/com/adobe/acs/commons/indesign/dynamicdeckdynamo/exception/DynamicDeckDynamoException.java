/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
