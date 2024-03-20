<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2016 Adobe
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

  @deprecated Use expression language with variable "acsCommonsPageRoot" instead
--%><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false" %><%
%><%@ page import="com.adobe.acs.commons.wcm.PageRootProvider" %><%
%><%@ page import="java.util.Map" %><%
%><%@ page import="java.util.HashMap" %><%
%><%@ page import="org.apache.sling.api.request.RequestDispatcherOptions" %><%
%><%@ page import="org.apache.sling.api.resource.Resource"%><%
%><%@ page import="org.apache.sling.api.resource.ResourceWrapper" %><%
%><%@ page import="org.apache.sling.api.resource.ValueMap" %><%
%><%@ page import="org.apache.sling.api.wrappers.ValueMapDecorator" %><%
%><%
    String contentPath = slingRequest.getRequestPathInfo().getSuffix();

    String rootPath = null;
    PageRootProvider pageRootProvider = sling.getService(PageRootProvider.class);
    if (pageRootProvider != null) {
        rootPath = pageRootProvider.getRootPagePath(contentPath);
    }

    Map<String, Object> mergedProperties = new HashMap<String, Object>();
    mergedProperties.putAll(resource.getValueMap());
    mergedProperties.put("rootPath", rootPath != null ? rootPath : "/");
    final ValueMap newValueMap = new ValueMapDecorator(mergedProperties);

    Resource resourceWrapper = new ResourceWrapper(resource) {
        @Override
        public ValueMap getValueMap() {
            return newValueMap;
        }
    };

    RequestDispatcherOptions options = new RequestDispatcherOptions();
    options.setForceResourceType("/libs/granite/ui/components/foundation/form/pathbrowser");
    RequestDispatcher dispatcher = slingRequest.getRequestDispatcher(resourceWrapper, options);
    dispatcher.include(slingRequest, slingResponse);
%>