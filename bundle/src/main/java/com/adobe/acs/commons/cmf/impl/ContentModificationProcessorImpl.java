/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.cmf.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.cmf.ContentModificationProcessor;
import com.adobe.acs.commons.cmf.ContentModificationStep;
import com.adobe.acs.commons.cmf.IdentifiedResources;
import com.adobe.acs.commons.cmf.NoSuchContentModificationStepException;

@Component
@Service
@Properties({
    @Property(name="felix.webconsole.label", value="cmf"),
    @Property(name="felix.webconsole.title", value="Content Modification Framework"),
})
@SuppressWarnings("serial")
public class ContentModificationProcessorImpl extends HttpServlet implements ContentModificationProcessor {

    @Reference(cardinality=ReferenceCardinality.OPTIONAL_MULTIPLE,
            policy=ReferencePolicy.DYNAMIC,
            referenceInterface=com.adobe.acs.commons.cmf.ContentModificationStep.class)
    HashMap<String, ContentModificationStep> registeredSteps = new HashMap<String,ContentModificationStep>();

    private static final Logger log = LoggerFactory.getLogger(ContentModificationProcessorImpl.class);

    protected void bindContentModificationStep (ContentModificationStep step, Map properties) {
        synchronized (registeredSteps) {
            String name = (String) properties.get(ContentModificationStep.STEP_NAME);
            if (StringUtils.isEmpty(name)) {
                name = step.getClass().getName();
            }
            registeredSteps.put (name, step);
            log.info("registered content modification step '{}'", name);
        }
    }

    protected void unbindContentModificationStep (ContentModificationStep step, Map properties) {
        synchronized (registeredSteps) {
            String name = (String) properties.get(ContentModificationStep.STEP_NAME);
            if (StringUtils.isEmpty(name)) {
                name = step.getClass().getName();
            }
            registeredSteps.remove(name);
            log.info("unregistered content modification step '{}'", name);
        }
    }

    /** Webconsole **/
    protected void doGet(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
        PrintWriter pw = res.getWriter();
        pw.println("<div class='statline'>Registered Content Modification Steps:</div>");
        pw.println("<ul>");
        for (String step: registeredSteps.keySet()) {
            pw.println("<li>" + step + "</li>");
        }
        pw.println("</ul>");
    }



    @Override
    public IdentifiedResources identifyAffectedResources(String name, String path,
            ResourceResolver resolver) throws NoSuchContentModificationStepException {

        ContentModificationStep cms = registeredSteps.get(name);
        if (cms == null) {
            String msg = String.format("ContentModificationStep %s not found", name);
            throw new NoSuchContentModificationStepException(msg);
        }

        Resource rootResource = resolver.getResource(path);

        List <Resource> r=  cms.identifyResources(rootResource);

        List <String> result = new ArrayList<String> (r.size());
        Iterator<Resource> iter = r.iterator();
        while (iter.hasNext()) {
            Resource res = iter.next();
            result.add(res.getPath());
        }

        log.info("ContentModificationStep {} identified {} resources below {} for modification", new Object[]{name,result.size(), path});

        return new IdentifiedResources (result, name);


    }

    @Override
    public void modifyResources(IdentifiedResources resources,
            ResourceResolver resolver)
            throws NoSuchContentModificationStepException, PersistenceException {

        ContentModificationStep cms = registeredSteps.get(resources.getContentModificationStep());
        if (cms == null) {
            String msg = String.format("ContentModificationStep %s not found", resources.getContentModificationStep());
            throw new NoSuchContentModificationStepException(msg);
        }

        Iterator<String> iter = resources.getPaths().iterator();
        while (iter.hasNext()) {
            String path = iter.next();
            Resource toModify = resolver.getResource(path);
            cms.performModification(toModify);
            toModify.getResourceResolver().commit();
        }


    }

}
