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
<%@include file="/libs/foundation/global.jsp"%>

<%@ page import="java.util.Iterator" %>
<%@ page import="com.day.cq.wcm.foundation.Image" %>
<%@ page import="org.apache.sling.commons.json.JSONArray" %>
<%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %>

<cq:includeClientLib categories="acs-commons.components.simpleslideshow"/>

<%
    Iterator<Resource> children = resource.listChildren();

    if(!children.hasNext()){
%>
        <wcmmode:edit>
            Double - Click to add Images
        </wcmmode:edit>
<%
    }else{
        Resource imagesResource = children.next();
        ValueMap map = imagesResource.adaptTo(ValueMap.class);
        String order = map.get("order", String.class);

        Image img = null; String src = null;
        JSONArray array = new JSONArray(order);
%>
        <div class="image_multifield_slideshow">
<%
        Resource imgResource;

        for(int i = 0; i < array.length(); i++){
            imgResource = imagesResource.getChild(String.valueOf(array.get(i)));

            img = new Image(imgResource);
            img.setItemName(Image.PN_REFERENCE, "imageReference");
            img.setSelector("img");
            img.setAlt(imgResource.getName());

            src = img.getSrc();
%>
            <img src="<%=src%>" <%= ( i == 0) ? "class=\"active\"" : ""%> alt="<%= img.getAlt() %>"/>
<%
        }
%>
        </div>
<%
    }
%>