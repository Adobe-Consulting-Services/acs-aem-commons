<%@ page import="javax.jcr.Session" %>
<%@ page import="com.day.cq.commons.jcr.JcrUtil" %>
<%@page session="false"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %>

<sling:defineObjects />

<%
    String dialogSitewideDataPath = slingRequest.getRequestPathInfo().getSuffix();
    Session dialogSitewideSession = resourceResolver.adaptTo(Session.class);

    // Ensure the path exists in the JCR so that we don't get a NPE
    // when attempting to load the dialog.
    JcrUtil.createPath(dialogSitewideDataPath, "nt:unstructured", "nt:unstructured", dialogSitewideSession, false);
%>

<%@include file="/libs/cq/gui/components/authoring/dialog/dialog.jsp" %>
