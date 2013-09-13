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
