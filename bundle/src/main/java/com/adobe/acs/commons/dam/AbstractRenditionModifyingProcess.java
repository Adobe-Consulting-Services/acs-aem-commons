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

import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.workflow.exec.WorkflowProcess;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.mime.MimeTypeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ConsumerType;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
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
@ConsumerType
public abstract class AbstractRenditionModifyingProcess {

    private static final int MAX_GIF_QUALITY = 255;

    private static final String DEFAULT_QUALITY = "60";

    private static final String ARG_QUALITY = "quality";

    private static final String ARG_RENDITION_NAME = "renditionName";

    /**
     * Logger instance for this class.
     */
    private static final Logger log = LoggerFactory.getLogger(AbstractRenditionModifyingProcess.class);

    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaData, WorkflowHelper workflowHelper)
            throws WorkflowException {
        String[] args = workflowHelper.buildArguments(metaData);

        final String renditionName = workflowHelper.getValuesFromArgs(ARG_RENDITION_NAME, args).size() > 0 ? workflowHelper.getValuesFromArgs(
                ARG_RENDITION_NAME, args).get(0) : null;

        // image quality: from 0 t0 100%
        final String qualityStr = workflowHelper.getValuesFromArgs(ARG_QUALITY, args).size() > 0 ? workflowHelper.getValuesFromArgs(ARG_QUALITY, args)
                .get(0) : DEFAULT_QUALITY;

        if (renditionName == null) {
            log.warn("Rendition name was not configured in arguments. Skipping.");
            return;
        }

        final WorkflowHelper.AssetResourceResolverPair pair = workflowHelper.getAssetFromPayload(workItem, workflowSession);
        if (pair == null) {
            return;
        }
        final Rendition rendition = pair.asset.getRendition(new PrefixRenditionPicker(renditionName));

        if (rendition == null) {
            log.warn("Rendition name {} was not available for asset {}. Skipping.", renditionName, pair.asset);
            return;
        }

        Layer layer = null;
        try {
            layer = new Layer(rendition.getStream());

            layer = processLayer(layer, rendition, workflowSession, args);

            String mimetype = layer.getMimeType();
            double quality = mimetype.equals("image/gif") ? workflowHelper.getQuality(MAX_GIF_QUALITY, qualityStr) : workflowHelper.getQuality(1.0,
                    qualityStr);

            saveImage(pair.asset, rendition, layer, mimetype, quality, workflowHelper);
        } catch (IIOException e) {
            log.warn("Unable to load image layer from " + rendition.getPath(), e);
        } catch (IOException e) {
            log.warn("Unable to load image layer from " + rendition.getPath(), e);
        } finally {
            if (layer != null) {
                layer.dispose();
                layer = null;
            }
            pair.resourceResolver.close();
        }

    }

    void saveImage(Asset asset, Rendition toReplace, Layer layer, String mimetype, double quality, WorkflowHelper workflowHelper)
            throws IOException {
        File tmpFile = File.createTempFile(getTempFileSpecifier(), "." + workflowHelper.getExtension(mimetype));
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
