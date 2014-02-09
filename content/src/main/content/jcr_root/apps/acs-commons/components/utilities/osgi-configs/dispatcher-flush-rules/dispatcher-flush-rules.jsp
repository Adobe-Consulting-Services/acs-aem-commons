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
        import="org.apache.commons.lang.StringUtils,
                com.adobe.acs.commons.configuration.osgi.OsgiConfigHelper,
                com.adobe.acs.commons.configuration.osgi.OsgiConfigConstants"%><%

    /* Services */
    final OsgiConfigHelper osgiConfigHelper = sling.getService(OsgiConfigHelper.class);

    /* OSGi Configuration Creators Properties */
    final String configurationType = properties.get(OsgiConfigConstants.PN_CONFIGURATION_TYPE, "single");

    /* Dispatcher Flush Rules Config Properties  */
    final String replicationActionType = properties.get("replication-action-type", "");
    final String[] hierarchicalRules = properties.get("rules.hierarchical", new String[]{});
    final String[] resourceOnlyRules = properties.get("rules.resource-only", new String[]{});
%>

<h3>
    Dispatcher Flush Rule Configuration
    <span style="float:right;">[ <a
        href="http://adobe-consulting-services.github.io/acs-aem-commons/features/dispatcher-flush-rules.html"
         target="_blank">Documentation</a> ]</span>
</h3>

<p>
    <strong>PID</strong>:
    <%= osgiConfigHelper.getPID(resource) %>
</p>

<p>
    <strong>Configuration Type</strong>:
    <%= configurationType %>
<p>

<p>
    <strong>Replication action type</strong>:
    <%= replicationActionType %>
</p>

<p><strong>Hierarchical rules</strong></p>

<ul>
    <% if (hierarchicalRules.length == 0) { %>
        <li class="not-set">No rules set</li>
    <% } %>

    <% for(final String rule : hierarchicalRules) { %>
    <li><%= rule %></li>
    <% } %>
</ul>


<p><strong>Resource-Only rules</strong></p>

<ul>
    <% if (resourceOnlyRules.length == 0) { %>
    <li class="not-set">No rules set</li>
    <% } %>

    <% for(final String rule : resourceOnlyRules) { %>
    <li><%= rule %></li>
    <% } %>
</ul>

<% if (StringUtils.isNotBlank(properties.get("acs.README", ""))) { %>
    <hr/>
    <p><strong>Notes</strong></p>
    <p><%= properties.get("acs.README", "")%></p>
<% } %>
