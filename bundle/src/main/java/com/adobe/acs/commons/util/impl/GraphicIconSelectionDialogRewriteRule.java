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
package com.adobe.acs.commons.util.impl;

import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;

import com.adobe.cq.dialogconversion.AbstractDialogRewriteRule;
import com.adobe.cq.dialogconversion.DialogRewriteException;
import com.adobe.cq.dialogconversion.DialogRewriteUtils;

/**
 * DialogRewriteRule which handles rewriting of the 'graphiciconselection' xtype.
 */
@Component
@Service
public final class GraphicIconSelectionDialogRewriteRule extends AbstractDialogRewriteRule {

    private static final String PN_OPTIONS = "options";
    private static final String XTYPE = "graphiciconselection";
    private static final String[] PROPERTIES_TO_COPY = { "name", "fieldLabel", "fieldDescription" };
    private static final String PN_SLING_RESOURCE_TYPE = "sling:resourceType";

    @Override
    public Node applyTo(final Node root, final Set<Node> finalNodes)
            throws DialogRewriteException, RepositoryException {
        final Node parent = root.getParent();
        final String name = root.getName();
        DialogRewriteUtils.rename(root);

        // add node for multifield
        final Node newRoot = parent.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        finalNodes.add(newRoot);
        for (final String propertyName : PROPERTIES_TO_COPY) {
            DialogRewriteUtils.copyProperty(root, propertyName, newRoot, propertyName);
        }

        newRoot.setProperty(PN_SLING_RESOURCE_TYPE, "acs-commons/components/authoring/graphiciconselect");

        if (root.hasProperty(PN_OPTIONS)) {
            final String options = root.getProperty(PN_OPTIONS).getString();
            if (options.startsWith("/etc/acs-commons/lists")) {
                final Node dataSource = newRoot.addNode("datasource", JcrConstants.NT_UNSTRUCTURED);
                dataSource.setProperty(PN_SLING_RESOURCE_TYPE, "acs-commons/components/utilities/genericlist/datasource");
                // assuming the options property is something like /etc/acs-commons/lists/font-awesome-icons/_jcr_content.list.json, we want /etc/acs-commons/lists/font-awesome-icons
                dataSource.setProperty("path", StringUtils.substringBeforeLast(options, "/"));
            }
        }

        root.remove();
        return newRoot;
    }

    @Override
    public boolean matches(final Node root) throws RepositoryException {
        return DialogRewriteUtils.hasXtype(root, XTYPE);
    }

}
