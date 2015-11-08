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

<%-- Running --%>
<section class="coral-Well" ng-show="app.running">
    
    <h4 acs-coral-heading>Workflow removal status: RUNNING</h4>

    <ul acs-coral-list>
        <li>WF instances checked: {{ status.checkedCount || 0 }}</li>
        <li>WF instances removed : {{ status.removedCount || 0 }}</li>
        <li>Initiated by: {{ status.initiatedBy }}</li>
        <li>Started at: {{ status.startedAt }}</li>
        <li>Duration: {{ status.duration }} seconds</li>
    </ul>

</section>

<%-- Finished: Completed or Errored --%>
<section class="coral-Well" ng-show="!app.running && (status.erredAt || status.completedAt)">
    <h4 acs-coral-heading>Workflow removal status: {{ status.erredAt ? 'ERROR' : 'COMPLETED' }}</h4>

    <ul acs-coral-list>
        <li>WF instances checked: {{ status.checkedCount || 0 }}</li>
        <li>WF instances removed : {{ status.removedCount || 0 }}</li>
        <li>Initiated by: {{ status.initiatedBy }}</li>
        <li>Started at: {{ status.startedAt }}</li>
        <li ng-show="status.erredAt">Erred at: {{ status.erredAt }}</li>
        <li ng-show="status.completedAt">Completed at: {{ status.completedAt }}</li>
        <li>Duration: {{ status.duration }} seconds</li>
    </ul>
</section>