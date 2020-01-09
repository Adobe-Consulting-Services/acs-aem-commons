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

package com.adobe.acs.commons.synth.impl;

import com.adobe.acs.commons.synth.SyntheticSlingHttpRequestExecutor;
import com.adobe.acs.commons.synth.impl.support.SyntheticSlingHttpServletResponse;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.engine.SlingRequestProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import java.io.IOException;


@Component
public class SyntheticSlingHttpRequestExecutorImpl implements SyntheticSlingHttpRequestExecutor {

    @Reference
    private SlingRequestProcessor requestProcessor;

    @Override
    public String execute(SlingHttpServletRequest syntheticRequest) throws ServletException, IOException {
        SyntheticSlingHttpServletResponse response = new SyntheticSlingHttpServletResponse();

        requestProcessor.processRequest(syntheticRequest, response, syntheticRequest.getResourceResolver());

        return response.getOutputAsString();
    }

}
