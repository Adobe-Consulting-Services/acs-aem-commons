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

import static com.day.cq.dam.api.DamConstants.DC_EXTENT;
import static com.day.cq.dam.api.DamConstants.METADATA_FOLDER;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.handler.ffmpeg.ExecutableLocator;
import com.day.cq.dam.handler.ffmpeg.FFMpegWrapper;
import com.day.cq.dam.video.FFMpegTranscodeProcess.Arguments;
import com.day.cq.dam.video.VideoProfile;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

/**
 * CQ DAM FFmpeg Audio Encode Process
 * Workflow process that transcodes audio files into different formats
 */
@Component(service=WorkflowProcess.class,properties= {
        "process.label=Encode Audio"
})
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public final class FFMpegAudioEncodeProcess implements WorkflowProcess, AudioHelper.AudioProcessor<MetaDataMap, Void> {

    @Reference
    private AudioHelper helper;

    @Reference
    private WorkflowHelper workflowHelper;

    private static final Logger log = LoggerFactory.getLogger(FFMpegAudioEncodeProcess.class);

    @SuppressWarnings({"PMD.CollapsibleIfStatements", "squid:S1066"})
    @Override
    public final void execute(WorkItem workItem, WorkflowSession wfSession, MetaDataMap metaData)
            throws WorkflowException {

        final WorkflowHelper.AssetResourceResolverPair pair = workflowHelper.getAssetFromPayload(workItem, wfSession);

        if (pair == null) {
            String wfPayload = workItem.getWorkflowData().getPayload().toString();
            String message = "execute: cannot process audio, asset [{" + wfPayload
                    + "}] in payload doesn't exist for workflow [{" + workItem.getId() + "}].";
            throw new WorkflowException(message);
        }

        final String assetMimeType = pair.asset.getMimeType();
        if ((assetMimeType == null || !assetMimeType.startsWith("audio/"))) {
            if (!pair.asset.getName().endsWith(".wav") || !pair.asset.getName().endsWith(".mp3")
                    || !pair.asset.getName().endsWith(".ogg")) {
                log.info("execute: asset [{}] is not of a audio mime type, asset ignored.", pair.asset.getPath());
                return;
            }
        }

        try {
            helper.process(pair.asset, pair.resourceResolver, metaData, this);
        } catch (AudioException e) {
            throw new WorkflowException("Unable to transcode audio", e);
        } finally {
            pair.resourceResolver.close();
        }
    }

    @Override
    @SuppressWarnings({"squid:S3776", "squid:S1141"})
    public Void processAudio(final Asset asset, final ResourceResolver resourceResolver, final File tempFile,
                        final ExecutableLocator locator, final File workingDir, final MetaDataMap metaData) throws AudioException {

        final long start = System.currentTimeMillis();

        log.info("processing asset [{}]...", asset.getPath());

        // create audio files from profiles
        String[] videoProfiles = getVideoProfiles(metaData);
        for (String videoProfile : videoProfiles) {
            VideoProfile profile = VideoProfile.get(resourceResolver, videoProfile);
            if (profile != null) {
                log.info("processAudio: creating audio using profile [{}]", videoProfile);
                FFMpegWrapper ffmpegWrapper = FFMpegWrapper.fromProfile(tempFile, profile, workingDir);
                ffmpegWrapper.setExecutableLocator(locator);
                FileInputStream fis = null;
                try {
                    final String renditionName = getRenditionName(ffmpegWrapper);
                    final File transcodedAudio = ffmpegWrapper.transcode();
                    fis = new FileInputStream(transcodedAudio);
                    asset.addRendition(renditionName, fis, ffmpegWrapper.getOutputMimetype());
                    try {
                        Files.delete(transcodedAudio.toPath());
                    } catch (Exception e) {
                        log.error("Transcoded audio file @ " + transcodedAudio.getAbsolutePath() +" coud not be deleted", e);
                    }
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                    log.error("processAudio: failed creating audio from profile [{}]: {}",
                            videoProfile, e.getMessage());
                }
            }
        }
        if (log.isInfoEnabled()) {
            final long time = System.currentTimeMillis() - start;
            log.info("finished processing asset [{}] in [{}ms].", asset.getPath(), time);
        }

        // get information about original audio file (size, video length,
        // ...)
        FFMpegWrapper wrapper = new FFMpegWrapper(tempFile, workingDir);
        wrapper.setExecutableLocator(locator);

        final Resource assetResource = asset.adaptTo(Resource.class);
        final Resource metadata = resourceResolver.getResource(assetResource, JcrConstants.JCR_CONTENT + "/" + METADATA_FOLDER);

        if (null != metadata) {
            try {
                final Node metadataNode = metadata.adaptTo(Node.class);
                if (metadataNode != null) {
                    metadataNode.setProperty(DC_EXTENT, wrapper.getInputDuration());
                }
            } catch (RepositoryException e) {
                log.warn("Unable to set metadata for asset [" + asset.getPath() + "]", e);
            }
        } else {
            log.warn("execute: failed setting metdata for asset [{}], no metdata node found.",
                    asset.getPath());
        }
        return null;
    }

    private String[] getVideoProfiles(MetaDataMap metaData) {
        List<String> profiles = workflowHelper.getValuesFromArgs(Arguments.VIDEO_PROFILES.getArgumentName(), workflowHelper.buildArguments(metaData));
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
