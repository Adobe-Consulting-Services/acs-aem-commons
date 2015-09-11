<%@ page import="com.adobe.acs.commons.replication.AgentHosts, com.day.cq.replication.AgentFilter" %>
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
  --%><%@include file="/libs/foundation/global.jsp" %><%
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
%><%@page session="false"
          import="com.adobe.acs.commons.replication.AgentHosts, 
                  com.adobe.acs.commons.replication.AemPublishAgentFilter"%><%

    AgentHosts agentHosts = sling.getService(AgentHosts.class);
    pageContext.setAttribute("hosts", agentHosts.getHosts(AemPublishAgentFilter.AEM_PUBLISH_AGENT_FILTER));

%>

<%--
<h4>Self</h4>
<div id="diff_self}">
    <pre>{{ results[0] }}</pre>
</div>

<hr/>

<c:forEach var="host" items="${hosts}" varStatus="loop">
    <h4>${host}</h4>
    <div id="diff_${host}">
        {{ results[${loop.count}] }}
    </div>
    
    <hr/>
</c:forEach>
--%>

<div class="coral-TabPanel coral-TabPanel--stacked" data-init="tabs">
    <nav class="coral-TabPanel-navigation">

        <a class="coral-TabPanel-tab" data-toggle="tab">Diff</a>

        <a ng-repeat="host in hosts track by $index"
           class="coral-TabPanel-tab {{ $index === 0 ? is-active : '' }}"
           data-toggle="tab">{{ host.name }}</a>

    </nav>
    <div class="coral-TabPanel-content">

        <section class="coral-TabPanel-pane">
            <div diff
                 inline="true"
                 base-data="diff.baseData"
                 new-data="diff.newData"></div>
        </section>
        <section ng-repeat="host in hosts track by $index"
                 class="coral-TabPanel-pane"><pre>{{ host.data }}</pre></section>

    </div>
</div>


