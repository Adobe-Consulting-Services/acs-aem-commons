<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="org.apache.commons.lang.StringUtils"%><%

    final String name = slingRequest.getParameter("name");

    if(StringUtils.isBlank(name)) {
        slingResponse.sendError(500);
    } else {
        sling.forward("/oak:index/" + name + ".1.json");
    }
%>
