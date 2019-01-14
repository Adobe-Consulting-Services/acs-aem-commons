/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.components.longformtext.impl;

import com.adobe.acs.commons.components.longformtext.LongFormTextComponent;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.html.HtmlParser;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * ACS AEM Commons - Components - Long-Form Text
 * Provides support for the ACS AEM Commons Long-form Text Component.
 */
@Component
public class LongFormTextComponentImpl implements LongFormTextComponent {
    private static final Logger log = LoggerFactory.getLogger(LongFormTextComponentImpl.class);

    @Reference
    private HtmlParser htmlParser;

    @Override
    public final String[] getTextParagraphs(final String text) {
        List<String> paragraphs = new ArrayList<String>();

        try {
            final Document doc = htmlParser.parse(null, IOUtils.toInputStream(text, "UTF-8"), "UTF-8");
            doc.getDocumentElement().normalize();

            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            final NodeList bodies = doc.getElementsByTagName("body");

            if (bodies != null && bodies.getLength() == 1) {
                final org.w3c.dom.Node body = bodies.item(0);
                final NodeList children = body.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    StringWriter writer = new StringWriter();
                    StreamResult result = new StreamResult(writer);

                    final org.w3c.dom.Node child = children.item(i);
                    if (child == null) {
                        log.warn("Found a null dom node.");
                        continue;
                    } else if (child.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) {
                        log.warn("Found a dom node is not an element; skipping");
                        continue;
                    }

                    stripNamespaces(child);
                    transformer.transform(new DOMSource(child), result);
                    writer.flush();

                    final String outerHTML = writer.toString();

                    if (StringUtils.isNotBlank(outerHTML)) {
                        paragraphs.add(outerHTML);
                    }
                }
            } else {
                log.debug("HTML does not have a single body tag. Cannot parse as expected.");
            }
        } catch (Exception e) {
            log.warn("Long Form Text encountered a parser error: {}", e);
        }

        return paragraphs.toArray(new String[paragraphs.size()]);
    }


    @Override
    public final void mergeParagraphSystems(final Resource resource,
                                            final int textParagraphSize) throws RepositoryException {
        if (resource == null
                || ResourceUtil.isNonExistingResource(resource)
                || !this.isModifiable(resource)) {
            // Nothing to merge, or user does not have access to merge
            return;
        }

        final Node targetNode = this.getOrCreateLastParagraphSystemResource(resource, textParagraphSize);

        if (targetNode == null) {
            log.info("Could not find last target node to merge long-form-text text inline par resources: {}",
                    textParagraphSize);
            return;
        }

        for (final Resource child : resource.getChildren()) {
            int index = this.getResourceIndex(child);

            if (index > textParagraphSize) {
                this.moveChildrenToNode(child, targetNode);
            }
        }
    }

    @Override
    public boolean hasContents(final Resource resource, final int index) {
        final Resource parResource = resource.getChild(LONG_FORM_TEXT_PAR + index);

        return parResource != null && parResource.listChildren().hasNext();
    }

    private void moveChildrenToNode(Resource resource, Node targetNode) throws RepositoryException {
        for (Resource child : resource.getChildren()) {

            // Use this to create a unique node name; else existing components might get overwritten.
            final Node uniqueNode = JcrUtil.createUniqueNode(targetNode, child.getName(),
                    JcrConstants.NT_UNSTRUCTURED, targetNode.getSession());

            // Once we have a unique node we made as a place holder, we can copy over it w the real component content
            JcrUtil.copy(child.adaptTo(Node.class), targetNode, uniqueNode.getName(), true);
        }

        // Remove the old long-form-text-par- node
        resource.adaptTo(Node.class).remove();

        // Save all changes
        targetNode.getSession().save();
    }

    private Node getOrCreateLastParagraphSystemResource(final Resource resource,
                                                        final int lastIndex) throws RepositoryException {
        final String resourceName = LONG_FORM_TEXT_PAR + lastIndex;
        final Resource lastResource = resource.getChild(resourceName);
        if (lastResource != null) {
            return lastResource.adaptTo(Node.class);
        }

        final Node parentNode = resource.adaptTo(Node.class);

        if (parentNode == null) {
            return null;
        }

        final Session session = parentNode.getSession();
        final Node node = JcrUtil.createPath(parentNode, resourceName, false, JcrConstants.NT_UNSTRUCTURED,
                JcrConstants.NT_UNSTRUCTURED, session, true);

        return node;
    }

    private int getResourceIndex(final Resource resource) {
        final String resourceName = resource.getName();
        if (!StringUtils.startsWith(resourceName, LONG_FORM_TEXT_PAR)) {
            return -1;
        }

        final String indexStr = StringUtils.removeStart(resourceName, LONG_FORM_TEXT_PAR);

        try {
            return Integer.parseInt(indexStr);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private boolean isModifiable(final Resource resource) throws RepositoryException {
        final String writePermissions = "add_node,set_property,remove";
        final Session userSession = resource.getResourceResolver().adaptTo(Session.class);
        final String path = resource.getPath();

        try {
            userSession.checkPermission(path, writePermissions);
        } catch (java.security.AccessControlException e) {
            log.debug("User does not have modify permissions [ {} ] on [ {} ]", writePermissions, resource.getPath());
            return false;
        }
        return true;
    }


    /**
     * Method borrowed from: https://blog.avisi.nl/2013/07/24/java-stripping-namespaces-from-xml-using-dom/
     *
     * Recursively renames the namespace of a node.
     * @param node the starting node.
     */
    private void stripNamespaces(org.w3c.dom.Node node) {
        Document document = node.getOwnerDocument();
        if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            document.renameNode(node, null, node.getNodeName());
        }
        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); ++i) {
            stripNamespaces(list.item(i));
        }
    }
}
