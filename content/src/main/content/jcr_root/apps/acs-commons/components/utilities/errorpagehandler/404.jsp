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
<%@page session="false"
        import="com.adobe.acs.commons.errorpagehandler.ErrorPageHandlerService,
        org.apache.sling.api.resource.Resource,
      	org.apache.sling.api.SlingHttpServletRequest,
      	org.apache.commons.lang3.StringUtils"%><%
%><%@include file="/libs/foundation/global.jsp" %><%
    ErrorPageHandlerService errorPageHandlerService = sling.getService(ErrorPageHandlerService.class);

    if (errorPageHandlerService != null && errorPageHandlerService.isEnabled()) {
        // Check for and handle 404 Requests properly according on Author/Publish
        if (errorPageHandlerService.doHandle404(slingRequest, slingResponse)) {

            SlingHttpServletRequest req = sling.getRequest();

            String resourcePath = StringUtils.removeEnd(resource.getPath(),resource.getName());

            Resource res = resource.getResourceResolver().resolve(req , resourcePath);


            final String path = errorPageHandlerService.findErrorPage(slingRequest, res);

           
            if (path != null) {
                slingResponse.setStatus(404);
                errorPageHandlerService.includeUsingGET(slingRequest, slingResponse, path);
                return;


                
            }
        }
    }
%><%@include file="/libs/sling/servlet/errorhandler/default.jsp" %>
