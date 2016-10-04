<%@ page import="javax.jcr.Session" %>
<%@ page import="com.day.cq.commons.jcr.JcrUtil" %>
<%@page session="false"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>

<sling:defineObjects />

<%
    String dialogSharedDataPath = slingRequest.getRequestPathInfo().getSuffix();
    Session dialogSharedSession = resourceResolver.adaptTo(Session.class);

    // Ensure the path exists in the JCR so that we don't get a NPE
    // when attempting to load the dialog.
    JcrUtil.createPath(dialogSharedDataPath, "nt:unstructured", "nt:unstructured", dialogSharedSession, false);
%>

<%@include file="/libs/cq/gui/components/authoring/dialog/dialog.jsp" %>
