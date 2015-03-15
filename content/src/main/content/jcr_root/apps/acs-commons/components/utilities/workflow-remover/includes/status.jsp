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

<div ng-show="app.running || status.status === 'complete'">

    <div class="section summary-section">
        <h3>Workflow Removal Status</h3>

        <section class="well">

            <h4 ng-show="!app.running &&  status.status === 'complete'">Previous Workflow Removal Status</h4>

            <ul>
                <li>Workflow instance removal status
                    : <span style="text-transform: capitalize;">{{ status.status || 'Not Started'}}</span></li>

                <li ng-show="app.running">WF instances checked: {{ status.checkedCount || 0 }}</li>
                <li ng-hide="app.running">Total WF instances checked: {{ status.checkedCount || 0 }}</li>

                <li ng-show="app.running">WF instances removed : {{ status.count || 0 }}</li>
                <li ng-hide="app.running">Total WF instances removed: {{ status.count || 0 }}</li>

                <li ng-show="status.initiatedBy">Initiated by: {{ status.initiatedBy }}</li>

                <li ng-show="status.startedAt">Started at: {{ status.startedAt }}</li>

                <li ng-show="status.completedAt">Started at: {{ status.completedAt }}</li>

                <li>Time taken: {{ status.timeTaken }} seconds</li>

            </ul>

        </section>
    </div>

</div>