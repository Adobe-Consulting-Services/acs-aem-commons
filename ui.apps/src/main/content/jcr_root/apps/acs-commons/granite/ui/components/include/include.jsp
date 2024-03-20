<%@ include file="/libs/granite/ui/global.jsp" %><%
%><%@ page session="false"
           import="com.adobe.granite.ui.components.Config,
                  com.adobe.acs.commons.granite.ui.components.NamespacedTransformedResourceProvider" %><%


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

    NamespacedTransformedResourceProvider transformedResourceProvider = sling.getService(NamespacedTransformedResourceProvider.class);
    Resource resourceWrapper = transformedResourceProvider.transformResourceWithNameSpacing(slingRequest, targetResource);

    cmp.include(resourceWrapper, cfg.get("resourceType", String.class), cmp.getOptions());
%>