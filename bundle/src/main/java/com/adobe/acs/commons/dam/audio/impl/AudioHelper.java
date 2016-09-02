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

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.handler.ffmpeg.ExecutableLocator;
import org.apache.sling.api.resource.ResourceResolver;

import java.io.File;

/**
 * Audio helper interface to reduce code duplication when working with FFMpeg to process Audio files.
 */
public interface AudioHelper {

    /**
     * Prepare an asset for processing by FFMpeg. The actual work is handled by the audioProcessor passed.
     *
     * @param asset an Asset
     * @param resourceResolver a ResourceResolver
     * @param args some arguments object
     * @param audioProcessor an implementation of the AudioProcessor interface
     * @param <A> the arguments type
     * @param <R> the processor result type
     * @return the result of processing
     * @throws AudioException if something goes wrong
     */
    <A,R> R process(Asset asset, ResourceResolver resourceResolver, A args, AudioProcessor<A, R> audioProcessor) throws AudioException;

    /**
     * Callback interface to actually invoke FFMpeg
     * @param <A> the arguments type
     * @param <R> the result type
     */
    interface AudioProcessor<A, R> {

        /**
         * Process a file given an FFMpeg environment.
         *
         * @param asset an Asset
         * @param resourceResolver a ResourceResolver
         * @param tempFile the Asset's original rendition as a File
         * @param locator the FFMpeg Executable Locator
         * @param workingDir a working directory for FFMpeg
         * @param args the arguments object
         * @return a result
         * @throws AudioException if something goes wrong
         */
        R processAudio(Asset asset, ResourceResolver resourceResolver, File tempFile, ExecutableLocator locator, File workingDir, A args)
                throws AudioException;
    }

}
