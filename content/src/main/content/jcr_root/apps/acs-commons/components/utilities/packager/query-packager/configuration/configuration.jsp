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
%><%@page session="false" contentType="text/html" pageEncoding="utf-8" %><%

    /* Package Definition */
    final String queryLanguage = properties.get("queryLanguage", "xpath");
    final String relPath = properties.get("relPath", "");
    final String query = properties.get("query", "");

    final String packageName = properties.get("packageName", "query");
    final String packageGroupName = properties.get("packageGroupName", "Query");
    final String packageVersion = properties.get("packageVersion", "1.0.0");
    final String packageDescription = properties.get("packageDescription",
            "Query Package initially defined by a ACS AEM Commons - Query Packager configuration.");

    final String packageACLHandling = properties.get("packageACLHandling", "Overwrite");
    final String conflictResolution = properties.get("conflictResolution", "IncrementVersion");
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

<h3>Query</h3>
<p>Query Language: <%= xssAPI.encodeForHTML(queryLanguage) %></p>
<p>Query: <%= xssAPI.encodeForHTML(query) %></p>
<p>Relative Path: <%= xssAPI.encodeForHTML(relPath) %></p>

<%-- Common Form (Preview / Create Package) used for submittins Packager requests --%>
<%-- Requires this configuration component have a sling:resourceSuperType of the ACS AEM Commons Packager --%>
<cq:include script="partials/form.jsp"/>
