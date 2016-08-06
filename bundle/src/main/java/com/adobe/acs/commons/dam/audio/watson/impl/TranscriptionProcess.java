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

import com.adobe.acs.commons.dam.audio.impl.FFMpegAudioUtils;
import com.adobe.acs.commons.dam.audio.watson.TranscriptionService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowExternalProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.dam.handler.ffmpeg.ExecutableLocator;
import com.day.cq.dam.handler.ffmpeg.FFMpegWrapper;
import com.day.cq.dam.handler.ffmpeg.FfmpegNotFoundException;
import com.day.cq.dam.video.VideoProfile;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

import static com.adobe.acs.commons.dam.audio.impl.FFMpegAudioUtils.createTempDir;
import static com.adobe.acs.commons.dam.audio.impl.FFMpegAudioUtils.resolveWorkingDir;
import static com.day.cq.dam.api.DamConstants.DC_EXTENT;
import static com.day.cq.dam.api.DamConstants.METADATA_FOLDER;

@Component
@Service
@Property(name = "process.name", value = "Generate Audio Transcript with IBM Watson")
public class TranscriptionProcess implements WorkflowExternalProcess {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionProcess.class);

    private static final Serializable NOOP = Long.valueOf(-1);

    @Reference
    private TranscriptionService transcriptionService;

    @Reference
    private ExecutableLocator locator;

    /**
     * FFmpeg working directory. If relative, relative to sling.home.
     */
    @Property(value = "./logs/ffmpeg")
    public static final String PROP_WORKING_DIR = "ffmpeg.workingdir";

    private File workingDir;

    @Override
    public Serializable execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        ResourceResolver resolver = workflowSession.adaptTo(ResourceResolver.class);
        Asset asset = getAssetFromPayload(workItem, resolver);
        if (asset == null) {
            return NOOP;
        }

        String mimeType = asset.getMimeType();
        if (!mimeType.startsWith("video/")) {
            return NOOP;
        }

        String jobId = null;

        File tmpDir = null;
        File tmpWorkingDir = null;
        FileOutputStream fos = null;
        InputStream is = null;
        try {
            // creating temp directory
            tmpDir = FFMpegAudioUtils.createTempDir(null);

            // creating temp working directory for ffmpeg
            tmpWorkingDir = FFMpegAudioUtils.createTempDir(workingDir);

            // streaming file to temp directory
            final File tmpFile = new File(tmpDir, asset.getName().replace(' ', '_'));
            fos = new FileOutputStream(tmpFile);
            is = asset.getOriginal().getStream();
            IOUtils.copy(is, fos);

            final long start = System.currentTimeMillis();

            log.info("processing asset [{}]...", asset.getPath());

            // create videos from profiles
            String videoProfile = "flacmono";
            VideoProfile profile = VideoProfile.get(resolver, videoProfile);
            if (profile != null) {
                log.info("processAudio: creating audio using profile [{}]", videoProfile);
                // creating temp working directory for ffmpeg
                FFMpegWrapper ffmpegWrapper = FFMpegWrapper.fromProfile(tmpFile, profile, tmpWorkingDir);
                ffmpegWrapper.setExecutableLocator(locator);
                FileInputStream fis = null;
                try {
                    final File transcodedAudio = ffmpegWrapper.transcode();
                    jobId = transcriptionService.startTranscriptionJob(transcodedAudio, ffmpegWrapper.getOutputMimetype());
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
            if (log.isInfoEnabled()) {
                final long time = System.currentTimeMillis() - start;
                log.info("finished initial processing of asset [{}] in [{}ms].", asset.getPath(), time);
            }

        } catch (IOException e) {
            throw new WorkflowException(e);
        } catch (FfmpegNotFoundException e) {
            log.error(e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(is);
            IOUtils.closeQuietly(fos);
            try {
                // cleaning up temp directory
                if (tmpDir != null) {
                    FileUtils.deleteDirectory(tmpDir);
                }
            } catch (IOException e) {
                log.error("Could not delete temp directory: {}", tmpDir.getPath());
                throw new WorkflowException(e);

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

        if (jobId != null) {
            return jobId;
        } else {
            return NOOP;
        }
    }

    @Override
    public boolean hasFinished(Serializable serializable, WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) {
        log.info("hasFinished? " + serializable);
        if (isNoopValue(serializable)) {
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
                } catch (Exception e) {
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
        log.info("handleResult " + serializable);
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

    private boolean isNoopValue(Serializable serializable) {
        return NOOP.equals(serializable);
    }

    @Activate
    protected final void activate(ComponentContext ctx) {
        String slingHome = ctx.getBundleContext().getProperty("sling.home");
        workingDir = FFMpegAudioUtils.resolveWorkingDir(slingHome, (String) ctx.getProperties().get(PROP_WORKING_DIR));
    }

}
