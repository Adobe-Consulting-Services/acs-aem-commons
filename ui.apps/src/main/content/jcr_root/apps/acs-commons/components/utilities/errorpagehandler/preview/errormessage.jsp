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
%><%@page session="false" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
          import="com.adobe.acs.commons.errorpagehandler.ErrorPageHandlerService,
                org.apache.commons.lang.StringEscapeUtils,
                javax.servlet.http.HttpServletResponse" %><%

    final ErrorPageHandlerService errorPageHandlerService = sling.getService(ErrorPageHandlerService.class);
    if (errorPageHandlerService == null || !errorPageHandlerService.isEnabled()) {
        return;
    }

    final String stackTrace = StringEscapeUtils.escapeHtml(errorPageHandlerService.getException(slingRequest));
    final String requestProgress = StringEscapeUtils.escapeHtml(errorPageHandlerService.getRequestProgress(slingRequest));
    final String path = errorPageHandlerService.findErrorPage(slingRequest, resource);

    final String erroringTitle = currentPage == null ? resource.getName() : currentPage.getTitle();
    final String erroringPath = currentPage == null ? resource.getPath() : currentPage.getPath();

    errorPageHandlerService.resetRequestAndResponse(slingRequest,
            slingResponse,
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
%>
<!DOCTYPE html>
<html lang="en">

<head>
    <title>Error | Adobe Experience Manager</title>
    <cq:includeClientLib css="acs-commons.error-page-handler"/>
    <cq:includeClientLib js="cq.foundation-main"/>
    <cq:includeClientLib js="cq.wcm.edit"/>
</head>

<body>
<div id="error-page-handler">
    <nav class="toolbar content-header">
        <div class="right icongroup">
            <a href="<%= path %>" class="button edit-error-page" target="_blank">Edit Error Page</a>
            <a href="<%= resource.getPath() %>.html?wcmmode=disabled" class="button" target="_blank">Publish Preview</a>
            <a href="<%= resource.getPath() %>.html" class="button primary edit-mode">Return to Edit mode</a>
        </div>
    </nav>

    <div class="message">
        <h1>ATTENTION</h1>

        <p>
            An error occurred preventing the page <strong><%= erroringTitle %></strong> at <strong><%= erroringPath %></strong> from rendering.
        </p>

        <p>
            Please consult your application support team.
        </p>

        <p>
            In the Publish environment, this erroring page will render using the error page:
            <a href="<%= path %>" target="_blank"><%= path %></a>
        </p>
    </div>

    <div class="content">
        <div class="section collapsed" id="error-message">
            <h2>Error Message</h2>
            <a href="#error-message"
               class="button toggle"
               role="button"
               data-collapse-text="Collapse error message"
               data-expand-text="Expand error message">Expand error message</a>
            <pre><%= stackTrace %></pre>
        </div>

        <div class="section collapsed" id="request-progress">
            <h2>Request Progress</h2>
            <a href="#request-progress"
               class="button toggle"
               role="button"
               data-collapse-text="Collapse request progress"
               data-expand-text="Expand request progress">Expand request progress</a>
            <pre><%= requestProgress %></pre>
        </div>
    </div>
</div>
<cq:includeClientLib js="acs-commons.error-page-handler"/>
</body>
</html>