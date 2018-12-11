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
package com.adobe.acs.commons.dam.impl;

import java.awt.Color;

import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.dam.AbstractRenditionModifyingProcess;
import com.day.cq.dam.api.Rendition;
import com.day.cq.workflow.WorkflowSession;
import com.day.image.Layer;

/**
 * Workflow process which mattes an image against a solid background to the specified size.
 */
@Component(properties= {"process.label=Matte Rendition"})
@SuppressWarnings({"squid:S00115", "checkstyle:localvariablename"})
public final class MatteRenditionProcess extends AbstractRenditionModifyingProcess implements WorkflowProcess {

    private static final int RADIX_HEX = 16;
    private static final int COLOR_STRING_LENGTH = 6;
    private static final int DEFAULT_WIDTH = 1000;
    private static final int DEFAULT_HEIGHT = 1000;
    private static final Logger log = LoggerFactory.getLogger(MatteRenditionProcess.class);

    enum VerticalPosition {
        top, bottom, middle
    }

    enum HoritzonalPosition {
        left, right, center
    }

    private static final String SPECIFIER = "matte";

    @Reference
    private WorkflowHelper workflowHelper;

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        execute(workItem, workflowSession, metaDataMap, workflowHelper);
    }

    @Override
    protected String getTempFileSpecifier() {
        return SPECIFIER;
    }

    @Override
    protected Layer processLayer(Layer layer, Rendition rendition, WorkflowSession workflowSession, String[] args) {
        final String dimensions = workflowHelper.getValuesFromArgs("dimension", args).size() > 0 ? workflowHelper.getValuesFromArgs("dimension",
                args).get(0) : null;

        final String backgroundColor = workflowHelper.getValuesFromArgs("bgcolor", args).size() > 0 ? workflowHelper.getValuesFromArgs(
                "bgcolor", args).get(0) : null;

        final String verticalPositionArgument = workflowHelper.getValuesFromArgs("vpos", args).size() > 0 ? workflowHelper.getValuesFromArgs(
                "vpos", args).get(0) : null;

        final String horizontalPositionArgument = workflowHelper.getValuesFromArgs("hpos", args).size() > 0 ? workflowHelper.getValuesFromArgs(
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
        } else if (str == null || str.length() != COLOR_STRING_LENGTH) {
            return defaultColor;
        }
        int r = Integer.parseInt(str.substring(0, 2), RADIX_HEX);
        int g = Integer.parseInt(str.substring(2, 4), RADIX_HEX);
        int b = Integer.parseInt(str.substring(4, 6), RADIX_HEX);
        return new Color(r, g, b);
    }

    private Integer[] getDimension(String dimensions) {
        if (dimensions != null) {
            String[] splits = dimensions.split(":");
            Integer[] d = new Integer[2];
            d[0] = Integer.valueOf(splits[0]);
            d[1] = Integer.valueOf(splits[1]);
            return d;
        }
        // default value(s)
        return new Integer[] { DEFAULT_WIDTH, DEFAULT_HEIGHT };
    }

}
