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
package com.adobe.acs.commons.oak.impl;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.oak.OakIndexManager;
import com.google.common.collect.ImmutableList;


/**
 * This implementation of the OakIndexManager also provides a small
 * interface via the OSGI console to check the status of all 
 * of EnsureOakIndex instances.
 * 
 *
 */

@Component(immediate=true)
@Service()
@Properties({
  @Property(name="felix.webconsole.title",value="Oak Ensure Index"),
  @Property(name="felix.webconsole.label", value="oakEnsureIndex"),
  @Property(name="felix.webconsole.category", value="Sling")
})

public class OakIndexManagerImpl extends HttpServlet implements OakIndexManager {
    
    private static Logger LOG = LoggerFactory.getLogger(OakIndexManagerImpl.class);

    @Reference(cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE, 
            referenceInterface=IndexApplier.class, 
            policy=ReferencePolicy.DYNAMIC)
    List<IndexApplier> availableIndexes = new ArrayList<IndexApplier>();

    
    @Override
    public void installAllIndexes() {
        ImmutableList<IndexApplier> indexes = ImmutableList.copyOf (availableIndexes);
        LOG.info("Starting to apply all pending index definitions");
        for (IndexApplier index : indexes) {
            if (! index.isApplied()) {
                index.applyIndex();
                LOG.debug ("Started applying index definition on " + index);
            }
        }
    }
    

    protected void bindAvailableIndexes (IndexApplier index) {
        availableIndexes.add(index);
        LOG.debug("Bound " + index);
    }
    
    
    protected void unbindAvailableIndexes (IndexApplier index) {
        availableIndexes.remove(index);
        LOG.debug("Unbound " + index);
    }


    protected void doGet (HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        final String path = request.getContextPath() + request.getServletPath() + request.getPathInfo();
        ImmutableList<IndexApplier> indexes = ImmutableList.copyOf (availableIndexes);
        
        writer.println("<table class='nicetable'><tr><th>Index</th><th>Status</th></tr>");
        
        for (IndexApplier index: indexes) {
            String isApplied = index.isApplied() ? "Definition applied" : "Definition <b>not</b> applied";
            String out = String.format("<tr><td>%s</td><td>%s</td></tr>", new Object[]{index,isApplied});
            writer.println(out);
        }
        
        writer.println("</table>");
        writer.println("<p>Please note, that this does not necessarily mean, "
                + "that the indexes have already been created!</p><p>&nbsp;</p>");
        
        writer.println(String.format(
                "<form method=\"POST\" action=\"%s\"><button type=\"submit\">Apply pending definitions</button></form>",
                path));
        
    }
    
    protected void doPost( HttpServletRequest request, HttpServletResponse response) throws IOException {
        PrintWriter writer = response.getWriter();
        final String path = request.getContextPath() + request.getServletPath() + request.getPathInfo();
        
        installAllIndexes();
        
        response.sendRedirect(path);
        
    }


    
    
}
