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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"
        import="org.apache.sling.api.SlingHttpServletResponse,
                com.adobe.acs.commons.wcm.ComponentHelper,
                com.adobe.acs.commons.util.ModeUtil,
                com.adobe.acs.commons.errorpagehandler.ErrorPageHandlerService"%><%

    final ErrorPageHandlerService errorPageHandlerService = sling.getService(ErrorPageHandlerService.class);

    if (errorPageHandlerService != null && errorPageHandlerService.isEnabled()) {
        final ComponentHelper componentHelper = sling.getService(ComponentHelper.class);
        final int status = errorPageHandlerService.getStatusCode(slingRequest);

        if (status >= SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR &&
                !ModeUtil.isDisabled(slingRequest)) {
            // If error is some sort of internal error (500+) and on Author (since WCMMode.DISABLED ~> Publish)
            if (ModeUtil.isPreview(slingRequest)) {
                %><cq:include script="/apps/acs-commons/components/utilities/errorpagehandler/preview/errormessage.jsp" /><%
                return;
            } else {
                // In Author and Edit or Design, so allow OOTB WCMDebugFilter to handle the error message display
                return;
            }
        } else {
            slingResponse.setStatus(status);
            final String path = errorPageHandlerService.findErrorPage(slingRequest, resource);

            if (path != null) {
                errorPageHandlerService.resetRequestAndResponse(slingRequest, slingResponse, status);
                errorPageHandlerService.includeUsingGET(slingRequest, slingResponse, path);
                return;
            }
        }
    }
%><%@include file="/libs/sling/servlet/errorhandler/default.jsp" %>