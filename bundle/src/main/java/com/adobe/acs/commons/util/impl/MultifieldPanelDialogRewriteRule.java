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
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;

import com.adobe.cq.dialogconversion.AbstractDialogRewriteRule;
import com.adobe.cq.dialogconversion.DialogRewriteException;
import com.adobe.cq.dialogconversion.DialogRewriteUtils;

/**
 * DialogRewriteRule which handles rewriting of the 'multifieldpanel' xtype.
 */
@Component
@Service
public final class MultifieldPanelDialogRewriteRule extends AbstractDialogRewriteRule {

    private static final String PN_NAME = "name";
    private static final String PN_SLING_RESOURCE_TYPE = "sling:resourceType";
    private static final String NN_ITEMS = "items";
    private static final String XTYPE = "multifieldpanel";

    @Override
    public Node applyTo(final Node root, final Set<Node> finalNodes) throws DialogRewriteException, RepositoryException {
        final Node parent = root.getParent();
        final String name = root.getName();
        DialogRewriteUtils.rename(root);

        // add node for multifield
        final Node newRoot = parent.addNode(name, JcrConstants.NT_UNSTRUCTURED);
        finalNodes.add(newRoot);
        DialogRewriteUtils.copyProperty(root, PN_NAME, newRoot, PN_NAME);

        newRoot.setProperty(PN_SLING_RESOURCE_TYPE, "granite/ui/components/foundation/form/fieldset");
        newRoot.setProperty("acs-commons-nested", "");

        final Node layout = newRoot.addNode("layout", JcrConstants.NT_UNSTRUCTURED);
        layout.setProperty(PN_SLING_RESOURCE_TYPE, "granite/ui/components/foundation/layouts/fixedcolumns");
        layout.setProperty("method", "absolute");

        final Node rootItems = newRoot.addNode(NN_ITEMS, JcrConstants.NT_UNSTRUCTURED);
        final Node container = rootItems.addNode("column", JcrConstants.NT_UNSTRUCTURED);
        container.setProperty(PN_SLING_RESOURCE_TYPE, "granite/ui/components/foundation/container");

        final Node columnItems = container.addNode(NN_ITEMS, JcrConstants.NT_UNSTRUCTURED);

        if (root.hasNode(NN_ITEMS)) {
            for (final NodeIterator children = root.getNode(NN_ITEMS).getNodes(); children.hasNext();) {
                final Node child = children.nextNode();
                root.getSession().move(child.getPath(), columnItems.getPath() + "/" + child.getName());
            }
        }

        finalNodes.add(newRoot);
        root.remove();
        return newRoot;
    }

    @Override
    public boolean matches(final Node root) throws RepositoryException {
        return DialogRewriteUtils.hasXtype(root, XTYPE);
    }

}
