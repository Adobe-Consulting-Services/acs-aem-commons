<%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
           import="com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ExpressionResolver,
                  com.adobe.acs.commons.granite.ui.components.include.NamespaceResourceWrapper" %><%


    Config cfg = cmp.getConfig();

    String path = cfg.get("path", String.class);

    if (path == null) {
        return;
    }

    // Get the resource using resourceResolver so that the search path is applied.
    Resource targetResource = resourceResolver.getResource(path);

    if (targetResource == null) {
        return;
    }

    NamespaceResourceWrapper resourceWrapper = new NamespaceResourceWrapper(targetResource, sling.getService(ExpressionResolver.class), slingRequest);

    cmp.include(resourceWrapper, cfg.get("resourceType", String.class), cmp.getOptions());
%>