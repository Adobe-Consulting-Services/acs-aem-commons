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
--%><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false" %><%
%><%@ page import="com.adobe.acs.commons.granite.ui.components.CustomELVariableInjector" %><%
%><%@ page import="org.apache.sling.api.request.RequestDispatcherOptions" %><%
%><%
    CustomELVariableInjector variableInjector = sling.getService(CustomELVariableInjector.class);
    if (variableInjector == null) {
        throw new IllegalStateException("No CustomELVariableInjector service is available");
    }
    variableInjector.inject(slingRequest);
    
    RequestDispatcherOptions options = new RequestDispatcherOptions();
    options.setForceResourceType("granite/ui/components/coral/foundation/container");
    RequestDispatcher dispatcher = slingRequest.getRequestDispatcher(resource, options);
    dispatcher.include(slingRequest, slingResponse);
%>