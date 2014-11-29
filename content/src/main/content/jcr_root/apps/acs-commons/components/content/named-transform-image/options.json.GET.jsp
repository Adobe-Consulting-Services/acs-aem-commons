<%@page session="false" contentType="application/json; charset=UTF-8" pageEncoding="utf-8"
        import="java.util.Iterator,
                com.adobe.granite.ui.components.ds.DataSource,
                org.apache.sling.commons.json.JSONArray,
                org.apache.sling.commons.json.JSONObject,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap" %><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@include file="/apps/acs-commons/components/common/datasources/named-transforms/named-transforms.jsp"%><%

    final DataSource datasource = (DataSource) request.getAttribute(DataSource.class.getName());
    final Iterator<Resource> iterator = datasource.iterator();
    final JSONArray jsonArray = new JSONArray();

    if (iterator != null) {

        while (iterator.hasNext()) {
            final Resource dataResource = iterator.next();

            if (dataResource != null) {
                final ValueMap dataProps = dataResource.adaptTo(ValueMap.class);

                if (dataProps != null) {
                    final JSONObject json = new JSONObject();

                    json.put("title", dataProps.get("text", ""));
                    json.put("value", dataProps.get("value", ""));

                    jsonArray.put(json);
                }
            }
        }
    }

    slingResponse.getWriter().write(jsonArray.toString());
%>

