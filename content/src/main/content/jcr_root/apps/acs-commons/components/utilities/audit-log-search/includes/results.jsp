<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2017 Adobe
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
<%@include file="/libs/foundation/global.jsp" %>
<%
com.day.cq.rewriter.linkchecker.LinkCheckerSettings.fromRequest(slingRequest).setIgnoreInternals(true);
%>
<div class="section">
	<h2 acs-coral-heading><span ng-show="result.count > 0">Found {{result.count}}</span> Audit Events <span ng-show="result.count > 0">in {{result.time}}ms</span></h2>
</div>
<div class="section audit-log-search-results" ng-show="result.count > 0">
	<table class="coral-Table coral-Table--hover data">
	
		<thead>
			<tr class="coral-Table-row">
				<th class="coral-Table-headerCell">Path</th>
				<th class="coral-Table-headerCell">Type</th>
				<th class="coral-Table-headerCell">User</th>
				<th class="coral-Table-headerCell">Time</th>
				<th class="coral-Table-headerCell">Modifications</th>
				<th class="coral-Table-headerCell">CRXDE</th>
			</tr>
		</thead>
		<tr ng-repeat="event in result.events" class="coral-Table-row">
			<td class="coral-Table-cell">
				<a target="_blank" href="/crx/de/index.jsp{{'#'+event.path}}" class="coral-Link">
					{{event.path}}
				</a>
			</td>
			<td class="coral-Table-cell">
				{{event.type}}
			</td>
			<td class="coral-Table-cell">
				<a class="coral-Link" target="_blank" href="/libs/granite/security/content/userEditor.html{{event.userPath }}">
					{{ event.userName }} <small>({{ event.userId }})</small>
				</a>
			</td>
			<td class="coral-Table-cell">
				{{event.time | date:'yyyy-MM-dd'}}&nbsp;{{event.time | date:'HH:mm:ss'}}&nbsp;{{event.time | date:'Z'}}
			</td>
			<td class="coral-Table-cell">
				<div ng-repeat="modified in event.modified">
						{{modified}}
				</div>
			</td>
			<td class="coral-Table-cell">
				<a target="_blank" href="/crx/de/index.jsp{{'#'+event.eventPath}}" class="coral-Button coral-Button--square">
					<i class="coral-Icon coral-Icon--gear coral-Icon--sizeS"></i>
				</a>
			</td>
		</tr>
	</table>
</div>