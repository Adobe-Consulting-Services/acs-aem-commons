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
                com.adobe.acs.commons.util.TextUtil,
                com.day.cq.replication.Agent,
    			com.day.cq.replication.AgentManager,
    			com.day.cq.wcm.api.WCMMode,
    			org.apache.commons.lang.StringUtils,
    			org.apache.commons.lang3.StringEscapeUtils,
    			java.util.Map"%><%

    /* Services */
    final AgentManager agentManager = sling.getService(AgentManager.class);

    /* Properties and Data */
    final WCMMode mode = WCMMode.fromRequest(slingRequest);

    final Map<String, Agent> agents = agentManager.getAgents();

    final String title = TextUtil.getFirstNonEmpty(
                    currentPage.getPageTitle(),
                    currentPage.getTitle(),
                    currentPage.getName());

    final boolean result = StringUtils.isNotBlank((slingRequest.getRequestPathInfo().getSuffix()));

%>
<% WCMMode.EDIT.toRequest(slingRequest); %>

<!DOCTYPE html>
    <head>
        <title>Dispatcher Flush | <%= StringEscapeUtils.escapeHtml4(title) %></title>
        <meta http-equiv="Content-Type" content="text/html; utf-8" />

        <script src="/libs/cq/ui/resources/cq-ui.js" type="text/javascript"></script>
        <cq:includeClientLib categories="cq.wcm.edit,acs-commons.utilities.dispatcherflush"/>
    </head>

    <body class="dispatcher-flusher">
        <h1>
            Dispatcher Flush
        </h1>
        <h2>
            <%= StringEscapeUtils.escapeHtml4(title) %>
        </h2>

        <% if(result) { %>
        <div class="result-message">
            <p>
                Your dispatcher flush requests have been issued with the following results.
            </p>

            <ul>
                <%
                boolean errors = false;
                int index = 0;

                do {
                    final Agent agent = agents.get(PathInfoUtil.getSuffixSegment(slingRequest, index));
                    final boolean status = StringUtils.equals("true", PathInfoUtil.getSuffixSegment(slingRequest, index + 1));

                    if(agent == null) { break; }
                    if(!status || errors) { errors = true;}

                    %><li><a href="<%= resourceResolver.map(agent.getConfiguration().getConfigPath()) %>.html" target="_blank"><%= agent.getConfiguration().getName() %></a>: <%= status ? "Success" : "Error" %></li><%

                    index += 2;
                } while(true);

                if(index == 0) { %>No Active Dispatcher Flush agents could be found for this run mode.<% }
                %>

            </ul>

            <% if(errors) { %>
            <p>
                Please review your Dispatcher Flush Agent logs to ensure all replication requests were successfully processed.
            </p>
            <% } %>
        </div>
        <% } %>

        <cq:include path="configuration" resourceType="<%= component.getPath() + "/configuration" %>"/>
    </body>
</html>

<% mode.toRequest(slingRequest); %>