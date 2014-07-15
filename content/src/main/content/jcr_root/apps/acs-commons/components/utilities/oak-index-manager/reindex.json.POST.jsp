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
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="application/json; charset=UTF-8" pageEncoding="utf-8"
          import="org.apache.commons.lang.StringUtils,
                org.apache.sling.commons.json.JSONObject,
                org.apache.sling.commons.json .JSONArray,
                org.apache.sling.api.SlingHttpServletResponse,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ModifiableValueMap"%><%

    final JSONObject json = new JSONObject();
    final JSONArray successArray = new JSONArray();
    final JSONArray errorArray = new JSONArray();
    final String[] names = slingRequest.getParameterValues("name");

    if(names == null) {
        slingResponse.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } else {
        for(final String name : names) {
            final Resource indexResource = resourceResolver.getResource("/oak:index/" + name);

            if(indexResource == null) {
                errorArray.put(name);
                continue;
            }

            final ModifiableValueMap mvp = indexResource.adaptTo(ModifiableValueMap.class);

            mvp.put("reindex", true);
            successArray.put(name);
        }
        resourceResolver.commit();
    }

    if(errorArray.length() > 0) {
        response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
    } else {
        json.put("success", successArray);
        json.put("error", errorArray);
        response.getWriter().print(json.toString(2));
    }
%>
