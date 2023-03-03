/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
import org.osgi.annotation.versioning.ProviderType;

import java.util.Map;

/**
 * Represent a button on a form
 */
@ProviderType
public final class ButtonComponent extends FieldComponent {
    private static final String ACTION_CONFIG_NAME_OPT = "actionConfigName";
    private static final String ACTIVE_OPT = "active";
    private static final String AUTOCOMPLETE_OPT = "autocomplete";
    private static final String BLOCK_OPT = "block";
    private static final String COMMAND_OPT = "command";
    private static final String COMMENT_OPT = "text_commentI18n";
    private static final String DISABLED_OPT = "disabled";
    private static final String FORM_ID_OPT = "formId";
    private static final String HIDE_TEXT_OPT = "hideText";
    private static final String ICON_OPT = "icon";
    private static final String ICON_SIZE_OPT = "iconSize";
    private static final String TEXT_OPT = "text";
    private static final String TRACKING_FEATURE_OPT = "trackingFeature";
    private static final String TRACKING_ELEMENT_OPT = "trackingElement";
    private static final String TYPE_OPT = "type";
    private static final String VARIANT_OPT = "variant";

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
        getOption(ACTION_CONFIG_NAME_OPT).ifPresent(this::setActionConfigName);
        getBooleanOption(ACTIVE_OPT).ifPresent(this::setActive);
        getOption(AUTOCOMPLETE_OPT).ifPresent(this::setAutocomplete);
        getBooleanOption(BLOCK_OPT).ifPresent(this::setBlock);
        getOption(COMMAND_OPT).ifPresent(this::setCommand);
        getOption(COMMENT_OPT).ifPresent(this::setComment);
        getBooleanOption(DISABLED_OPT).ifPresent(this::setDisabled);
        getOption(FORM_ID_OPT).ifPresent(this::setFormId);
        getBooleanOption(HIDE_TEXT_OPT).ifPresent(this::setHideText);
        getOption(ICON_OPT).ifPresent(this::setIcon);
        getOption(ICON_SIZE_OPT).ifPresent(this::setIconSize);
        getOption(TEXT_OPT).ifPresent(this::setText);
        getOption(TRACKING_ELEMENT_OPT).ifPresent(this::setTrackingElement);
        getOption(TRACKING_FEATURE_OPT).ifPresent(this::setTrackingFeature);
        getOption(TYPE_OPT).ifPresent(this::setType);
        getOption(VARIANT_OPT).ifPresent(this::setVariant);
    }

    @Override
    public Resource buildComponentResource() {
        Map<String, Object> properties = getProperties();
        properties.put(ACTION_CONFIG_NAME_OPT, getActionConfigName());
        properties.put(ACTIVE_OPT, isActive());
        properties.put(AUTOCOMPLETE_OPT, getAutocomplete());
        properties.put(BLOCK_OPT, isBlock());
        properties.put(COMMAND_OPT, getCommand());
        properties.put(COMMENT_OPT, getComment());
        properties.put(DISABLED_OPT, isDisabled());
        properties.put(FORM_ID_OPT, getFormId());
        properties.put(HIDE_TEXT_OPT, isHideText());
        properties.put(ICON_OPT, getIcon());
        properties.put(ICON_SIZE_OPT, getIconSize());
        properties.put(TEXT_OPT, getText());
        properties.put(TRACKING_ELEMENT_OPT, getTrackingElement());
        properties.put(TRACKING_FEATURE_OPT, getTrackingFeature());
        properties.put(TYPE_OPT, getType());
        properties.put(VARIANT_OPT, getVariant());
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