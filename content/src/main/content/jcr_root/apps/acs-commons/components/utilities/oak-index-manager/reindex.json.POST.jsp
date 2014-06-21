<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="org.apache.commons.lang.StringUtils,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ModifiableValueMap"%><%

    final String[] names = slingRequest.getParameterValues("name");

    if(names == null) {
        slingResponse.sendError(500);
    } else {
        for(final String name : names) {
            final Resource indexResource = resourceResolver.getResource("/oak:index/" + name);
            if(indexResource == null) {
                slingResponse.sendError(500);
            }

            final ModifiableValueMap mvp = indexResource.adaptTo(ModifiableValueMap.class);

            mvp.put("reindex", true);
        }
        resourceResolver.commit();
    }
%>
