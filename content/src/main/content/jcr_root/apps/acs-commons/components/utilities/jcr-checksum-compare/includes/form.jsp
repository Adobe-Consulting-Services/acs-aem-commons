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
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"
          import="com.adobe.acs.commons.replication.AgentHosts, 
          com.adobe.acs.commons.replication.AemPublishAgentFilter"%><%

    AgentHosts agentHosts = sling.getService(AgentHosts.class);
    pageContext.setAttribute("hosts", agentHosts.getHosts(AemPublishAgentFilter.AEM_PUBLISH_AGENT_FILTER));

%><form class="coral-Form coral-Form--vertical acs-form"
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
        <h3 class="coral-Form-fieldset-legend">AEM Instances</h3>

        <ul class="coral-List coral-List--minimal">
            <li class="coral-List-item">
                <label class="coral-Checkbox">
                    <input class="coral-Checkbox-input"
                           ng-model="form.targets.self"
                           ng-true-value="true" 
                           ng-false-value="false"
                           type="checkbox"
                           value="self">
                    <span class="coral-Checkbox-checkmark"></span>
                    <span class="coral-Checkbox-description">Self</span>
                </label>
            </li>
            <c:forEach var="host" items="${hosts}" varStatus="status">
            <li class="coral-List-item">
                <label class="coral-Checkbox">
                    <input class="coral-Checkbox-input"
                           type="checkbox"
                           ng-model="form.targets.remote[${status.index}]"
                           ng-true-value="'${host}'"
                           ng-false-value=""
                           value="${host}">
                    <span class="coral-Checkbox-checkmark"></span>
                    <span class="coral-Checkbox-description">${host}</span>
                </label>
            </li>
            </c:forEach>
        </ul>
    </section>

    <hr/>

    <button type="submit"
            role="button"
            ng-hide="app.running"
            ng-click="compare()"
            class="coral-Button coral-Button--primary">Compare</button>

</form>