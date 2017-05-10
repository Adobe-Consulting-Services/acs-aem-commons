/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.dam.audio.impl;

/**
 * Generic exception for audio processing.
 */
public class AudioException extends Exception {

    /**
     * Default constructor.
     */
    public AudioException() {
    }

    /**
     * Constructor with an error message.
     *
     * @param message an error message
     */
    public AudioException(String message) {
        super(message);
    }

    /**
     * Constructor with an error message and a cause.
     * @param message an error message
     * @param cause a cause
     */
    public AudioException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor with a cause.
     * @param cause a cause
     */
    public AudioException(Throwable cause) {
        super(cause);
    }
}
