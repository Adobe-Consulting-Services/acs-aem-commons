<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 Adobe
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
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="org.apache.commons.lang3.StringEscapeUtils,
    			com.day.cq.wcm.api.WCMMode,
    			com.adobe.acs.commons.designer.DesignHtmlLibraryManager"%><%

    final WCMMode mode = WCMMode.fromRequest(slingRequest);

    String title = currentPage.getTitle();
    if (title == null) {
        title = currentPage.getName();
    }

%>
<% WCMMode.EDIT.toRequest(slingRequest); %>

<!DOCTYPE html>
    <head>
        <title>CQ5 Design | <%= StringEscapeUtils.escapeHtml4(title) %></title>
        <meta http-equiv="Content-Type" content="text/html; utf-8" />

        <cq:includeClientLib categories="cq.wcm.edit,acs-commons.utilities.clientlibsmanager"/>
        <script src="/libs/cq/ui/resources/cq-ui.js" type="text/javascript"></script>
    </head>

    <body>
        <h1><%= StringEscapeUtils.escapeHtml4(title) %></h1>

        <%-- Include ClientLibs Manager Component --%>
        <cq:include path="<%= DesignHtmlLibraryManager.RESOURCE_NAME %>"  resourceType="acs-commons/components/utilities/designer/clientlibsmanager"/>

    </body>
</html>

<% mode.toRequest(slingRequest); %>
