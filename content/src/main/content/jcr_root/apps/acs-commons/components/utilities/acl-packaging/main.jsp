<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2014 Adobe
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
          import="com.adobe.acs.commons.util.PathInfoUtil,
                com.adobe.granite.xss.XSSAPI,
    			org.apache.commons.lang.StringUtils,
                com.adobe.acs.commons.util.TextUtil,
    			java.util.Map"%><%

    /* Page Properties */
    final String pageTitle = TextUtil.getFirstNonEmpty(
            currentPage.getPageTitle(),
            currentPage.getTitle(),
            currentPage.getName());
%>

<h1>ACL Packaging</h1>

<h2><%= xssAPI.encodeForHTML(pageTitle) %></h2>

<cq:include path="configuration" resourceType="<%= component.getPath() + "/configuration" %>"/>
