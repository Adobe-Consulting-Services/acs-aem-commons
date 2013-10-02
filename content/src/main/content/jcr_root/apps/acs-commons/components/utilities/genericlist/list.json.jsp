<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
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