<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="org.apache.commons.lang3.StringEscapeUtils,
    			com.day.cq.wcm.api.WCMMode,
    			com.adobe.acs.commons.designer.DesignHtmlLibraryManager"%>
<%

    WCMMode mode = WCMMode.fromRequest(slingRequest);

    String title = currentPage.getTitle();
    if (title == null) {
        title = currentPage.getName();
    }

%><% WCMMode.EDIT.toRequest(slingRequest); %>
<!DOCTYPE html>
<html>
<head>
    <title>CQ5 Design | <%= StringEscapeUtils.escapeHtml4(title) %></title>
    <meta http-equiv="Content-Type" content="text/html; utf-8" />

    <cq:include script="headlibs.jsp"/>
    <cq:include script="/libs/wcm/core/components/init/init.jsp"/>
    <script src="/libs/cq/ui/resources/cq-ui.js" type="text/javascript"></script>

    <style>
        h1 {
            border-width: 0;
        }

        h2 {
            margin: 0 0 .5em 0;
            padding-left: 0;
            width: auto;
        }

        em {
            margin: 0;
            padding: 0;
        }

        p {
            margin-left: 1em;
            padding-left: 0;
            width: auto;
        }

        ul {
            list-style-position:inside;
            list-style-type: disc;
            margin-left: 0;
            padding-left: 0;
            width: auto;
        }

        ul li {
            border-width: 0;
            margin-left: 1em;
            width: auto;
        }

        .instructions {
            background-color: #F7F7F7;
            border: solid 1px #CCC;
            margin: 10px 0 0 500px;
            padding: 2em;
            width: 500px;
        }

        .instructions p,
        .instructions ul {
            margin-left: 0;
            padding-left: 0;
        }

        .clientlibs {
            float: left;
            width: 450px;
        }

    </style>
</head>
<body>
<h1><%= StringEscapeUtils.escapeHtml4(title) %></h1>

<cq:include path="<%= DesignHtmlLibraryManager.RESOURCE_NAME %>"  resourceType="acs-commons/components/utilities/designer/clientlibsmanager"/>
</body>
</html>
<% mode.toRequest(slingRequest); %>
