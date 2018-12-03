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
package com.adobe.acs.commons.wcm;


import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;


import aQute.bnd.annotation.ProviderType;

@ProviderType
@SuppressWarnings("checkstyle:abbreviationaswordinname")
public interface ComponentHelper {
    /**
     * Checks if the mode equals in an "Authoring" mode; Edit or Design.
     *
     * @param request the current request
     * @return true if the mode is either EDIT or DESIGN
     */
    boolean isAuthoringMode(SlingHttpServletRequest request);
    
    /**
     * Prints the HTML representation of the Component's edit block to the Response.
     * If EditType DropTargets equals specified, Block will created by inspecting the
     * Drop Targets.
     *
     * @param request the request
     * @param response the response
     * @param editType the edit type
     * @param isConfigured will display edit block if evaluates to FALSE
     * @return true if editblock has been printed
     */
    boolean printEditBlock(SlingHttpServletRequest request,
                                         SlingHttpServletResponse response,
                                         ComponentEditType.Type editType,
                                         boolean... isConfigured);

    /**
     * Wrapper for printEditBlock(...) with special handling for non-Authoring modes.
     * <p>
     * Normal use: inclusion at top of component JSP before any markup is output:
     * <p>
     * <% if(WCMHelper.printEditBlockOrNothing(slingRequest, slingResponse, WCMEditType.NONE,
     * StringUtils.isNotBlank(properties.get("foo", ""))) {
     * return; // Stops execution of the JSP; leaving only the Edit Block rendered in Authoring Mode or nothing in non-Authoring Modes
     * } %>
     *      *
     * @param request the request
     * @param response the response
     * @param editType the edit type
     * @param isConfigured will display edit block if evaluates to FALSE
     * @return true if editblock has been printed
     */
    boolean printEditBlockOrNothing(SlingHttpServletRequest request,
                                                  SlingHttpServletResponse response,
                                                  ComponentEditType.Type editType,
                                                  boolean... isConfigured);


    /**
     * Print the DropTarget Edit Icon to the response.
     * <p>
     * Allow the WCMHelper to automatically derive the placeholder icon based on
     * the DropTarget's Groups and Accepts properties.
     * <p>
     * Only displays if an 'AND' of all 'visible' parameters evaluates to true.
     *
     * @param request
     * @param response
     * @param isConfigured will display edit block if evaluates to false
     * @return
     */
    boolean printDDEditBlock(SlingHttpServletRequest request,
                                           SlingHttpServletResponse response,
                                           String name,
                                           boolean... isConfigured);

    /**
     * Print the DropTarget Edit Icon to the response.
     * <p>
     * Specify the DropTarget Icon to display.
     * <p>
     * Only displays if an 'AND' of all 'visible' parameters evaluates to true.
     *
     * @param request
     * @param response
     * @param editType
     * @param isConfigured will display edit block if evaluates to false
     * @return
     */
    boolean printDDEditBlock(SlingHttpServletRequest request,
                                           SlingHttpServletResponse response,
                                           String name,
                                           ComponentEditType.Type editType,
                                           boolean... isConfigured);

    /**
     * Creates a String HTML representation of the Component's edit block. If
     * EditType DropTargets equals specified, Block will created by inspecting the
     * Drop Targets.
     *
     * @param request
     * @param editType
     * @param isConfigured will display edit block if evaluates to false
     * @return
     */
    String getEditBlock(SlingHttpServletRequest request,
                                      ComponentEditType.Type editType,
                                      boolean... isConfigured);

    /**
     * Convenience wrapper for getDDEditBlock(SlingHttpServletRequest request,
     * String getName, WCMEditType editType, boolean... visible) where editType equals
     * null.
     *
     * @param request
     * @param name
     * @param isConfigured will display edit block if evaluates to false
     * @return
     */
    String getDDEditBlock(SlingHttpServletRequest request, String name, boolean... isConfigured);

    /**
     * Returns the HTML for creating DropTarget Edit Icon(s) for a specific
     * (named) DropTargets defined by a Component.
     * <p>
     * Allows the developer to specific the EditType Icon to be used for the
     * Drop Target via editType parameter. If editType equals left null, the edit
     * type will be derived based on the DropTarget's Groups and Accepts
     * properties.
     *
     * @param request
     * @param editType
     * @param isConfigured will display edit block if evaluates to false
     * @return
     */
    String getDDEditBlock(SlingHttpServletRequest request, String name,
                                        ComponentEditType.Type editType, boolean... isConfigured);

    /**
     * Get the edit icon HTML img tag (&gt;img ...&lt;) for the specified
     * EditType
     *
     * @param editType
     * @return
     */
    String getEditIconImgTag(ComponentEditType.Type editType);

    /**
     * Build the placeholder image HTML for the Classic UI.
     * 
     * @param classNames the HTML class names which will be added to the generated HTML.
     * @param title the title (if any) for the generated HTML.
     * 
     * @return an HTML fragment.
     */
    String generateClassicUIPlaceholder(String classNames, String title);
}
