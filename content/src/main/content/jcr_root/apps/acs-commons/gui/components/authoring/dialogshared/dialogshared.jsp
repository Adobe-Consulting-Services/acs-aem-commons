<%@ page session="false"
         import="javax.jcr.Session,
                 org.apache.jackrabbit.JcrConstants,
                 com.day.cq.commons.jcr.JcrUtil" %><%
%><%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling" %><%

%><sling:defineObjects /><%

    String dialogSharedDataPath = slingRequest.getRequestPathInfo().getSuffix();
    Session dialogSharedSession = resourceResolver.adaptTo(Session.class);

    /**
     * This is an unusual case where we must write a node on a read (GET) to ensure that the the OOTB dialog
     * implementation does not NPE.
     *
     * SharedComponentPropsPageInfoProvider checks if the current user has permissions to write that node, and if not
     * then flags the shared component properties feature as disabled, which in turn makes it so the UI does not give
     * the user the buttons to configure shared/global configs (and thus they will not hit this code).
     **/

    if (!dialogSharedSession.nodeExists(dialogSharedDataPath)) {
        JcrUtil.createPath(dialogSharedDataPath,  JcrConstants.NT_UNSTRUCTURED, JcrConstants.NT_UNSTRUCTURED, dialogSharedSession, false);
        dialogSharedSession.save();
    }

%><%@include file="/libs/cq/gui/components/authoring/dialog/dialog.jsp" %>