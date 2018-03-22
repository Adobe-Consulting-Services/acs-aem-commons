/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.componentcloner.impl;

import com.day.cq.commons.jcr.JcrUtil;
import com.google.gson.JsonObject;
import javax.annotation.Nonnull;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;

/**
 * A {@link SlingSafeMethodsServlet} extension for cloning components using the component cloner.
 */
@SlingServlet(
        extensions = { "json" },
        methods = { "GET" },
        resourceTypes = { "acs-commons/components/authoring/component-cloner" },
        selectors = { "clone" }
)
public class ComponentClonerServlet extends SlingSafeMethodsServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ComponentClonerServlet.class);

    private static final String ERROR_NAME = "componentClonerError";

    /*
     * (non-Javadoc)
     * @see org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet()
     */
    @Override
    protected void doGet(final @Nonnull SlingHttpServletRequest request, final @Nonnull SlingHttpServletResponse response)
            throws IOException {

        JsonObject responseJson = new JsonObject();
        response.setContentType("application/json");

        try {
            String pathOfNodeToClone = request.getParameter("path");
            if (pathOfNodeToClone == null) {
                LOG.error("The cloned component's path is empty. Please make sure path property is configured.");
                responseJson.addProperty(ERROR_NAME, true);
                response.getWriter().write(responseJson.toString());
            } else {
                Resource nodeToCloneResource = request.getResourceResolver().getResource(pathOfNodeToClone);
                if (nodeToCloneResource == null) {
                    LOG.error("The node to clone's resource came back null. Invalid path, please make sure path property is configured.");
                    responseJson.addProperty(ERROR_NAME, true);
                    response.getWriter().write(responseJson.toString());
                } else {
                    Resource resource = request.getResource();
                    Node nodeToClone = nodeToCloneResource.adaptTo(Node.class);
                    executeNodeCloning(resource, nodeToClone);
                    responseJson.addProperty(ERROR_NAME, false);
                    response.getWriter().write(responseJson.toString());
                }
            }
        } catch (Exception e) {
            LOG.error("An error has occurred cloning the component.", e);
            responseJson.addProperty(ERROR_NAME, true);
            response.getWriter().write(responseJson.toString());
        }
    }

    /**
     * Execute the logic for cloning and moving the node while removing the original 'component-cloner'
     * node and saving the session.
     * @param resource Resource
     * @param nodeToClone Node
     * @throws RepositoryException exception
     */
    protected void executeNodeCloning(final Resource resource, final Node nodeToClone) throws RepositoryException {
        String pathOfNodeToReplace = resource.getPath();
        Node nodeToReplace = resource.adaptTo(Node.class);
        Node parentNode = nodeToReplace.getParent();
        Session session = parentNode.getSession();
        String uniqueNodeName = createUniqueNodeName(0, nodeToClone.getName(), parentNode);
        Node clonedNode = JcrUtil.copy(nodeToClone, parentNode, uniqueNodeName);
        parentNode.orderBefore(clonedNode.getName(), nodeToReplace.getName());
        session.removeItem(pathOfNodeToReplace);
        session.save();
    }

    /**
     * Recursive method to create a unique node name for the cloned node if the cloned node's name exists
     * to avoid overwriting existing nodes.
     * @param count int
     * @param nodeToCloneName String
     * @param parentNode Node
     * @return String
     * @throws RepositoryException exception
     */
    protected String createUniqueNodeName(final int count, final String nodeToCloneName, final Node parentNode) throws RepositoryException {
        if (parentNode.hasNode(nodeToCloneName)) {
            String tempNodeToCloneName = nodeToCloneName.replaceFirst("_([0-9]+)$", "") + "_" + String.valueOf(count);
            return createUniqueNodeName(count + 1, tempNodeToCloneName, parentNode);
        } else {
            return nodeToCloneName;
        }
    }
}
