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
package com.adobe.acs.commons.dam.audio.watson.impl;

import aQute.bnd.annotation.ProviderType;

import java.io.InputStream;

/**
 * Service fronting Watson Speech to Text API.
 */
@ProviderType
public interface TranscriptionService {

    /**
     * Start a transcription job with Watson.
     *
     * @param stream the audio stream. must be in a supported format.
     * @param mimeType the content type of the stream.
     * @return a job ID
     */
    String startTranscriptionJob(InputStream stream, String mimeType);

    /**
     * Retrieve the current result for the job ID.
     *
     * @param jobId the job ID
     * @return the current result
     */
    Result getResult(String jobId);

    /**
     * Result value interface.
     */
    interface Result {
        /**
         * Checks if the job is complete.
         *
         * @return true if the job is complete
         */
        boolean isCompleted();

        /**
         * Get the text transcription for a completed job.
         *
         * @return the text content or null for an incomplete job
         */
        String getContent();
    }
}
