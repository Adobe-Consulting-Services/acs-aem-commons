/*
 * Copyright 2019 Adobe.
 *
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
 */
package com.adobe.acs.commons.mcp.form;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;

/**
 * Represent a button on a form
 */
public class ButtonComponent extends FieldComponent {
    private static final String ACTION_CONFIG_NAME = "actionConfigName";
    private static final String ACTIVE = "active";
    private static final String AUTOCOMPLETE = "autocomplete";
    private static final String BLOCK = "block";
    private static final String COMMAND = "command";
    private static final String COMMENT = "text_commentI18n";
    private static final String DISABLED = "disabled";
    private static final String FORM_ID = "formId";
    private static final String HIDE_TEXT = "hideText";
    private static final String ICON = "icon";
    private static final String ICON_SIZE = "iconSize";
    private static final String TEXT = "text";
    private static final String TRACKING_FEATURE = "trackingFeature";
    private static final String TRACKING_ELEMENT = "trackingElement";
    private static final String TYPE = "type";
    private static final String VARIANT = "variant";

    private String actionConfigName = "";
    private boolean active = true;
    private String autocomplete = "off";
    private boolean block = false;
    private String command = "";
    private String comment = "";
    private boolean disabled = false;
    private String formId = "";
    private boolean hideText = false;
    private String icon = "";
    private String iconSize = "S";
    private String text = "";
    private String trackingFeature = "";
    private String trackingElement = "";
    private String type = "button";
    private String variant = "primary";

    public ButtonComponent() {
        setResourceType("granite/ui/components/coral/foundation/button");
    }

    @Override
    public void init() {
        getOption(ACTION_CONFIG_NAME).ifPresent(this::setActionConfigName);
        getBooleanOption(ACTIVE).ifPresent(this::setActive);
        getOption(AUTOCOMPLETE).ifPresent(this::setAutocomplete);
        getBooleanOption(BLOCK).ifPresent(this::setBlock);
        getOption(COMMAND).ifPresent(this::setCommand);
        getOption(COMMENT).ifPresent(this::setComment);
        getBooleanOption(DISABLED).ifPresent(this::setDisabled);
        getOption(FORM_ID).ifPresent(this::setFormId);
        getBooleanOption(HIDE_TEXT).ifPresent(this::setHideText);
        getOption(ICON).ifPresent(this::setIcon);
        getOption(ICON_SIZE).ifPresent(this::setIconSize);
        getOption(TEXT).ifPresent(this::setText);
        getOption(TRACKING_ELEMENT).ifPresent(this::setTrackingElement);
        getOption(TRACKING_FEATURE).ifPresent(this::setTrackingFeature);
        getOption(TYPE).ifPresent(this::setType);
        getOption(VARIANT).ifPresent(this::setVariant);
    }

    @Override
    public Resource buildComponentResource() {
        ResourceMetadata meta = getComponentMetadata();
        meta.put(ACTION_CONFIG_NAME, getActionConfigName());
        meta.put(ACTIVE, isActive());
        meta.put(AUTOCOMPLETE, getAutocomplete());
        meta.put(BLOCK, isBlock());
        meta.put(COMMAND, getCommand());
        meta.put(COMMENT, getComment());
        meta.put(DISABLED, isDisabled());
        meta.put(FORM_ID, getFormId());
        meta.put(HIDE_TEXT, isHideText());
        meta.put(ICON, getIcon());
        meta.put(ICON_SIZE, getIconSize());
        meta.put(TEXT, getText());
        meta.put(TRACKING_ELEMENT, getTrackingElement());
        meta.put(TRACKING_FEATURE, getTrackingFeature());
        meta.put(TYPE, getType());
        meta.put(VARIANT, getVariant());
        return super.buildComponentResource();
    }

    /**
     * @return the actionConfigName
     */
    public String getActionConfigName() {
        return actionConfigName;
    }

    /**
     * @param actionConfigName the actionConfigName to set
     */
    public void setActionConfigName(String actionConfigName) {
        this.actionConfigName = actionConfigName;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the autocomplete
     */
    public String getAutocomplete() {
        return autocomplete;
    }

    /**
     * @param autocomplete the autocomplete to set
     */
    public void setAutocomplete(String autocomplete) {
        this.autocomplete = autocomplete;
    }

    /**
     * @return the block
     */
    public boolean isBlock() {
        return block;
    }

    /**
     * @param block the block to set
     */
    public void setBlock(boolean block) {
        this.block = block;
    }

    /**
     * @return the command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @param command the command to set
     */
    public void setCommand(String command) {
        this.command = command;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @param comment the comment to set
     */
    public void setComment(String comment) {
        this.comment = comment;
    }

    /**
     * @return the disabled
     */
    public boolean isDisabled() {
        return disabled;
    }

    /**
     * @param disabled the disabled to set
     */
    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    /**
     * @return the formId
     */
    public String getFormId() {
        return formId;
    }

    /**
     * @param formId the formId to set
     */
    public void setFormId(String formId) {
        this.formId = formId;
    }

    /**
     * @return the hideText
     */
    public boolean isHideText() {
        return hideText;
    }

    /**
     * @param hideText the hideText to set
     */
    public void setHideText(boolean hideText) {
        this.hideText = hideText;
    }

    /**
     * @return the icon
     */
    public String getIcon() {
        return icon;
    }

    /**
     * @param icon the icon to set
     */
    public void setIcon(String icon) {
        this.icon = icon;
    }

    /**
     * @return the iconSize
     */
    public String getIconSize() {
        return iconSize;
    }

    /**
     * @param iconSize the iconSize to set
     */
    public void setIconSize(String iconSize) {
        this.iconSize = iconSize;
    }

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return the trackingFeature
     */
    public String getTrackingFeature() {
        return trackingFeature;
    }

    /**
     * @param trackingFeature the trackingFeature to set
     */
    public void setTrackingFeature(String trackingFeature) {
        this.trackingFeature = trackingFeature;
    }

    /**
     * @return the trackingElement
     */
    public String getTrackingElement() {
        return trackingElement;
    }

    /**
     * @param trackingElement the trackingElement to set
     */
    public void setTrackingElement(String trackingElement) {
        this.trackingElement = trackingElement;
    }

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
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
}