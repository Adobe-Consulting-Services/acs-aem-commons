<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2016 Adobe
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
<%-- Failure Summary --%>
<h2 style="clear: both; margin-top: 1rem;">{{ data.status.failures.length }} Failures</h2>

<table class="coral-Table">
    <thead>
    <tr class="coral-Table-row">
        <th class="coral-Table-headerCell">Payload</th>
        <th class="coral-Table-headerCell">Time</th>
    </tr>
    </thead>
    <tbody ng-show="data.status.failures.length > 0">
    <tr class="coral-Table-row" ng-repeat="item in data.status.failures">
        <td class="coral-Table-cell">{{ item.payloadPath }}</td>
        <td class="coral-Table-cell">{{ item.failedAt }}</td>
    </tr>
    </tbody>

    <tbody ng-hide="data.status.failures.length > 0">
    <tr class="coral-Table-row">
        <td class="coral-Table-cell" colspan="2">
            <div style="text-align: center;">
                <h3 style="color: #999">No Failures!</h3>
                <em class="coral-Icon coral-Icon--thumbUp" style="font-size: 100px; line-height: 130px; color: #eee;"></em>
            </div>
        </td>
    </tr>
    </tbody>

</table>

<div style="clear: both; margin-bottom: 1rem;"></div>
