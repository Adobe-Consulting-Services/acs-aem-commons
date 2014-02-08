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
        import="org.apache.commons.lang.StringUtils"%><%

    final String pid = properties.get("acs.pid", "PID Not set");
    final String configurationType = properties.get("acs.configurationType", "single");
    final String replicationActionType = properties.get("prop.replication-action-type", "");

    final String[] hierarchicalRules = properties.get("prop.rules.hierarchical", new String[]{});
    final String[] resourceOnlyRules = properties.get("prop.rules.resource-only", new String[]{});
%>

<h3>Dispatcher Flush Rule Configuration</h3>

<h4>Pid: <%= pid %></h4>
<h4>Configuration Type: <%= configurationType %></h4>

<p><%= properties.get("notes", "")%></p>


<h4>Replication Action Type: <%= replicationActionType %></h4>

<h4>Hierarchical Rules</h4>

<ul>
    <% for(final String rule : hierarchicalRules) { %>
    <li><%= rule %></li>
    <% } %>
</ul>


<h4>Resource Only Rules</h4>

<ul>
    <% for(final String rule : resourceOnlyRules) { %>
    <li><%= rule %></li>
    <% } %>
</ul>