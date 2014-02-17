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
import com.day.cq.wcm.api.NameConstants;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
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

@SlingServlet(
        label = "ACS AEM Commons - Unique Vanity Path Checker",
        description = "Checks if the entered vanity path is already in use",
        metatype = false,
        paths = { "/bin/wcm/duplicateVanityCheck" },
        methods = { "GET" }
)

public class VanityDuplicateCheck extends SlingSafeMethodsServlet{

    private static final Logger log = LoggerFactory.getLogger(VanityDuplicateCheck.class);

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
            //log.debug("vanity path parameter passed is {}", vanityPath);
            //log.debug("page path parameter passed is {}", pagePath);
            try {
                QueryManager qm = session.getWorkspace().getQueryManager();
                String xpath = "//element(*)[" + NameConstants.PN_SLING_VANITY_PATH + "='"+ vanityPath + "']";
                //log.debug("xpath is {}", xpath);

                Query query = qm.createQuery(xpath, Query.XPATH);
                log.debug("Xpath Query Statement is {}", query.getStatement());
                QueryResult result = query.execute();
                NodeIterator nodes = result.getNodes();
                //log.debug("result is ", result.getNodes().toString());

                TidyJSONWriter tidyJSONWriter = new TidyJSONWriter(response.getWriter());

                tidyJSONWriter.object();

                tidyJSONWriter.key("vanitypaths").array();

                while (nodes.hasNext()) {
                    Node node = nodes.nextNode();
                    //log.debug("Node path is {}", node.getPath());
                    //log.debug("Page path is {}", pagePath);
                    if(node != null && node.getPath().startsWith("/content"))
                    {
                        // check whether the path of the page where the vanity path is defined matches the dialog's path
                        // which means that the vanity path is legal.
                        if(node.getPath().equals(pagePath))
                        {
                            //do not add that to the list
                            //log.debug("Node path is {}", node.getPath());
                            //log.debug("Page path is {}", pagePath);
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
                log.error( "Error in doGet", re );
            }
        } catch (JSONException e) {
            log.error( "Error in doGet", e );
        }
    }


}
