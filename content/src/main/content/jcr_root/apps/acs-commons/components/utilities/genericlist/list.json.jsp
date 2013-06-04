<%@page contentType="application/json" pageEncoding="utf-8" import="java.util.Iterator,org.apache.sling.commons.json.*"%><%
%><%@include file="/libs/foundation/global.jsp"%><%
/* Create a json object of name/value parameters
   for use in select dropdowns
*/

    Resource list = resource.getChild("list");

    Iterator<Resource> listItems = list.listChildren();

    JSONArray jarray = new JSONArray();

    while (listItems.hasNext()) {
        Resource listItem = listItems.next();
        String title = listItem.adaptTo(ValueMap.class).get("jcr:title", String.class);
        String value = listItem.adaptTo(ValueMap.class).get("value", "");
        if (title != null) {
            JSONObject jobject=new JSONObject();
            jobject.put("value",value);
            jobject.put("text",title);
            jarray.put(jobject);
        }
    }
    out.println(jarray.toString());


%>