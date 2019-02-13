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
package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.wcm.ComponentEditType;
import com.adobe.acs.commons.wcm.ComponentHelper;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.ComponentEditConfig;
import com.day.cq.wcm.api.components.DropTarget;
import com.day.cq.wcm.commons.WCMUtils;
import com.day.cq.wcm.foundation.Placeholder;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletRequest;

/**
 * Component Helper is an OSGi Service used in the context of CQ Components
 * for encapsulating common tasks and performing common checks.
 *
 * Get using @Reference annotation or via SlingScriptHelper's .getService(..) method
 */
@Component(label = "ACS AEM Commons - Component Helper",
        description = "Component Helper is a service used in the context of CQ Components for "
                + "encapsulating common tasks and performing common checks.")
@Service
@SuppressWarnings({"checkstyle:abbreviationaswordinname", "squid:S1192"})
public final class ComponentHelperImpl implements ComponentHelper {
    private static final String CSS_EDIT_MODE = "wcm-helper-edit-mode";

    public String generateClassicUIPlaceholder(String classNames, String title) {
        StringBuilder html = new StringBuilder();
        // Create the HTML img tag used for the edit icon
        html.append("<img src=\"/libs/cq/ui/resources/0.gif\" ");
        html.append(" class=\"").append(classNames);
        html.append("\" ");
        if (StringUtils.isNotBlank(title)) {
            html.append("alt=\"").append(title).append("\" ");
            html.append("title=\"").append(title).append("\"");
        }
        html.append("/>");
        return html.toString();
    }

    public boolean isDesignMode(SlingHttpServletRequest request) {
        return WCMMode.DESIGN.equals(WCMMode.fromRequest(request));
    }

    public boolean isDisabledMode(SlingHttpServletRequest request) {
        return WCMMode.DISABLED.equals(WCMMode.fromRequest(request));
    }

    public boolean isEditMode(SlingHttpServletRequest request) {
        return WCMMode.EDIT.equals(WCMMode.fromRequest(request));
    }

    public boolean isPreviewMode(SlingHttpServletRequest request) {
        return WCMMode.PREVIEW.equals(WCMMode.fromRequest(request));
    }

    public boolean isReadOnlyMode(SlingHttpServletRequest request) {
        return WCMMode.READ_ONLY.equals(WCMMode.fromRequest(request));
    }

    public boolean isAuthoringMode(SlingHttpServletRequest request) {
        return (isEditMode(request) || isDesignMode(request));
    }

    public boolean isTouchAuthoringMode(ServletRequest request) {
       return Placeholder.isAuthoringUIModeTouch(request);
    }

    public boolean printEditBlock(SlingHttpServletRequest request,
                                         SlingHttpServletResponse response,
                                         ComponentEditType.Type editType,
                                         boolean... isConfigured) {

        final String html = getEditBlock(request, editType, isConfigured);

        if (html == null) {
            return false;
        }

        try {
            response.getWriter().print(html);
            response.getWriter().flush();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    public boolean printEditBlockOrNothing(SlingHttpServletRequest request,
                                          SlingHttpServletResponse response,
                                          ComponentEditType.Type editType,
                                          boolean... isConfigured) {
        if (isAuthoringMode(request)) {
            return printEditBlock(request, response, editType, isConfigured);
        } else {
            // If NOT authoring mode (Preview or Publish)
            return !conditionAndCheck(isConfigured);
        }
    }

    public boolean printDDEditBlock(SlingHttpServletRequest request,
                                           SlingHttpServletResponse response,
                                           String name,
                                           boolean... isConfigured) {

        return printDDEditBlock(request, response, name, null, isConfigured);
    }

    public boolean printDDEditBlock(SlingHttpServletRequest request,
                                           SlingHttpServletResponse response,
                                           String name,
                                           ComponentEditType.Type editType,
                                           boolean... isConfigured) {

        final String html = getDDEditBlock(request, name, editType, isConfigured);

        if (html == null) {
            return false;
        }

        try {
            response.getWriter().print(html);
            response.getWriter().flush();
        } catch (IOException e) {
            return false;
        }

        return true;
    }


    @SuppressWarnings("squid:S3776")
    public String getEditBlock(SlingHttpServletRequest request,
                                      ComponentEditType.Type editType,
                                      boolean... isConfigured) {

        final Resource resource = request.getResource();
        final com.day.cq.wcm.api.components.Component component = WCMUtils.getComponent(resource);
        if (!isAuthoringMode(request)
                || conditionAndCheck(isConfigured)) {
            return null;
        } else if (ComponentEditType.NONE.equals(editType)) {
            return "<!-- Edit Mode Placeholder is specified as: " + editType.getName() + " -->";
        }

        StringBuilder html = new StringBuilder("<div class=\"wcm-edit-mode " + CSS_EDIT_MODE + "\">");

        if (component == null) {
            html.append(getCssStyle());
            html.append("Could not resolve CQ Component type.");
        } else if (ComponentEditType.NOICON.equals(editType) || ComponentEditType.NONE.equals(editType)) {
            final String title = StringUtils.capitalize(component.getTitle());

            html.append(getCssStyle());
            html.append("<dl>");
            html.append("<dt>" + title + " Component</dt>");

            if (component.isEditable()) {
                html.append("<dd>Double click or Right click to Edit</dd>");
            }

            if (component.isDesignable()) {
                html.append("<dd>Switch to Design mode and click the Edit button</dd>");
            }

            if (!component.isEditable() && !component.isDesignable()) {
                html.append("<dd>The component cannot be directly authored</dd>");
            }

            html.append("</dl>");
        } else if (ComponentEditType.DROPTARGETS.equals(editType)) {
            // Use DropTargets
            ComponentEditConfig editConfig = component.getEditConfig();
            Map<String, DropTarget> dropTargets = (editConfig != null) ? editConfig.getDropTargets() : null;

            if (dropTargets != null && !dropTargets.isEmpty()) {
                // Auto generate images with drop-targets
                for (final Map.Entry<String, DropTarget> entry : dropTargets.entrySet()) {
                    final DropTarget dropTarget = entry.getValue();

                    html.append("<img src=\"/libs/cq/ui/resources/0.gif\" ");
                    html.append("class=\"").append(dropTarget.getId());
                    html.append(" ").append(getWCMEditType(dropTarget).getCssClass()).append("\" ");
                    html.append("alt=\"Drop Target: ").append(dropTarget.getName()).append("\" ");
                    html.append("title=\"Drop Target: ").append(dropTarget.getName()).append("\"/>");
                }
            }
        } else {
            final String title = StringUtils.capitalize(component.getTitle());

            // Use specified EditType
            html.append("<img src=\"/libs/cq/ui/resources/0.gif\" ");
            html.append("class=\"").append(editType.getCssClass()).append("\" alt=\"");
            html.append(title).append("\" ");
            html.append("title=\"").append(title).append("\"/>");
        }

        html.append("</div>");

        return html.toString();
    }

    public String getDDEditBlock(SlingHttpServletRequest request, String name, boolean... isConfigured) {
        return getDDEditBlock(request, name, null, isConfigured);
    }

    @SuppressWarnings("squid:S3776")
    public String getDDEditBlock(SlingHttpServletRequest request, String name,
            ComponentEditType.Type editType, boolean... isConfigured) {
        if (!isAuthoringMode(request) || conditionAndCheck(isConfigured)) {
            return null;
        }

        final Resource resource = request.getResource();
        final com.day.cq.wcm.api.components.Component component = WCMUtils.getComponent(resource);

        StringBuilder html = new StringBuilder();

        ComponentEditConfig editConfig = component.getEditConfig();
        Map<String, DropTarget> dropTargets = (editConfig != null) ? editConfig.getDropTargets() : null;

        if (dropTargets != null && !dropTargets.isEmpty()) {
            DropTarget dropTarget = null;

            // Find the named Drop Target
            for (final Map.Entry<String, DropTarget> entry : dropTargets.entrySet()) {
                dropTarget = entry.getValue();
                if (StringUtils.equals(name, dropTarget.getName())) {
                    break;
                } else {
                    dropTarget = null;
                }
            }

            if (dropTarget != null) {
                // If editType has not been specified then intelligently determine the best match
                editType = (editType == null) ? getWCMEditType(dropTarget) : editType;

                String classNames = dropTarget.getId() + " " + editType.getCssClass();
                String placeholderTitle = "Drop Target: " + dropTarget.getName();

                html.append(generateClassicUIPlaceholder(classNames, placeholderTitle));
            }
        }

        return html.toString();
    }

    public String getEditIconImgTag(ComponentEditType.Type editType) {
        final String title = StringUtils.capitalize(editType.getName());

        return "<img src=\"/libs/cq/ui/resources/0.gif\"" + " "
                + "class=\"" + editType.getCssClass() + "\""
                + " " + "alt=\"" + title + "\" "
                + "title=\"" + title + "\" />";
    }

    /**
     * "Intelligently" determines the WCMEditType to use based on the
     * DropTarget.
     * <p>
     * Inspects the DropTarget's Groups and Accepts to make this determination.
     * <p>
     * If no match can be found, defaults to TEXT
     *
     * @param dropTarget the drop target
     * @return the component edit type
     */
    private ComponentEditType.Type getWCMEditType(DropTarget dropTarget) {
        if (dropTarget == null) {
            return ComponentEditType.NONE;
        }
        List<String> groups = Arrays.asList(dropTarget.getGroups());
        List<String> accepts = Arrays.asList(dropTarget.getAccept());

        if (groups.isEmpty() && accepts.isEmpty()) {
            return ComponentEditType.NONE;
        }

        if (groups.contains("media")) {
            if (matches(accepts, "image")) {
                return ComponentEditType.IMAGE;
            } else if (matches(accepts, "video")) {
                return ComponentEditType.VIDEO;
            } else if (matches(accepts, "flash")) {
                return ComponentEditType.FLASH;
            } else if (accepts.size() == 1 && ".*".equals(accepts.get(0))) {
                return ComponentEditType.FILE;
            }
        } else if (groups.contains("page")) {
            return ComponentEditType.REFERENCE;
        } else if (groups.contains("paragraph")) {
            return ComponentEditType.REFERENCE;
        }

        return ComponentEditType.TEXT;
    }

    private boolean matches(List<String> list, String pattern) {
        if (StringUtils.isBlank(pattern)) {
            return false;
        }

        for (String item : list) {
            if (item == null) {
                continue;
            } else if (item.contains(pattern)) {
                return true;
            }
        }

        return false;
    }

    private boolean conditionAndCheck(boolean... conditions) {
        if (conditions == null) {
            return false;
        }

        for (int i = 0; i < conditions.length; i++) {
            if (!conditions[i]) {
                return false;
            }
        }

        return true;
    }

    /**
     * Inline CSS style used for edit blocks. Ideally this would be a external
     * CSS however for portability this exist in this Java class.
     *
     * @return inline CSS style declaration
     */
    private String getCssStyle() {
        String css = "";
        css += "<style>";
        css += "." + CSS_EDIT_MODE + " { border:dashed 2px #ccc; color:#ccc; padding:1em; }";
        css += "." + CSS_EDIT_MODE + " dt { font-weight: bold; }";
        css += "." + CSS_EDIT_MODE + " dd { display:list-item; list-style-type:disc; margin-left:1.5em; }";
        css += "</style>";
        return css;
    }
}