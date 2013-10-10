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
 * Abstract asset workflow which performs some action on a particular rendition
 * (which was presumably created by an earlier workflow process).
 * 
 * Arguments:
 * <ul>
 * <li>renditionName - The name of the rendition to modify.</li>
 * </ul>
 *
 */
public abstract class AbstractRenditionModifyingProcess extends AbstractAssetWorkflowProcess {

    private static final int MAX_GIF_QUALITY = 255;

    private static final String DEFAULT_QUALITY = "60";

    private static final int MAX_GENERIC_QUALITY = 100;

    private static final String ARG_QUALITY = "quality";

    private static final String ARG_RENDITION_NAME = "renditionName";

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractRenditionModifyingProcess.class);

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaData)
            throws WorkflowException {
        String[] args = buildArguments(metaData);

        final String renditionName = getValuesFromArgs(ARG_RENDITION_NAME, args).size() > 0 ? getValuesFromArgs(
                ARG_RENDITION_NAME, args).get(0) : null;

        // image quality: from 0 t0 100%
        final String qualityStr = getValuesFromArgs(ARG_QUALITY, args).size() > 0 ? getValuesFromArgs(ARG_QUALITY, args)
                .get(0) : DEFAULT_QUALITY;

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
            double quality = mimetype.equals("image/gif") ? getQuality(MAX_GIF_QUALITY, qualityStr) : getQuality(1.0,
                    qualityStr);

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
    protected final String getExtension(String mimetype) {
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
    protected final double getQuality(double base, String qualityStr) {
        int q = Integer.valueOf(qualityStr);
        double res = base * q / MAX_GENERIC_QUALITY;
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
