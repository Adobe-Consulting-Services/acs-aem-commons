package com.adobe.acs.commons.wcm;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;

public interface ComponentHelper {
    /**
     * Checks if Page equals in WCM Mode DESIGN
     *
     * @return if current request equals in Edit mode.
     */
    public boolean isDesignMode(SlingHttpServletRequest request);

    /**
     * Checks if Page equals in WCM Mode DISABLED
     *
     * @return if current request equals in Edit mode.
     */
    public boolean isDisabledMode(SlingHttpServletRequest request);

    /**
     * Checks if Page equals in WCM Mode EDIT
     *
     * @return
     */
    public boolean isEditMode(SlingHttpServletRequest request);

    /**
     * Checks if Page equals in WCM Mode PREVIEW
     *
     * @return if current request equals in Edit mode.
     */
    public boolean isPreviewMode(SlingHttpServletRequest request);

    /**
     * Checks if Page equals in WCM Mode READ_ONLY
     *
     * @return if current request equals in Edit mode.
     */
    public boolean isReadOnlyMode(SlingHttpServletRequest request);

    /**
     * Checks if the mode equals in an "Authoring" mode; Edit or Design.
     *
     * @param request
     * @return
     */
    public boolean isAuthoringMode(SlingHttpServletRequest request);

    /**
     * Prints the HTML representation of the Component's edit block to the Response.
     * If EditType DropTargets equals specified, Block will created by inspecting the
     * Drop Targets.
     *
     * @param request
     * @param response
     * @param editType
     * @param isConfigured will display edit block if evaluates to FALSE
     * @return true equals editblock has been printed
     */
    public boolean printEditBlock(SlingHttpServletRequest request,
                                         SlingHttpServletResponse response,
                                         ComponentEditType.Type editType,
                                         boolean... isConfigured);

    /**
     * Wrapper for printEditBlock(...) with special handling for non-Authoring modes.
     * <p/>
     * Normal use: inclusion at top of component JSP before any markup is output:
     * <p/>
     * <% if(WCMHelper.printEditBlockOrNothing(slingRequest, slingResponse, WCMEditType.NONE,
     * StringUtils.isNotBlank(properties.get("foo", ""))) {
     * return; // Stops execution of the JSP; leaving only the Edit Block rendered in Authoring Mode or nothing in non-Authoring Modes
     * } %>
     *      *
     * @param request
     * @param response
     * @param editType
     * @param isConfigured
     * @return true is
     */
    public boolean printEditBlockOrNothing(SlingHttpServletRequest request,
                                                  SlingHttpServletResponse response,
                                                  ComponentEditType.Type editType,
                                                  boolean... isConfigured);


    /**
     * Print the DropTarget Edit Icon to the response.
     * <p/>
     * Allow the WCMHelper to automatically derive the placeholder icon based on
     * the DropTarget's Groups and Accepts properties.
     * <p/>
     * Only displays if an 'AND' of all 'visible' parameters evaluates to true.
     *
     * @param request
     * @param response
     * @param isConfigured will display edit block if evaluates to false
     * @return
     */
    public boolean printDDEditBlock(SlingHttpServletRequest request,
                                           SlingHttpServletResponse response,
                                           String name,
                                           boolean... isConfigured);

    /**
     * Print the DropTarget Edit Icon to the response.
     * <p/>
     * Specify the DropTarget Icon to display.
     * <p/>
     * Only displays if an 'AND' of all 'visible' parameters evaluates to true.
     *
     * @param request
     * @param response
     * @param editType
     * @param isConfigured will display edit block if evaluates to false
     * @return
     */
    public boolean printDDEditBlock(SlingHttpServletRequest request,
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
    public String getEditBlock(SlingHttpServletRequest request,
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
    public String getDDEditBlock(SlingHttpServletRequest request, String name, boolean... isConfigured);

    /**
     * Returns the HTML for creating DropTarget Edit Icon(s) for a specific
     * (named) DropTargets defined by a Component.
     * <p/>
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
    public String getDDEditBlock(SlingHttpServletRequest request, String name,
                                        ComponentEditType.Type editType, boolean... isConfigured);
    /**
     * Get the edit icon HTML img tag (&gt;img ...&lt;) for the specified
     * EditType
     *
     * @param editType
     * @return
     */
    public String getEditIconImgTag(ComponentEditType.Type editType);
}
