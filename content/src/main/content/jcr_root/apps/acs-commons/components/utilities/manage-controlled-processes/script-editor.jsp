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
<cq:includeClientLib css="acs-commons.manage-controlled-processes.app"/>
<div id="blocklyArea" style="height:90%; min-height: 500px; width:100%">
    <div id="blocklyDiv" style="height:100%; width:100%;"></div>
    <cq:include script="script-editor-toolbar.jsp"/>
</div>
<cq:includeClientLib js="acs-commons.manage-controlled-processes.app"/>