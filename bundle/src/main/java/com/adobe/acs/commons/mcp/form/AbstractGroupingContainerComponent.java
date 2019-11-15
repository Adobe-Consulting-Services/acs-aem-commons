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
package com.adobe.acs.commons.mcp.form;

import com.adobe.acs.commons.mcp.form.DialogProvider.DialogStyle;
import com.adobe.acs.commons.mcp.util.SyntheticResourceBuilder;
import java.util.LinkedHashMap;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;

/**
 * An expression of the tab layout container with convenience methods to build
 * tabs more easily. The data structures are meant to show content in the order
 * it is added.
 */
public abstract class AbstractGroupingContainerComponent extends ContainerComponent {

    @SuppressWarnings({"squid:S1444", "squid:ClassVariableVisibilityCheck"}) // can't be fixed for backwards compatibility reasons
    public static String GENERIC_GROUP = "Misc";
    public static final String MARGIN_PROPERTY = "margin";
    public static final String ITEMS = "items";

    private String layout = "";
    private boolean margin = true;
    private final Map<String, ContainerComponent> groups = new LinkedHashMap<>();

    public ContainerComponent getOrCreateGroup(String groupName) {
        if (StringUtils.isBlank(groupName)) {
            groupName = getPropertiesTabName() == null ? GENERIC_GROUP : getPropertiesTabName();
        }
        if (!groups.containsKey(groupName)) {
            ContainerComponent tab = new ContainerComponent();
            tab.setTitle(groupName);
            groups.put(groupName, tab);
            super.addComponent(groupName, tab);
            return tab;
        } else {
            return groups.get(groupName);
        }

    }

    public void addComponent(String group, String fieldName, FieldComponent component) {
        getOrCreateGroup(group).addComponent(fieldName, component);
    }

    @Override
    public Resource buildComponentResource() {
        getComponentMetadata().put(MARGIN_PROPERTY, isMargin());
        AbstractResourceImpl res = (AbstractResourceImpl) super.buildComponentResource();
        AbstractResourceImpl layoutResource = new AbstractResourceImpl(res.getPath() + "/layout", getLayout(), null, null);
        res.addChild(layoutResource);
        return res;
    }

    public static class TabsComponent extends AbstractGroupingContainerComponent {

        // See also: https://helpx.adobe.com/experience-manager/6-3/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/tabs/index.html
        private String orientation = "horizontal";
        private String size = "M";

        public TabsComponent() {
            setLayout("granite/ui/components/foundation/layouts/tabs");
        }

        @Override
        public Resource buildComponentResource() {
            if (getDialogStyle() == DialogStyle.COMPONENT) {
                setResourceType("granite/ui/components/coral/foundation/tabs");
                getComponentMetadata().put("maximized", true);
            }
            getComponentMetadata().put("orientation", getOrientation());
            getComponentMetadata().put("size", getSize());
            return super.buildComponentResource();
        }

        @Override
        protected AbstractResourceImpl generateItemsResource(String path, boolean useFieldSet) {
            AbstractResourceImpl items = super.generateItemsResource(path, useFieldSet);
            if (getDialogStyle() == DialogStyle.COMPONENT) {
                SyntheticResourceBuilder rb = new SyntheticResourceBuilder(ITEMS, null);
                items.children.forEach(tab -> {
                    rb.createChild(tab.getName(), tab.getResourceType())
                            .withAttributes(tab.getResourceMetadata())
                            .withAttributes(MARGIN_PROPERTY, true);
                    rb.createChild(ITEMS)
                            .createChild("columns", "granite/ui/components/coral/foundation/fixedcolumns")
                            .withAttributes(MARGIN_PROPERTY, true)
                            .createChild(ITEMS)
                            .createChild("column", "granite/ui/components/coral/foundation/container");
                    tab.getChildren().forEach(rb::withChild);
                    rb.up(5);
                });
                return rb.build();
            } else {
                return items;
            }
        }

        /**
         * @param orientation the orientation to set
         */
        public void setOrientation(String orientation) {
            this.orientation = orientation;
        }

        /**
         * @return the size
         */
        public String getSize() {
            return size;
        }

        /**
         * @param size the size to set
         */
        public void setSize(String size) {
            this.size = size;
        }

        /**
         * @return the orientation
         */
        public String getOrientation() {
            return orientation;
        }
    }

    public static class AccordionComponent extends AbstractGroupingContainerComponent {
        // See also: https://helpx.adobe.com/experience-manager/6-3/sites/developing/using/reference-materials/granite-ui/api/jcr_root/libs/granite/ui/components/coral/foundation/accordion/index.html

        private String variant = "default";
        private boolean multiple = false;

        public AccordionComponent() {
            setLayout("granite/ui/components/coral/foundation/accordion");
        }

        @Override
        public Resource buildComponentResource() {
            getComponentMetadata().put("variant", getVariant());
            getComponentMetadata().put("multiple", isMultiple());
            return super.buildComponentResource();
        }

        /**
         * @return the variant
         */
        public String getVariant() {
            return variant;
        }

        /**
         * @param variant the variant to set
         */
        public void setVariant(String variant) {
            this.variant = variant;
        }

        /**
         * @return the multiple
         */
        public boolean isMultiple() {
            return multiple;
        }

        /**
         * @param multiple the multiple to set
         */
        public void setMultiple(boolean multiple) {
            this.multiple = multiple;
        }
    }

    /**
     * @return the layout
     */
    public String getLayout() {
        return layout;
    }

    /**
     * @param layout the layout to set
     */
    public void setLayout(String layout) {
        this.layout = layout;
    }

    /**
     * @return the margin
     */
    public boolean isMargin() {
        return margin;
    }

    /**
     * @param margin the margin to set
     */
    public void setMargin(boolean margin) {
        this.margin = margin;
    }
}
