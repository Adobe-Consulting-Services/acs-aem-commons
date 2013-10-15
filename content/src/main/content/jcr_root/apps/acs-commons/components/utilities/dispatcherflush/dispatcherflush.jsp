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
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="org.apache.commons.lang3.StringEscapeUtils,
    			com.adobe.acs.commons.util.TextUtil,
    			com.day.cq.wcm.api.WCMMode"%><%

    final WCMMode mode = WCMMode.fromRequest(slingRequest);
    final String title = TextUtil.getFirstNonEmpty(
                    currentPage.getPageTitle(),
                    currentPage.getTitle(),
                    currentPage.getName());

    final boolean success = "/success".equals(slingRequest.getRequestPathInfo().getSuffix());

%>
<% WCMMode.EDIT.toRequest(slingRequest); %>

<!DOCTYPE html>
    <head>
        <title>CQ5 Design | <%= StringEscapeUtils.escapeHtml4(title) %></title>
        <meta http-equiv="Content-Type" content="text/html; utf-8" />

        <script src="/libs/cq/ui/resources/cq-ui.js" type="text/javascript"></script>
        <cq:includeClientLib categories="cq.wcm.edit,acs-commons.utilities.dispatcherflush"/>
    </head>

    <body class="dispatcher-flusher">
        <h1><%= StringEscapeUtils.escapeHtml4(title) %></h1>

        <% if(success) { %>
        <div class="success-message">
            <p>
                Your dispatcher flush requests have been issued.
            </p>
            <p>
                Please review your Dispatcher Flush Agent logs to ensure all replication requests were successfully processed.
            </p>
        </div>
        <% } %>

        <cq:include path="configuration" resourceType="<%= component.getPath() + "/configuration" %>"/>
    </body>
</html>

<% mode.toRequest(slingRequest); %>
