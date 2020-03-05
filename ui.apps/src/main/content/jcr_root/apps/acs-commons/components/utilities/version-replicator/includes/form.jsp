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
                 java.util.ArrayList,
                 com.day.cq.replication.Agent"
%><%

    final AgentManager agentManager = sling.getService(AgentManager.class);
    final Collection<Agent> agents = agentManager.getAgents().values();

    final Collection<String> agentResourcePaths = new ArrayList<String>();
    for (final Agent agent: agents) {
        agentResourcePaths.add(resourceResolver.map(slingRequest, agent.getConfiguration().getConfigPath()));
    }
    pageContext.setAttribute("agents", agents);
    pageContext.setAttribute("agentResourcePaths", agentResourcePaths);
%>
<form class="coral-Form coral-Form--vertical acs-form"
        novalidate
        id="versionReplicator"
        ng-hide="app.running"
        ng-sumbit="replicate()">
    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Root paths</h3>
        <p class="instructions">
            Select the root paths to replicate. Resource versions matching the specified date & time will be replicated.
        </p>
        <table class="coral-Table acs-table">
            <tbody>
                <tr class="coral-Table-row"
                    ng-repeat="rootPath in form.rootPaths">
                    <td class="coral-Table-cell acs-table-cell">
                        <input type="text"
                               name="rootPaths"
                               class="coral-Form-field coral-Textfield"
                               placeholder="Enter a root path"
                               ng-model="form.rootPaths.path"/>
                    </td>
                    <td class="coral-Table-cell acs-table-cell-action">
                        <i ng-show="form.rootPaths.length >= 1"
                           ng-click="form.rootPaths.splice($index, 1)"
                           class="coral-Icon coral-Icon--minusCircle"></i>
                    </td>
                </tr>
            </tbody>
            <tfoot>
            <tr class="coral-Table-row">
                <td colspan="2" class="coral-Table-cell property-add">
                    <span ng-click="form.rootPaths.push({})">
                        <i class="coral-Icon coral-Icon--addCircle withLabel"></i>
                        Add a root path
                     </span>
                </td>
            </tr>
            </tfoot>
        </table>
    </section>

    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Version date & time</h3>
        <p class="instructions">
            Select the date and time used to derive the correct resource version to replicate.
        </p>
        <div class="coral-Datepicker coral-InputGroup" data-init="datepicker">
              <input class="coral-InputGroup-input coral-Textfield"
                     type="datetime"
                     name="datetimecal"
                     ng-model="datetimecal"
                     valueFormat="YYYY-MM-DD[T]HH:mm a"
                     displayFormat="YYYY-MM-DD[T]HH:mm a">
              <span class="coral-InputGroup-button">
                <button class="coral-Button coral-Button--secondary coral-Button--square" type="button" title="Datetime Picker">
                  <i class="coral-Icon coral-Icon--sizeS coral-Icon--calendar"></i>
                </button>
              </span>
        </div>
    </section>
    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Replication agents</h3>
        <div class="instructions">
            Select 1 or more replication agents target for this replication.
        </div>
        <ul id="cmbAgent" class="coral-List coral-List--minimal acs-column-33-33-33">
            <c:forEach var="agent" items="${agents}" varStatus="loop">
                 <c:set var="index">${loop.index}</c:set>
                 <c:if test="${agent.valid && agent.enabled}">
                     <li class="coral-List-item">
                        <label class="coral-Checkbox">
                            <input class="coral-Checkbox-input"
                                   name="cmbAgent" value="${agent.id}"
                                   ng-checked="form.agents.indexOf('${agent.id}') >= 0"
                                   ng-click="toggleModelSelection('${agent.id}')"
                                   type="checkbox"
                                   data-agent-path="${agentResourcePaths[loop.index]}"
                                   data-agent-name="${agent.configuration.name}">
                            <span class="coral-Checkbox-checkmark"></span>
                            <span class="coral-Checkbox-description">${agent.configuration.name}</span>
                        </label>
                 </c:if>
            </c:forEach>
    </section>
</form>