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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"
          import="com.adobe.acs.commons.replication.AemPublishAgentFilter,
                  com.adobe.acs.commons.replication.AgentHosts,
                  java.util.List,
                  org.apache.commons.lang.StringUtils"%><%

    AgentHosts agentHosts = sling.getService(AgentHosts.class);
    List<String> hosts = agentHosts.getHosts(AemPublishAgentFilter.AEM_PUBLISH_AGENT_FILTER);

    pageContext.setAttribute("hostNames", "['" + StringUtils.join(hosts, "', '") + "']");

%><div class="page"
        role="main"
        ng-controller="MainCtrl"
        ng-init="app.resource = '${resourcePath}'; init(${hostNames});">

    <form class="coral-Form coral-Form--vertical acs-form"
            novalidate
            ng-hide="app.running">


        <div id="tabs" class="coral-TabPanel coral-TabPanel--large" data-init="tabs">
            <nav class="coral-TabPanel-navigation">
                <a class="coral-TabPanel-tab is-active" data-toggle="tab">Configuration</a>
                <a class="coral-TabPanel-tab content-comparison-tab" data-toggle="tab">Content Comparison</a>
                <a class="coral-TabPanel-tab download-json-tab" data-toggle="tab">Download JSON</a>
            </nav>

            <div class="coral-TabPanel-content">
                <section class="coral-TabPanel-pane is-active">
                    <cq:include script="includes/tab-configuration.jsp"/>
                </section>

                    <section class="coral-TabPanel-pane">
                        <cq:include script="includes/tab-content-comparison.jsp"/>
                    </section>

                    <section class="coral-TabPanel-pane">
                        <cq:include script="includes/tab-download-json.jsp"/>
                    </section>
                </div>
        </div>

    </form>

    <section ng-show="app.running" class="positioned-example-section">
        <div class="coral-Wait coral-Wait--large coral-Wait--center"></div>
    </section>
</div>