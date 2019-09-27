<%@ page import="com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher" %>
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
        import="com.day.cq.replication.Agent,
        org.apache.commons.lang.StringUtils,
        com.adobe.acs.commons.replication.dispatcher.DispatcherFlushFilter"%><%

    /* Services */
    final DispatcherFlusher dispatcherFlusher = sling.getService(DispatcherFlusher.class);

    /* Agents */
    final Agent[] flushAgents = dispatcherFlusher.getAgents(DispatcherFlushFilter.HIERARCHICAL);
    boolean hasAgents = flushAgents.length > 0;

    /* Flush Action */
    final String actionType = properties.get("replicationActionType", "");
    boolean hasActionType = StringUtils.isNotBlank(actionType);

    /* Flush Paths */
    final String[] paths = properties.get("paths", new String[]{});
    boolean hasPaths = paths.length > 0;
%>

<% if(hasActionType && hasPaths && hasAgents) { %>
<form action="<%= slingRequest.getContextPath() %><%= resource.getPath() %>.flush.html" method="post">
    <input class="button" type="submit" value="Flush Paths on Dispatchers"/>
</form>
<% } %>

<h3>Flush Action</h3>
<ul>
    <% if(StringUtils.equals("ACTIVATE", actionType)) { %>
    <li>Invalidate Cache</li>
    <% } else if(StringUtils.equals("DELETE", actionType)) { %>
    <li>Delete Cache</li>
    <% } else { %>
    <li class="not-set">Flush method not set</li>
    <% } %>
</ul>

<h3>Paths to Flush</h3>
<ul>
    <% if(!hasPaths) { %><li class="not-set">Dispatcher flush paths not set</li><% } %>
    <% for(final String path : paths) { %>
    <li><%= path %></li>
    <% } %>
</ul>

<h3>Active Dispatcher Flush Agents (excludes Resource Only agents)</h3>
<ul>
    <% if(!hasAgents) { %><li class="not-set"><a href="<%= slingRequest.getContextPath() %>/miscadmin#/etc/replication/agents.author" target="_blank">No active
    Dispatcher Flush replication agents</a></li><% } %>
    <% for(final Agent agent : flushAgents) { %>
    <li><a href="<%= resourceResolver.map(agent.getConfiguration().getConfigPath()) %>.log.html" target="_target"><%= agent.getConfiguration().getName() %></a></li>
    <% } %>
</ul>
