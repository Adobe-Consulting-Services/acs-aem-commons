<%@include file="/libs/foundation/global.jsp"%><%
%><%@ page session="false"
        contentType="application/json; charset=UTF-8"
        pageEncoding="utf-8"
        import="com.adobe.acs.commons.images.NamedImageTransformer,
                org.apache.commons.lang.StringUtils,
                org.apache.sling.commons.json.JSONArray,
                org.apache.sling.commons.json.JSONObject"%><%
 %><%

    final NamedImageTransformer[] namedImageTransforms = sling.getServices(NamedImageTransformer.class, null);
    final JSONArray jsonArray = new JSONArray();

    for (final NamedImageTransformer transform : namedImageTransforms) {
        final JSONObject json = new JSONObject();

        json.put("text", StringUtils.capitalize(
                                    StringUtils.replace(transform.getTransformName(), "-", " ")));
        json.put("value", transform.getTransformName());

        jsonArray.put(json);
    }

    try {
        slingResponse.getWriter().print(jsonArray.toString());
    } catch(Exception e) {
        slingResponse.setStatus(500);
    }
%>