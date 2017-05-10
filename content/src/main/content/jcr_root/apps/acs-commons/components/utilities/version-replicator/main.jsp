<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 - 2014 Adobe
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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@ page contentType="text/html" pageEncoding="utf-8" session="false"
         import="com.day.cq.i18n.I18n,
                 com.day.cq.replication.AgentManager,
                 java.util.Collection,
                 com.day.cq.replication.Agent"
%><%

    final I18n i18n = new I18n(slingRequest);
    final AgentManager agentManager = sling.getService(AgentManager.class);
    final Collection<Agent> agents = agentManager.getAgents().values();
    final String action = resourceResolver.map(slingRequest, currentPage.getContentResource().getPath())
            + ".replicateversion.json";
%>

<h1>
    <%= i18n.get("Version Replication") %>
</h1>

<%-- Error Message Notification --%>
<div id="error-message" class="notification hidden">
    <h4>An error occurred. Please correct and try again.</h4>
    <ul><li class="message"></li></ul>
</div>

<%-- Result Notifications --%>
<div id="results" class="notification hidden">
    <div id="replication-agents-info">
        <h2>Your replication request has been submitted.</h2>
        <p>This request may take some time to complete. Please review the Replication agent logs and
            configurations the complete replication status does not appear below shortly.</p>
        <div class="message"></div>
    </div>
    <div id="replication-queue-message"></div>
</div>

<div style="width: 650px">

    <%-- Inline Form of CQ Widgets and normal HTML inputs --%>
    <div class="cq-inline-form" data-action="<%= action %>">

        <%-- CQ Widgets --%>
        <div id="CQ" class="cq-widget-form">
            <%-- Root Paths --%>
            <div class="field-row">
                <label for="multifieldpaths">
                    <%=i18n.get("Root paths")%>
                </label>

                <p class="field-instructions">
                    Select the root paths to replicate. Resource versions matching the specified date &amp; time
                    will be replicated.
                </p>

                <%-- Container to inject ExtJS Widgets into --%>
                <div id="cq-inject-rootpaths"></div>
            </div>

            <%-- Date and Time --%>
            <div class="field-row">

                <label for="datetimecal">
                    <%=i18n.get("Version date &amp; time")%>
                </label>

                <p class="field-instructions">
                    Select the date and time used to derive the correct resource version to replicate.
                </p>

                <div id="cq-inject-datetime"></div>
            </div>
        </div>

        <%-- Replication Agents --%>
        <div class="field-row">
            <label for="agentId">
                <%= i18n.get("Replication agents") %>
            </label>

            <p class="field-instructions">
                Select 1 or more replication agents target for this replication.
            </p>

            <select id="cmbAgent" name="cmbAgent" multiple size="<%= agents.size() %>">
                <% for (final Agent agent : agents) {
                    if (agent.isEnabled() && agent.isValid()) {
                        %><option
                                value="<%= agent.getId() %>"
                                data-agent-path="<%= resourceResolver.map(slingRequest, agent.getConfiguration().getConfigPath()) %>">
                            <%= agent.getConfiguration().getName() %>
                        </option><%
                    }
                } %>
            </select>
        </div>

        <%-- Submit button --%>
        <div class="field-row">
            <input type="submit"
                   id="submit-button"
                   class="button"
                   value="<%= i18n.get("Replicate Versions") %>"/>
        </div>
    </div>
</div>

<cq:includeClientLib js="acs-commons.utilities.version-replicator"/>
