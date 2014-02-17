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
package com.adobe.acs.commons.util;

import com.day.cq.commons.TidyJSONWriter;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;
import java.io.IOException;


/**
 * Class used by the vanity URL check validation javascript. It determines if the vanity URL entered by the user is
 * already in use already
 */

@Component(metatype = false)
@Service
@Properties({
        @Property(name = "sling.servlet.paths", value = "/bin/wcm/duplicateVanityCheck"),
        @Property(name = "sling.servlet.methods", value = "GET")
})

public class VanityDuplicateCheck extends SlingAllMethodsServlet{

    private static final Logger logger = LoggerFactory.getLogger(VanityDuplicateCheck.class);

    /**
     * Overriden doGet method, runs a query to see if the vanity URL entered by the user in already in use.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        try{
            Session session = request.getResourceResolver().adaptTo(Session.class);
            final String vanityPath = request.getParameter("vanityPath");
            final String pagePath = request.getParameter("pagePath");
            //logger.debug("vanity path parameter passed is {}", vanityPath);
            //logger.debug("page path parameter passed is {}", pagePath);
            try {
                QueryManager qm = session.getWorkspace().getQueryManager();
                String xpath = "//element(*)[sling:vanityPath='"+ vanityPath + "']";
                //logger.debug("xpath is {}", xpath);

                Query query = qm.createQuery(xpath, Query.XPATH);
                logger.debug("Xpath Query Statement is {}", query.getStatement());
                QueryResult result = query.execute();
                NodeIterator nodes = result.getNodes();
                //logger.debug("result is ", result.getNodes().toString());

                TidyJSONWriter tidyJSONWriter = new TidyJSONWriter(response.getWriter());

                tidyJSONWriter.object();

                tidyJSONWriter.key("vanitypaths").array();

                response.setContentType("text/html");

                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    //logger.debug("Node path is {}", node.getPath());
                    //logger.debug("Page path is {}", pagePath);
                    if(node != null && node.getPath().contains("/content"))
                    {
                        // check whether the path of the page where the vanity path is defined matches the dialog's path
                        // which means that the vanity path is legal.
                        if(node.getPath().equals(pagePath))
                        {
                            //do not add that to the list
                            //logger.debug("Node path is {}", node.getPath());
                            //logger.debug("Page path is {}", pagePath);
                        } else {
                            tidyJSONWriter.value(node.getPath());
                        }
                    }
                }


                tidyJSONWriter.endArray();
                tidyJSONWriter.endObject();
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
            }
            catch(RepositoryException re){
                logger.error( "Error in doGet", re );
            }
        } catch (JSONException e) {
            logger.error( "Error in doGet", e );
        }
    }


}
