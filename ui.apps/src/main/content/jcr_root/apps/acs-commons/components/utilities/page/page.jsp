<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2013 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>

<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="com.adobe.granite.xss.XSSAPI,
                com.day.cq.wcm.api.WCMMode,
                org.apache.commons.lang.StringUtils,
                com.adobe.acs.commons.util.TextUtil"%><%

    /* Services */
    final XSSAPI xss = sling.getService(XSSAPI.class);

    /* Page Properties */
    final String pageTitle = TextUtil.getFirstNonEmpty(
                    currentPage.getPageTitle(),
                    currentPage.getTitle(),
                    currentPage.getName());

    final String pageDescription = currentPage.getDescription();
%><%

%><% WCMMode.EDIT.toRequest(slingRequest); %><%

%><!DOCTYPE html>
    <head>
        <meta charset="utf-8">
        <meta http-equiv="Content-Type" content="text/html; utf-8" />

        <title><%= xss.encodeForHTML(pageTitle) %></title>
        <meta name="description" content="<%= xss.encodeForHTML(pageDescription) %>">
        <meta name="viewport" content="width=device-width, initial-scale=1">

        <cq:includeClientLib css="cq.wcm.edit,acs-commons.utilities.page,acs-commons.components"/>
        <script src="/libs/cq/ui/resources/cq-ui.js" type="text/javascript"></script>
        <cq:includeClientLib js="cq.wcm.edit,acs-commons.utilities.page"/>

        <cq:include script="headlibs.jsp"/>
    </head>

    <body>
        <div id="acs-commons" class="acs-commons">
            <cq:include script="main.jsp"/>
        </div>
    </body>
</html>
