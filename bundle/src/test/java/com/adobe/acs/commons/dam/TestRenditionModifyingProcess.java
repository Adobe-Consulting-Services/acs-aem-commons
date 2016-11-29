/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

import java.io.IOException;

import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.Rendition;
import com.day.cq.workflow.WorkflowSession;
import com.day.image.Layer;

public class TestRenditionModifyingProcess extends AbstractRenditionModifyingProcess {

    private TestHarness harness;

    @Override
    protected String getTempFileSpecifier() {
        return this.harness.getTempFileSpecifier();
    }

    @Override
    protected Layer processLayer(Layer layer, Rendition rendition, WorkflowSession workflowSession, String[] args) {
        return this.harness.processLayer(layer, rendition, workflowSession, args);
    }

    @Override
    void saveImage(Asset asset, Rendition toReplace, Layer layer, String mimetype, double quality, WorkflowHelper workflowHelper)
            throws IOException {
        this.harness.saveImage(asset, toReplace, layer, mimetype, quality);
    }

}