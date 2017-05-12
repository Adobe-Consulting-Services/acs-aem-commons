<%--
  #%L
  ACS AEM Tools Package
  %%
  Copyright (C) 2014 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
--%>
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" %>
<cq:includeClientLib css="acs-commons.manage-controlled-processes.app'}"/>
"/>
<cq:includeClientLib categories="coralui3,coralui2,cq.authoring.dialog,granite.ui.coral.foundation,granite.ui.shell"/>
<coral-shell>
    <coral-shell-header class="coral--dark">
        <coral-shell-header-home  data-globalnav-navigator-main-href="/mnt/overlay/granite/ui/content/shell/globalnav.html">
            <a is="coral-shell-homeanchor" icon="adobeExperienceManagerColor" href="/">Adobe Experience Manager</a>
        </coral-shell-header-home>
        <coral-shell-header-actions>
        </coral-shell-header-actions>
    </coral-shell-header>

    <coral-shell-content role="main">
        <div id="granite-shell-content" class="foundation-layout-panel">
            <div class="foundation-layout-panel-header">
                <div id="granite-shell-actionbar" class="granite-actionbar foundation-collection-actionbar" data-foundation-collection-actionbar-target="#cq-siteadmin-admin-childpages">
                    <div class="granite-actionbar-centerwrapper">
                        <div class="granite-actionbar-center"><span class="granite-title" role="heading" aria-level="1">Manage Controlled Processes</span></div>
                    </div>
                    <div class="granite-actionbar-left"></div>
                    <div class="granite-actionbar-right"></div>
                </div>                    
            </div>
            <div class="foundation-layout-panel-bodywrapper">
                <div class="foundation-layout-panel-body">
                    <div class="foundation-layout-panel-content">
                        <coral-columnview id="mcp-main-menu" style="max-width:175px; position:absolute; left:0; overflow-x: hidden;">
                            <coral-columnview-column>
                                <coral-columnview-column-content>
                                    <coral-columnview-item role="treeitem" variant="drilldown" icon="plug" data-src="block-browser">Blocks</coral-columnview-item>
                                    <coral-columnview-item role="treeitem" variant="drilldown" icon="scribble" data-src="script-editor">Scripts</coral-columnview-item>
                                    <coral-columnview-item role="treeitem" variant="drilldown" icon="pausePlay" data-src="process-manager">Processes</coral-columnview-item>
                                    <coral-columnview-item role="treeitem" variant="drilldown" icon="properties" data-src="settings">Settings</coral-columnview-item>
                                </coral-columnview-column-content>
                            </coral-columnview-column>
                        </coral-columnview>
                        <div id="mcp-workspace" style="position:absolute; left:176px; right:0px; text-align: center;">
                            <h3>Please select from one the options on the left to get started.</h3>
                        </div>                        
                    </div>
                </div>
            </div>
        </div>
    </coral-shell-content>
</coral-shell>
<cq:includeClientLib js="acs-commons.manage-controlled-processes.app"/>