<%-- 
    Document   : process-manager
    Created on : Mar 29, 2017, 5:30:49 PM
    Author     : brobert
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %>
<cq:includeClientLib css="acs-commons.manage-controlled-processes.app"/>
<cq:includeClientLib categories="coralui3,coralui2,cq.authoring.dialog,granite.ui.coral.foundation"/>
<section style="width:50%; margin: 5px;" class="coral-Well" id="processManager">
    <form class="coral-Form coral-Form--vertical">
        <section class="coral-Form-fieldset">
            <div class="coral-Form-fieldwrapper">
                <label class="coral-Form-fieldlabel">Source Folder</label>
                <span class="coral-Form-field assetfilter path coral-PathBrowser" data-init="pathbrowser" data-root-path="/content/dam" data-option-loader="granite.ui.pathBrowser.pages.hierarchyNotFile" data-picker-src="/libs/wcm/core/content/common/pathbrowser/column.html/content/dam?predicate=hierarchyNotFile" data-picker-title="Select Path" data-crumb-root="dam" data-picker-multiselect="true" data-root-path-valid-selection="true" style="display: block;">
                    <span class="coral-InputGroup coral-InputGroup--block">
                        <input class="coral-InputGroup-input coral-Textfield js-coral-pathbrowser-input" type="text" id="sourceFolder" name="sourceFolder" autocomplete="off" value="" placeholder="Enter path" data-validation="">
                        <span class="coral-InputGroup-button">
                            <button class="coral-Button coral-Button--square js-coral-pathbrowser-button" type="button" title="Browse"><i class="coral-Icon coral-Icon--sizeS coral-Icon--folderSearch"></i></button>
                        </span>
                    </span>
                </span>
            </div>
            <div class="coral-Form-fieldwrapper">
                <label class="coral-Form-fieldlabel">Destination Folder</label>
                <span class="coral-Form-field assetfilter path coral-PathBrowser" data-init="pathbrowser" data-root-path="/content/dam" data-option-loader="granite.ui.pathBrowser.pages.hierarchyNotFile" data-picker-src="/libs/wcm/core/content/common/pathbrowser/column.html/content/dam?predicate=hierarchyNotFile" data-picker-title="Select Path" data-crumb-root="dam" data-picker-multiselect="false" data-root-path-valid-selection="true" style="display: block;">
                    <span class="coral-InputGroup coral-InputGroup--block">
                        <input class="coral-InputGroup-input coral-Textfield js-coral-pathbrowser-input" type="text" id="destinationFolder" name="destinationFolder" autocomplete="off" value="" placeholder="Enter path" data-validation="">
                        <span class="coral-InputGroup-button">
                            <button class="coral-Button coral-Button--square js-coral-pathbrowser-button" type="button" title="Browse"><i class="coral-Icon coral-Icon--sizeS coral-Icon--folderSearch"></i></button>
                        </span>
                    </span>
                </span>
            </div>
        </section>
        <button coral-multifield-add icon="save" type="button" id="startButton" is="coral-button">Move folder(s)</button>
        <coral-progress id="moveProgress" size="L" labelposition="bottom"></coral-progress>
    </form>
</section>
<cq:includeClientLib js="acs-commons.manage-controlled-processes.app"/>