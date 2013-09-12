package com.adobe.acs.commons.dam.impl;

import java.io.IOException;
import java.io.InputStream;

import javax.imageio.IIOException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.acs.commons.dam.AbstractRenditionModifyingProcess;
import com.day.cq.dam.api.Rendition;
import com.day.cq.workflow.WorkflowSession;
import com.day.image.Layer;

/**
 * Workflow process which adds a watermark to a rendition. Watermarks will always be anchored in the bottom left corner.
 * 
 * Arguments:
 * <ul>
 * <li>watermark - The repository path of the watermark.</li>
 * </ul>
 */
@Component(metatype = false)
@Service
@Property(name = "process.label", value = "Add Watermark to Rendition")
public final class AddWatermarkToRenditionProcess extends AbstractRenditionModifyingProcess {

    private static final String ARG_WATERMARK = "watermark";

    private static final String WATERMARK_SPECIFIER = ARG_WATERMARK;

    private void addWatermark(Layer layer, Layer watermark) {
        layer.blit(watermark, 0, layer.getHeight() - watermark.getHeight(), watermark.getWidth(),
                watermark.getHeight(), 0, 0);
    }

    private Layer getLayer(String path, Session session) {
        if (path != null) {
            ResourceResolver resolver = getResourceResolver(session);
            Resource resource = resolver.getResource(path);
            if (resource != null) {
                InputStream inStream = resource.adaptTo(InputStream.class);
                if (inStream != null) {
                    try {
                        return new Layer(inStream);
                    } catch (IIOException e) {
                        log.warn("Unable to load image layer from " + path, e);
                    } catch (IOException e) {
                        log.warn("Unable to load image layer from " + path, e);
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected String getTempFileSpecifier() {
        return WATERMARK_SPECIFIER;
    }

    @Override
    protected Layer processLayer(Layer layer, Rendition rendition, WorkflowSession workflowSession, String[] args) {
        final String watermarkPath = getValuesFromArgs(ARG_WATERMARK, args).size() > 0 ? getValuesFromArgs(
                ARG_WATERMARK, args).get(0) : null;

        if (watermarkPath != null) {
            Layer watermark = null;
            try {
                watermark = getLayer(watermarkPath, workflowSession.getSession());

                if (watermark != null) {
                    addWatermark(layer, watermark);
                }
            } finally {
                if (watermark != null) {
                    watermark.dispose();
                    watermark = null;
                }
            }
        } else {
            log.info("No watermark specified. Skipping.");
        }
        return layer;
    }

}
