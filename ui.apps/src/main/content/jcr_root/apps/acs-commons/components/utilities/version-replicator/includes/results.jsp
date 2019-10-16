<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2015 Adobe
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
<%@include file="/libs/foundation/global.jsp" %><%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<%@ page
         import="com.day.cq.i18n.I18n,
                 com.day.cq.replication.AgentManager,
                 java.util.Collection,
                 com.day.cq.replication.Agent"
%><%

    final AgentManager agentManager = sling.getService(AgentManager.class);
    final Collection<Agent> agents = agentManager.getAgents().values();
    pageContext.setAttribute("agents", agents);
%>
  <%-- Error Message Notification --%>
  <section class="coral-Well" ng-show="app.running && app.results">
        <div id="error-message">
            <h4 acs-coral-heading>An error occurred. Please correct and try again.</h4>
            <ul><li class="message"></li></ul>
        </div>
  </section>


  <%-- Result Notifications --%>
  <section class="coral-Well" ng-show="app.running" ng-model="app.results">
       <div id="results">
          <div id="replication-agents-info">
              <h4 acs-coral-heading>Your replication request has been submitted.</h4>
              <p class="instructions">This request may take some time to complete. Please review the Replication agent logs and
                 configurations the complete replication status does not appear below shortly.</p>
              <div class="message">
                    <li ng-repeat="agent in agents">

                        <h3>{{title}}</h3>
                        <ul>
                            <li>
                                <a href="{{logHref}}" target="_blank">Replication Log</a>
                            </li>
                            <li>
                                <a href="{{agentHref}}" target="_blank">Replication Agent Config</a>
                            </li>
                        </ul>

                    </li>
              </div>
          </div>
          <div id="replication-queue-message">
              <h4>Version replication initiated with the following items</h4>
              <ul>
                  <li ng-repeat="result in results">
                       <li>{{path}} [ <strong>{{status}}</strong>{{[version=""]}}
                            <%-- Check version null --%>
                        ]
                  </li>
              </ul>
          </div>
       </div>
  </section>
