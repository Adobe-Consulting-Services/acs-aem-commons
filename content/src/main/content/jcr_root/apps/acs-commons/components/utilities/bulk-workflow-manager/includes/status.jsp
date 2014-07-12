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

<div ng-hide="data.status.state === 'not started'">

    <div class="section summary-section">
        <h3>Summary</h3>

        <section class="well">

            <div class="left">
                <ul>
                    <li>Status: <span style="text-transform: capitalize;">{{ data.status.state }}</span></li>
                    <li>Total: {{ data.status.total }}</li>
                    <li>Complete: {{ data.status.complete }}</li>
                    <li>Remaining: {{ data.status.remaining }}</li>
                    <li>Current Batch: {{ data.status.currentBatch }}</li>

                    <li ng-show="data.status.startedAt">Started At: {{ data.status.startedAt }}</li>
                    <li ng-show="data.status.stoppedAt">Stopped At: {{ data.status.stoppedAt }}</li>
                    <li ng-show="data.status.completedAt">Competed At: {{ data.status.completedAt }}</li>
                </ul>
            </div>

            <div class="right">
                <ul>
                    <li>Batch Size: {{ data.status.batchSize }}</li>
                    <li>Workflow Model: {{ data.status.workflowModel }}</li>
                    <li>Auto Purge Batches: {{ data.status.autoPurgeWorkflows }}</li>
                </ul>
            </div>
            <div style="clear: both;"></div>
        </section>
    </div>

    <div class="section progress-section">
        <div ng-show="data.status.percentComplete || data.status.percentComplete === 0">
            <div class="progress">
                <div class="bar"
                     style="width: {{ data.status.percentComplete }}%;"></div>
            </div>
            <label>{{ data.status.percentComplete }}%</label>
        </div>
    </div>

    <div class="section button-controls-section">
        <button ng-click="stop()"
                ng-show="data.status.state === 'running'"
                class="warning">Stop Bulk Workflow</button>
        <button ng-click="resume()"
                ng-show="data.status.state === 'stopped'"
                class="primary">Resume Bulk Workflow</button>
    </div>


    <div    ng-show="data.status.state === 'running'"
            class="section current-batch-section">

        <h3>Current Batch</h3>

         <div class="status-interval">
             Refresh status table every
            <input type="text"
                   class="status-interval-input"
                   ng-blur="updatePollingInterval(form.pollingInterval)"
                   ng-model="form.pollingInterval"
                   placeholder="{{ dfault.pollingInterval }}"/> seconds
        </div>

        <table class="data current-batch-table">
            <thead>
                <tr>
                    <th class="status-col">Status</th>
                    <th>Payload</th>
                </tr>
            </thead>
            <tbody>
                <tr ng-repeat="resource in data.status.active ">
                    <td class="{{ resource.state }}">{{ resource.state || 'NOT STARTED' }}</td>
                    <td>{{ resource.path }}</td>
                </tr>
            </tbody>
        </table>
    </div>
</div>