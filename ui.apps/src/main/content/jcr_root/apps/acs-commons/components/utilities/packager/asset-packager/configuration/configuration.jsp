<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2019 Adobe
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
%><%@page session="false" contentType="text/html" pageEncoding="utf-8" %><%

    /* Package Definition */
    final String[] principalNames = properties.get("principalNames", new String[]{});
    final String[] includePatterns = properties.get("includePatterns", new String[]{});

    final String packageName = properties.get("packageName", "assets");
    final String packageGroupName = properties.get("packageGroupName", "Assets");
    final String packageVersion = properties.get("packageVersion", "1.0.0");
    final String packageDescription = properties.get("packageDescription", "Asset Package initially defined by a ACS AEM Commons - Asset Packager configuration.");

    final String packageACLHandling = properties.get("packageACLHandling", "Overwrite");
    final String conflictResolution = properties.get("conflictResolution", "IncrementVersion");

    /* Asset Packaging Configuration */
    final String pagePath = properties.get("pagePath", "");
    final boolean excludePages = properties.get("excludePages", false);

    /* Advanced Packaging Details */
    final String assetPrefix = properties.get("assetPrefix", "");
    final String[] pageExclusions = properties.get("pageExclusions", new String[]{});
    final String[] assetExclusions = properties.get("assetExclusions", new String[]{});

    final boolean showAdvanced = !"".equals(assetPrefix) || pageExclusions.length > 0 || assetExclusions.length > 0;
%>

<h3>Package definition</h3>
<ul>
    <li>Package name: <%= xssAPI.encodeForHTML(packageName) %></li>
    <li>Package group: <%= xssAPI.encodeForHTML(packageGroupName) %></li>
    <li>Package version: <%= xssAPI.encodeForHTML(packageVersion) %></li>
    <li>Package description: <%= xssAPI.encodeForHTML(packageDescription) %></li>
    <li>Package ACL handling: <%= xssAPI.encodeForHTML(packageACLHandling) %></li>
    <li>Conflict resolution: <%= xssAPI.encodeForHTML(conflictResolution) %></li>
</ul>

<h3>Asset Packaging Configuration</h3>
<ul>
    <li>Page Path: <%= pagePath %></li>
    <li>Exclude Pages: <%= excludePages %></li>
</ul>

<% if (showAdvanced) { %>
<h3>Advanced Details</h3>
<ul>
    <li>Asset Prefix: <%= assetPrefix %></li>
    <li>Page Exclusion Patterns:
      <ul style="margin-bottom:0;">
        <% for (String exclusion : pageExclusions) { %>
          <li><%= exclusion %></li>
        <% } %>
      </ul>
    </li>
    <li>Asset Exclusion Patterns:
      <ul style="margin-bottom:0;">
        <% for (String exclusion : assetExclusions) { %>
          <li><%= exclusion %></li>
        <% } %>
      </ul>
    </li>
</ul>
<% } %>

<%-- Common Form (Preview / Create Package) used for submitting Packager requests --%>
<%-- Requires this configuration component have a sling:resourceSuperType of the ACS AEM Commons Packager --%>
<cq:include script="partials/form.jsp"/>
