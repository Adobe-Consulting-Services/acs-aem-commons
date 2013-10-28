package com.adobe.acs.commons.dam.impl;

import java.awt.Color;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.dam.AbstractRenditionModifyingProcess;
import com.day.cq.dam.api.Rendition;
import com.day.cq.workflow.WorkflowSession;
import com.day.image.Layer;

/**
 * Workflow process which mattes an image against a solid background to the specified size.
 */
@Component(metatype = false)
@Service
@Property(name = "process.label", value = "Matte Rendition")
public class MatteRenditionProcess extends AbstractRenditionModifyingProcess {

    private static final Logger log = LoggerFactory.getLogger(MatteRenditionProcess.class);

    enum VerticalPosition {
        top, bottom, middle
    }

    enum HoritzonalPosition {
        left, right, center
    }

    private static final String SPECIFIER = "matte";

    @Override
    protected String getTempFileSpecifier() {
        return SPECIFIER;
    }

    @Override
    protected Layer processLayer(Layer layer, Rendition rendition, WorkflowSession workflowSession, String[] args) {
        final String dimensions = getValuesFromArgs("dimension", args).size() > 0 ? getValuesFromArgs("dimension",
                args).get(0) : null;

        final String backgroundColor = getValuesFromArgs("bgcolor", args).size() > 0 ? getValuesFromArgs(
                "bgcolor", args).get(0) : null;

        final String verticalPositionArgument = getValuesFromArgs("vpos", args).size() > 0 ? getValuesFromArgs(
                "vpos", args).get(0) : null;

        final String horizontalPositionArgument = getValuesFromArgs("hpos", args).size() > 0 ? getValuesFromArgs(
                "hpos", args).get(0) : null;

        if (dimensions != null && backgroundColor != null) {
            int width = getDimension(dimensions)[0];
            int height = getDimension(dimensions)[1];
            if (layer.getHeight() == height && layer.getWidth() == width) {
                return layer;
            }

            Color matteColor = parseColor(backgroundColor, Color.BLACK);
            Layer newLayer = new Layer(width, height, matteColor);
            newLayer.setMimeType(rendition.getMimeType());

            int topAnchor;
            int leftAnchor;

            VerticalPosition vpos = VerticalPosition.valueOf(verticalPositionArgument);
            switch (vpos) {
            case bottom:
                topAnchor = newLayer.getHeight() - layer.getHeight();
                break;
            case middle:
                topAnchor = (newLayer.getHeight() - layer.getHeight()) / 2;
                break;
            default:
                topAnchor = 0;
                break;
            }

            HoritzonalPosition hpos = HoritzonalPosition.valueOf(horizontalPositionArgument);
            switch (hpos) {
            case right:
                leftAnchor = newLayer.getWidth() - layer.getWidth();
                break;
            case center:
                leftAnchor = (newLayer.getWidth() - layer.getWidth()) / 2;
                break;
            default:
                leftAnchor = 0;
                break;
            }
            newLayer.blit(layer, leftAnchor, topAnchor, layer.getWidth(), layer.getHeight(), 0, 0);
            return newLayer;
        } else {
            log.info("No dimension or background color specified. Skipping");
            return layer;
        }
    }

    private Color parseColor(String str, Color defaultColor) {
        if ("transparent".equals(str)) {
            return null;
        } else if (str == null || str.length() != 6) {
            return defaultColor;
        }
        int r = Integer.parseInt(str.substring(0, 2), 16);
        int g = Integer.parseInt(str.substring(2, 4), 16);
        int b = Integer.parseInt(str.substring(4, 6), 16);
        return new Color(r, g, b);
    }

    private Integer[] getDimension(String dimensions) {
        if (dimensions != null) {
            String splits[] = dimensions.split(":");
            Integer d[] = new Integer[2];
            d[0] = Integer.valueOf(splits[0]);
            d[1] = Integer.valueOf(splits[1]);
            return d;
        }
        // default value(s)
        return new Integer[] { 1000, 1000 };
    }

}
