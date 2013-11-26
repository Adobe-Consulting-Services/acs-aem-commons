/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.handler.ffmpeg.FFMpegWrapper;
import com.day.cq.dam.video.FFMpegTranscodeProcess.Arguments;
import com.day.cq.dam.video.VideoProfile;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component(label = "CQ DAM FFmpeg Audio Encode Process",
        description = "Workflow process that transcodes audio files into different formats")
@Service
@Properties({ @Property(name = "process.label", value = "Encode Audio", propertyPrivate = true) })
public final class FFMpegAudioEncodeProcess extends AbstractFFMpegAudioProcess {

    private static final Logger log = LoggerFactory.getLogger(FFMpegAudioEncodeProcess.class);

    String[] buildArguments(MetaDataMap metaData) {
        String processArgs = metaData.get("PROCESS_ARGS", String.class);
        if (processArgs != null && !processArgs.equals("")) {
            return processArgs.split(",");
        } else {
            return null;
        }
    }

    protected void processAudio(final MetaDataMap metaData, final Asset asset, final File tmpFile,
            final WorkflowSession wfSession) throws IOException, RepositoryException {

        final long start = System.currentTimeMillis();

        log.info("processing asset [{}]...", asset.getPath());

        ResourceResolver resolver = getResourceResolver(wfSession.getSession());

        // create videos from profiles
        String[] videoProfiles = getVideoProfiles(metaData);
        for (String videoProfile : videoProfiles) {
            VideoProfile profile = VideoProfile.get(resolver, videoProfile);
            if (profile != null) {
                log.info("processAudio: creating audio using profile [{}]", videoProfile);
                // creating temp working directory for ffmpeg
                File tmpWorkingDir = createTempDir(getWorkingDir());
                FFMpegWrapper ffmpegWrapper = FFMpegWrapper.fromProfile(tmpFile, profile, tmpWorkingDir);
                ffmpegWrapper.setExecutableLocator(getLocator());
                FileInputStream fis = null;
                try {
                    final String renditionName = getRenditionName(ffmpegWrapper);
                    final File transcodedAudio = ffmpegWrapper.transcode();
                    fis = new FileInputStream(transcodedAudio);
                    asset.addRendition(renditionName, fis, ffmpegWrapper.getOutputMimetype());
                    if (!transcodedAudio.delete()) {
                        log.error("Transcoded audio file @ {} coud not be deleted");
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    log.error("processAudio: failed creating audio from profile [{}]: {}",
                            videoProfile, e.getMessage());
                } finally {
                    IOUtils.closeQuietly(fis);
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
        log.info("finished processing asset [{}] in [{}ms].", asset.getPath(), System.currentTimeMillis() - start);
    }

    public String[] getVideoProfiles(MetaDataMap metaData) {
        List<String> profiles = getValuesFromArgs(Arguments.VIDEO_PROFILES.getArgumentName(), buildArguments(metaData));
        return profiles.toArray(new String[profiles.size()]);
    }

    private String getRenditionName(FFMpegWrapper ffmpegWrapper) {
        String outputFormat = ffmpegWrapper.getOutputExtension();
        String profileName = ffmpegWrapper.getProfileName();
        StringBuilder builder = new StringBuilder();
        builder.append("cq5dam.audio.").append(profileName);
        builder.append(".").append(outputFormat);
        return builder.toString();
    }

}
