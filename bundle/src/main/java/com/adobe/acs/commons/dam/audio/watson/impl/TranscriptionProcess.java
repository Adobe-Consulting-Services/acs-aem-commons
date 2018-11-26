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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.dam.audio.impl.AudioException;
import com.adobe.acs.commons.dam.audio.impl.AudioHelper;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowExternalProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.dam.handler.ffmpeg.ExecutableLocator;
import com.day.cq.dam.handler.ffmpeg.FFMpegWrapper;
import com.day.cq.dam.video.VideoProfile;

@Component(name = "ACS AEM Commons - Watson Transcription Workflow Process", service=WorkflowExternalProcess.class,properties= {
                   "process.name=Generate Audio Transcript with IBM Watson"
           },configurationPolicy=ConfigurationPolicy.REQUIRE)
@Designate(ocd=TranscriptionProcess.Config.class)
public class TranscriptionProcess implements WorkflowExternalProcess, AudioHelper.AudioProcessor<MetaDataMap, Serializable> {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionProcess.class);

    @Reference
    private TranscriptionService transcriptionService;

    @Reference
    private AudioHelper audioHelper;
    
    @ObjectClassDefinition(name="ACS AEM Commons - Watson Transcription Workflow Process")
    public @interface Config {
        @AttributeDefinition(name="Transocde Profile",
                description="Profile name for audio transcoding. Must be a format acceptable to Watson",defaultValue=DEFAULT_PROFILE)
        String profile() default DEFAULT_PROFILE;
    }
    
    private static final String DEFAULT_PROFILE = "flacmono";


    private String profileName;

    @Activate
    protected void activate(Config conf) {
        this.profileName = conf.profile();
    }

    @Override
    public Serializable execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
        Asset asset = getAssetFromPayload(workItem, resolver);
        if (asset == null) {
            return null;
        }

        String mimeType = asset.getMimeType();
        if (!mimeType.startsWith("video/") && !mimeType.startsWith("audio/")) {
            return null;
        }

        try {
            Serializable jobId = audioHelper.process(asset, resolver, metaDataMap, this);

            if (jobId != null) {
                return jobId;
            } else {
                return null;
            }
        } catch (AudioException e) {
            throw new WorkflowException("Unable to start transcription process.", e);
        }
    }

    @Override
    @SuppressWarnings("squid:S1141")
    public Serializable processAudio(Asset asset, ResourceResolver resourceResolver, File tempFile,
                                     ExecutableLocator locator, File workingDir, MetaDataMap args) throws AudioException {
        final long start = System.currentTimeMillis();
        String jobId = null;

        log.info("processing asset [{}]...", asset.getPath());

        VideoProfile profile = VideoProfile.get(resourceResolver, profileName);
        if (profile != null) {
            log.info("processAudio: creating audio using profile [{}]", profileName);
            // creating temp working directory for ffmpeg
            FFMpegWrapper ffmpegWrapper = FFMpegWrapper.fromProfile(tempFile, profile, workingDir);
            ffmpegWrapper.setExecutableLocator(locator);
            try {
                final File transcodedAudio = ffmpegWrapper.transcode();
                FileInputStream stream = new FileInputStream(transcodedAudio);
                jobId = transcriptionService.startTranscriptionJob(stream, ffmpegWrapper.getOutputMimetype());
                IOUtils.closeQuietly(stream);
                try {
                    Files.delete(transcodedAudio.toPath());
                } catch (IOException e) {
                    log.error("Transcoded audio file @ " + transcodedAudio.getAbsolutePath() + " coud not be deleted", e);
                }
            } catch (IOException e) {
                log.error("processAudio: failed creating audio from profile [{}]: {}",
                        profileName, e.getMessage());
            }
        }
        if (log.isInfoEnabled()) {
            final long time = System.currentTimeMillis() - start;
            log.info("finished initial processing of asset [{}] in [{}ms].", asset.getPath(), time);
        }
        return jobId;
    }

    @Override
    public boolean hasFinished(Serializable serializable, WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) {
        if (serializable == null) {
            return true;
        }
        ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
        Asset asset = getAssetFromPayload(workItem, resolver);

        if (asset == null) {
            log.error("job started, but asset no longer exists.");
            return true;
        }

        if (serializable instanceof String) {
            TranscriptionService.Result result = transcriptionService.getResult((String) serializable);
            if (result.isCompleted()) {
                try {
                    asset.addRendition("transcription.txt", new ByteArrayInputStream(result.getContent().getBytes("UTF-8")), "text/plain");
                    log.info("Transcription for {} created.", asset.getPath());
                } catch (UnsupportedEncodingException e) {
                    log.error("Unable to save new rendition", e);
                }
                return true;
            } else {
                return false;
            }
        } else {
            log.error("Unexpected serializable {}", serializable);
            return true;
        }
    }

    @Override
    public void handleResult(Serializable serializable, WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        // nothing to do here because the result is handled in hasFinished
    }

    private Asset getAssetFromPayload(WorkItem item, ResourceResolver resourceResolver) {
        if (item.getWorkflowData().getPayloadType().equals("JCR_PATH")) {
            String path = item.getWorkflowData().getPayload().toString();
            Resource resource = resourceResolver.getResource(path);
            if (resource != null) {
                return DamUtil.resolveToAsset(resource);
            } else {
                log.error("Resource [{}] in payload of workflow [{}] does not exist.", path, item.getWorkflow().getId());
            }
        }

        return null;
    }

}
