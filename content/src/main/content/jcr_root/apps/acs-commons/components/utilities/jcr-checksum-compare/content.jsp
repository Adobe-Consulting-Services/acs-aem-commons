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
                  java.util.List, org.apache.sling.commons.json.JSONObject, org.apache.sling.commons.json.JSONArray, org.apache.commons.lang.StringUtils"%><%

    AgentHosts agentHosts = sling.getService(AgentHosts.class);
    List<String> hosts = agentHosts.getHosts(AemPublishAgentFilter.AEM_PUBLISH_AGENT_FILTER);

    pageContext.setAttribute("hostNames", "['" + StringUtils.join(hosts, "', '") + "']");

%><div class="page"
        role="main"
        ng-controller="MainCtrl"
        ng-init="app.resource = '${resourcePath}'; init(${hostNames});">

    <form class="coral-Form coral-Form--vertical acs-form"
            novalidate
            ng-show="!app.running">

        <section class="coral-Form-fieldset">
            <h3 class="coral-Form-fieldset-legend">Path</h3>

            <input class="coral-Form-field coral-Textfield"
                   ng-model="form.path"
                   type="text"
                   placeholder="Path to compare">
        </section>


        <section class="coral-Form-fieldset">
            <h3 class="coral-Form-fieldset-legend">Path</h3>

            <input class="coral-Form-field coral-Textfield"
                   ng-model="form.path"
                   type="text"
                   placeholder="Path to compare">
        </section>


        <div class="coral-TabPanel coral-TabPanel--large" data-init="tabs">
            <nav class="coral-TabPanel-navigation">
                <a class="coral-TabPanel-tab is-active" data-toggle="tab">Content Comparison</a>
                <a class="coral-TabPanel-tab" data-toggle="tab">Download JSON</a>
            </nav>

            <div class="coral-TabPanel-content">
                <section class="coral-TabPanel-pane is-active">
                    <cq:include script="includes/tab-content-hashes.jsp"/>
                </section>

                <section class="coral-TabPanel-pane">
                    <cq:include script="includes/tab-json-dump.jsp"/>
                </section>
            </div>
        </div>

    </form>
</div>