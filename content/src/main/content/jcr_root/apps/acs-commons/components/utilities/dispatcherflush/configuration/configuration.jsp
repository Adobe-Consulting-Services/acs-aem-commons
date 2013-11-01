<%@ page import="com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher" %>
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
        import="com.day.cq.replication.Agent,
        com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher"%><%

    /* Services */
    final DispatcherFlusher dispatcherFlusher = sling.getService(DispatcherFlusher.class);

    /* Agents */
    final Agent[] flushAgents = dispatcherFlusher.getFlushAgents();
    boolean hasAgents = flushAgents.length > 0;

    /* Flush Paths */
    final String[] paths = properties.get("paths", new String[]{});
    boolean hasPaths = paths.length > 0;
%>

<div class="dispatcher-flush-config">
    <h3>Paths to Flush</h3>
    <ul>
        <% if(!hasPaths) { %><li class="not-set">Dispatcher flush paths not set</li><% } %>
        <% for(final String path : paths) { %>
        <li><%= path %></li>
        <% } %>
    </ul>

    <% if(hasPaths) { %>
    <form action="<%= resource.getPath() %>.flush.html" method="post">
        <input type="submit" value="Flush Paths"/>
    </form>
    <% } %>

    <h3>Active Flush Agent Logs</h3>
    <ul>
        <% if(!hasAgents) { %><li class="not-set">No active Dispatcher Flush agents</li><% } %>
        <% for(final Agent agent : flushAgents) { %>
        <li><a href="<%= resourceResolver.map(agent.getConfiguration().getConfigPath()) %>.log.html" target="_target"><%= agent.getConfiguration().getName() %></a></li>
        <% } %>
    </ul>
</div>
