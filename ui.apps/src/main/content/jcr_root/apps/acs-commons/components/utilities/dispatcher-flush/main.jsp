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
          import="com.adobe.acs.commons.util.PathInfoUtil,
                com.day.cq.replication.Agent,
    			com.day.cq.replication.AgentManager,
                org.apache.sling.xss.XSSAPI,
    			org.apache.commons.lang.StringUtils,
                com.adobe.acs.commons.util.TextUtil,
    			java.util.Map"%><%

    /* Services */
    final XSSAPI xss = sling.getService(XSSAPI.class);
    final AgentManager agentManager = sling.getService(AgentManager.class);

    /* Page Properties */
    final String pageTitle = TextUtil.getFirstNonEmpty(
            currentPage.getPageTitle(),
            currentPage.getTitle(),
            currentPage.getName());

    /* Properties and Data */
    final Map<String, Agent> agents = agentManager.getAgents();
    final boolean result = StringUtils.isNotBlank((slingRequest.getRequestPathInfo().getSuffix()));
    final boolean generalError =
            StringUtils.equals("/replication-error", slingRequest.getRequestPathInfo().getSuffix());
%>

<h1>Dispatcher Flush</h1>

<h2><%= xss.encodeForHTML(pageTitle) %></h2>

<% if(result) { %>
<div class="call-out">
    <p>
        Your dispatcher flush requests have been issued with the following results:
    </p>

    <% if(generalError) { %>
    <p>
        An error occurred during replication.
        Possible issues include invalid flush paths or lack of active dispatcher flush agents.
    </p>
    <% } %>

    <ul>
        <%
        boolean errors = false;
        int index = 0;

        while(!generalError) {
            final Agent agent = agents.get(PathInfoUtil.getSuffixSegment(slingRequest, index));
            final boolean status = StringUtils.equals("true", PathInfoUtil.getSuffixSegment(slingRequest, index + 1));

            if(agent == null) { break; }
            if(!status || errors) { errors = true;}

            %><li><a href="<%= resourceResolver.map(agent.getConfiguration().getConfigPath()) %>.html" target="_blank"><%= agent.getConfiguration().getName() %></a>: <%= status ? "Success" : "Error" %></li><%

            index += 2;
        };

        if(index == 0 && !generalError) { %>No active Dispatcher Flush agents could be found for this run mode.<% } %>
    </ul>

    <% if(errors || generalError) { %>
    <p>
        Please review your Dispatcher Flush Agent logs to ensure all replication requests were successfully processed.
    </p>
    <% } %>
</div>
<% } %>

<cq:include path="configuration" resourceType="<%= component.getPath() + "/configuration" %>"/>
