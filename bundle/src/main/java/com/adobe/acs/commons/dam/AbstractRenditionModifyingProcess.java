package com.adobe.acs.commons.dam;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.IIOException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.dam.commons.process.AbstractAssetWorkflowProcess;
import com.day.cq.dam.commons.util.PrefixRenditionPicker;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.image.Layer;

/**
 * Abstract asset workflow which performs some action on a particular rendition (which was presumably created by an earlier workflow process).
 * 
 * Arguments:
 * <ul>
 * <li>renditionName - The name of the rendition to modify.</li>
 * </ul>
 *
 */
public abstract class AbstractRenditionModifyingProcess extends AbstractAssetWorkflowProcess {

    private static final String ARG_QUALITY = "quality";

    private static final String ARG_RENDITION_NAME = "renditionName";

    /**
     * Logger instance for this class.
     */
    @SuppressWarnings("PMD.LoggerIsNotStaticFinal")
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaData)
            throws WorkflowException {
        String[] args = buildArguments(metaData);

        final String renditionName = getValuesFromArgs(ARG_RENDITION_NAME, args).size() > 0 ? getValuesFromArgs(
                ARG_RENDITION_NAME, args).get(0) : null;

        // image quality: from 0 t0 100%
        final String qualityStr = getValuesFromArgs(ARG_QUALITY, args).size() > 0 ? getValuesFromArgs(ARG_QUALITY, args)
                .get(0) : "60";

        if (renditionName == null) {
            log.warn("Rendition name was not configured in arguments. Skipping.");
            return;
        }

        final Asset asset = getAssetFromPayload(workItem, workflowSession.getSession());
        final Rendition rendition = asset.getRendition(new PrefixRenditionPicker(renditionName));

        if (rendition == null) {
            log.warn("Rendition name {} was not available for asset {}. Skipping.", renditionName, asset);
            return;
        }

        Layer layer = null;
        try {
            layer = new Layer(rendition.getStream());

            layer = processLayer(layer, rendition, workflowSession, args);

            String mimetype = layer.getMimeType();
            double quality = mimetype.equals("image/gif") ? getQuality(255, qualityStr) : getQuality(1.0, qualityStr);

            saveImage(asset, rendition, layer, mimetype, quality);
        } catch (IIOException e) {
            log.warn("Unable to load image layer from " + rendition.getPath(), e);
        } catch (IOException e) {
            log.warn("Unable to load image layer from " + rendition.getPath(), e);
        } finally {
            if (layer != null) {
                layer.dispose();
                layer = null;
            }
        }

    }

    private String[] buildArguments(MetaDataMap metaData) {
        // the 'old' way, ensures backward compatibility
        String processArgs = metaData.get("PROCESS_ARGS", String.class);
        if (processArgs != null && !processArgs.equals("")) {
            return processArgs.split(",");
        } else {
            return new String[0];
        }
    }

    void saveImage(Asset asset, Rendition toReplace, Layer layer, String mimetype, double quality)
            throws IOException {
        File tmpFile = File.createTempFile(getTempFileSpecifier(), "." + getExtension(mimetype));
        OutputStream out = FileUtils.openOutputStream(tmpFile);
        InputStream is = null;
        try {
            layer.write(mimetype, quality, out);
            is = FileUtils.openInputStream(tmpFile);
            asset.addRendition(toReplace.getName(), is, mimetype);
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(is);
            FileUtils.deleteQuietly(tmpFile);
        }
    }

    /**
     * Return the extension corresponding to the mime type.
     * 
     * @param mimetype the mimetype
     * @return the corresponding extension
     */
    protected String getExtension(String mimetype) {
        return mimeTypeService.getExtension(mimetype);
    }

    /**
     * Parse the provided quality string, from 1 to 100, and
     * apply it to the base. Allows for a constant scale to be used
     * and applied to different image types which support different
     * quality scales.
     * 
     * @param base the maximal quality value
     * @param qualityStr the string to parse
     * @return a usable quality value
     */
    protected double getQuality(double base, String qualityStr) {
        int q = Integer.valueOf(qualityStr);
        double res = base * q / 100;
        return res;
    }

    /**
     * Create a specifier to be used for temporary file location.
     * 
     * @return the temp file qualifier
     */
    protected abstract String getTempFileSpecifier();

    /**
     * Perform the actual layer processing and return the layer to be saved.
     * 
     * @param layer the source image data
     * @param rendition the source rendition object
     * @param workflowSession the workflow session
     * @param args the parsed process arguments
     * 
     * @return the modified layer
     */
    protected abstract Layer processLayer(Layer layer, Rendition rendition, WorkflowSession workflowSession,
            String[] args);
}
