package com.adobe.acs.commons.dam.audio.impl;

import static com.day.cq.commons.jcr.JcrConstants.*;
import static com.day.cq.dam.api.DamConstants.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.process.AbstractAssetWorkflowProcess;
import com.day.cq.dam.handler.ffmpeg.ExecutableLocator;
import com.day.cq.dam.handler.ffmpeg.FFMpegWrapper;
import com.day.cq.dam.handler.ffmpeg.FfmpegNotFoundException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component(componentAbstract = true, metatype = true)
public abstract class AbstractFFMpegAudioProcess extends AbstractAssetWorkflowProcess {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * FFmpeg working directory. If relative, relative to sling.home.
     * 
     */
    @Property(value = "./logs/ffmpeg")
    public static final String PROP_WORKING_DIR = "ffmpeg.workingdir";

    @Reference(policy = ReferencePolicy.STATIC)
    protected ExecutableLocator locator;

    private File workingDir;

    public void execute(WorkItem workItem, WorkflowSession wfSession, MetaDataMap metaData) throws WorkflowException {

        final Asset asset = getAssetFromPayload(workItem, wfSession.getSession());

        if (asset == null) {
            String wfPayload = workItem.getWorkflowData().getPayload().toString();
            String message = "execute: cannot process audio, asset [{" + wfPayload
                    + "}] in payload doesn't exist for workflow [{" + workItem.getId() + "}].";
            throw new WorkflowException(message);
        }

        final String assetMimeType = asset.getMimeType();
        if (assetMimeType == null || !assetMimeType.startsWith("audio/")) {
            if (!asset.getName().endsWith(".wav") || !asset.getName().endsWith(".mp3")
                    || !asset.getName().endsWith(".ogg")) {
                log.info("execute: asset [{}] is not of a audio mime type, asset ignored.", asset.getPath());
                return;
            }
        }

        File tmpDir = null;
        File tmpWorkingDir = null;
        FileOutputStream fos = null;
        InputStream is = null;
        FFMpegWrapper wrapper = null;
        try {
            // creating temp directory
            tmpDir = createTempDir(null);

            // creating temp working directory for ffmpeg
            tmpWorkingDir = createTempDir(getWorkingDir());

            // streaming file to temp directory
            final File tmpFile = new File(tmpDir, asset.getName().replace(' ', '_'));
            fos = new FileOutputStream(tmpFile);
            is = asset.getOriginal().getStream();
            IOUtils.copy(is, fos);

            processAudio(metaData, asset, tmpFile, wfSession);

            // get information about original audio file (size, video length,
            // ...)
            wrapper = new FFMpegWrapper(tmpFile, tmpWorkingDir);
            wrapper.setExecutableLocator(locator);

            final ResourceResolver resolver = getResourceResolver(wfSession.getSession());
            final Resource assetResource = asset.adaptTo(Resource.class);
            final Resource metadata = resolver.getResource(assetResource, JCR_CONTENT + "/" + METADATA_FOLDER);

            if (null != metadata) {

                final Node metadataNode = metadata.adaptTo(Node.class);
                metadataNode.setProperty(DC_EXTENT, wrapper.getInputDuration());

                metadataNode.getSession().save();
            } else {
                log.warn("execute: failed setting metdata for asset [{}] in workflow [{}], no metdata node found.",
                        asset.getPath(), workItem.getId());
            }

        } catch (IOException e) {
            throw new WorkflowException(e);
        } catch (RepositoryException e) {
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
    }

    protected File getWorkingDir() {
        workingDir.mkdir();
        return workingDir;
    }

    private File resolveWorkingDir(String slingHome, String path) {
        if (path == null) {
            path = "";
        }
        // ensure proper separator in the path (esp. for systems, which do
        // not use "slash" as a separator, e.g Windows)
        path = path.replace('/', File.separatorChar);

        // create a file instance and check whether this is absolute. If not
        // create a new absolute file instance with the base dir (sling.home or
        // working dir of current JVM) and get the absolute path name from that
        File workingDir = new File(path);
        if (!workingDir.isAbsolute()) {
            File baseDir = new File((slingHome == null) ? "" /* jvm working dir */: slingHome).getAbsoluteFile();
            workingDir = new File(baseDir, path).getAbsoluteFile();
        }
        try {
            log.info("ffmpeg working directory: {}", workingDir.getCanonicalPath());
        } catch (IOException e) {
            log.info("ffmpeg working directory: {}", workingDir.getAbsolutePath());
        }

        return workingDir;
    }

    protected void activate(ComponentContext ctx) {
        String slingHome = ctx.getBundleContext().getProperty("sling.home");
        workingDir = resolveWorkingDir(slingHome, (String) ctx.getProperties().get(PROP_WORKING_DIR));
    }

    /**
     * creates a temporary directory in the given dir.
     * 
     * @param parentDir parent directory in which temporary directory is to be
     *            created
     */
    protected File createTempDir(File parentDir) {
        File tempDir = null;
        try {
            tempDir = File.createTempFile("cqdam", null, parentDir);
            tempDir.delete();
            tempDir.mkdir();
        } catch (IOException e) {
            log.warn("could not create temp directory in the [{}] with the exception", parentDir, e);
        }
        return tempDir;
    }

    protected abstract void processAudio(final MetaDataMap metaData, final Asset asset, final File tmpFile,
            final WorkflowSession wfSession) throws IOException, RepositoryException;
}
