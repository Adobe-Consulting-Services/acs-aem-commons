/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.dam.audio.impl;

import com.day.cq.dam.api.Asset;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferencePolicy;
import com.day.cq.dam.handler.ffmpeg.ExecutableLocator;
import com.day.cq.dam.handler.ffmpeg.FfmpegNotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

@Component
public class AudioHelperImpl implements AudioHelper {

    private static final Logger log = LoggerFactory.getLogger(AudioHelperImpl.class);

    /**
     * FFmpeg working directory. If relative, relative to sling.home.
     */
        public static final String PROP_WORKING_DIR = "ffmpeg.workingdir";

    @Reference(policy = ReferencePolicy.STATIC)
    private ExecutableLocator locator;

    private File workingDir;

    protected final void activate(ComponentContext ctx) {
        String slingHome = ctx.getBundleContext().getProperty("sling.home");
        workingDir = FFMpegAudioUtils.resolveWorkingDir(slingHome, (String) ctx.getProperties().get(PROP_WORKING_DIR));
        if (!workingDir.exists() && !workingDir.mkdirs()) {
            throw new IllegalStateException("Could not create " + workingDir.getPath());
        }
    }

    @Override
    @SuppressWarnings("findsecbugs:PATH_TRAVERSAL_IN")
    public <A, R> R process(Asset asset, ResourceResolver resourceResolver, A args, AudioProcessor<A, R> audioProcessor)
            throws AudioException {
        File tmpDir = null;
        File tmpWorkingDir = null;
        File tmpFile;

        try {
            // creating temp directory
            tmpDir = FFMpegAudioUtils.createTempDir(null);

            // creating temp working directory for ffmpeg
            tmpWorkingDir = FFMpegAudioUtils.createTempDir(workingDir);

            // streaming file to temp directory
            tmpFile = Files.createTempFile(tmpDir.toPath(), "acs-commons", "audio").toFile();
        } catch (IOException e) {
            throw new AudioException(e);
        }

        try (FileOutputStream fos = new FileOutputStream(tmpFile);
             InputStream is = asset.getOriginal().getStream();) {

            IOUtils.copy(is, fos);

            return audioProcessor.processAudio(asset, resourceResolver, tmpFile, locator, tmpWorkingDir, args);

        } catch (IOException e) {
            throw new AudioException(e);
        } catch (FfmpegNotFoundException e) {
            log.error("Unable to find ffmpeg", e);
            return null;
        } finally {
            try {
                // cleaning up temp directory
                FileUtils.deleteDirectory(tmpDir);
            } catch (IOException e) {
                log.warn("Could not delete temp directory: {}", tmpDir.getPath());
            }
            try {
                // cleaning up ffmpeg's temp working directory
                if (tmpWorkingDir != null) {
                    FileUtils.deleteDirectory(tmpWorkingDir);
                }
            } catch (IOException e) {
                log.warn("Could not delete ffmpeg's temporary working directory: {}", tmpWorkingDir.getPath());
            }
        }
    }
}
