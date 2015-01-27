package com.adobe.acs.commons.components.longformtext.impl;

import com.adobe.acs.commons.components.longformtext.LongFormTextComponent;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.html.HtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.List;


@Component(
        label = "ACS AEM Commons - Components - Long-form Text",
        description = "Provides support for the ACS AEM Commons Long-form Text Component."
)
@Service
public class LongFormTextComponentImpl implements LongFormTextComponent {
    private static final Logger log = LoggerFactory.getLogger(LongFormTextComponentImpl.class);

    @Reference
    private HtmlParser htmlParser;

    @Override
    public final String[] getTextParagraphs(final String longFormText) {
        List<String> paragraphs = new ArrayList<String>();

        try {
            final Document doc = htmlParser.parse(null, IOUtils.toInputStream(longFormText), "UTF-8");
            doc.getDocumentElement().normalize();

            final DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
            final LSSerializer lsSerializer = domImplementation.createLSSerializer();

            lsSerializer.getDomConfig().setParameter("xml-declaration", false);
            lsSerializer.getDomConfig().setParameter("namespaces", false);

            final NodeList bodies = doc.getElementsByTagName("body");

            if (bodies != null && bodies.getLength() == 1) {
                final org.w3c.dom.Node body = bodies.item(0);
                final NodeList children = body.getChildNodes();

                for (int i = 0; i < children.getLength(); i++) {
                    final String outerHTML = lsSerializer.writeToString(children.item(i));

                    if (StringUtils.isNotBlank(outerHTML)) {
                        paragraphs.add(outerHTML);
                    }
                }
            } else {
                log.debug("HTML to parse does not have a single body tag.");
            }
        } catch (Exception e) {
            log.debug("Article encounter a parser error: {}", e.getMessage());
        }

        return paragraphs.toArray(new String[paragraphs.size()]);
    }

    @Override
    public final void mergeArticleParagraphSystems(final Resource articleTextResource,
                                                   final int textParagraphSize) throws RepositoryException {
        if (articleTextResource == null
                || ResourceUtil.isNonExistingResource(articleTextResource)
                || !this.isModifiable(articleTextResource)) {
            // Nothing to merge, or user does not have access to merge
            return;
        }

        final List<Resource> children = IteratorUtils.toList(articleTextResource.getChildren().iterator());

        final Node targetNode = this.getOrCreateLastParagraphSystemResource(articleTextResource, textParagraphSize);

        if (targetNode == null) {
            log.info("Could not find last target node to merge article text inline par resources: {}",
                    textParagraphSize);
            return;
        }

        for (final Resource child : children) {
            int index = this.getResourceIndex(child);

            if (index > textParagraphSize) {
                this.moveChildrenToNode(child, targetNode);
            }
        }
    }

    private void moveChildrenToNode(Resource resource, Node targetNode) throws RepositoryException {
        for (Resource child : resource.getChildren()) {

            // Use this to create a unique node name; else existing components might get overwritten.
            final Node uniqueNode = JcrUtil.createUniqueNode(targetNode, child.getName(),
                    JcrConstants.NT_UNSTRUCTURED, targetNode.getSession());

            // Once we have a unique node we made as a place holder, we can copy over it w the real component content
            JcrUtil.copy(child.adaptTo(Node.class), targetNode, uniqueNode.getName(), true);
        }

        // Remove the old article-par- node
        resource.adaptTo(Node.class).remove();

        // Save all changes
        targetNode.getSession().save();
    }

    private Node getOrCreateLastParagraphSystemResource(final Resource articleTextResource,
                                                        final int lastIndex) throws RepositoryException {
        final String resourceName = LONG_FORM_TEXT_PAR + lastIndex;
        final Resource lastResource = articleTextResource.getChild(resourceName);
        if (lastResource != null) {
            return lastResource.adaptTo(Node.class);
        }

        final Node parentNode = articleTextResource.adaptTo(Node.class);

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
}
