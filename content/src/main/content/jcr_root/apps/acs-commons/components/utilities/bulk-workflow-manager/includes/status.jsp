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
                    <li>Current Batch Timeout: {{ data.status.batchTimeoutCount }}
                            of {{ data.status.batchTimeout }}</li>

                    <li ng-show="data.status.startedAt">Started At: {{ data.status.startedAt }}</li>
                    <li ng-show="data.status.stoppedAt && !data.status.completedAt">Stopped At: {{ data.status.stoppedAt }}</li>
                    <li ng-show="data.status.completedAt">Competed At: {{ data.status.completedAt }}</li>
                </ul>
            </div>

            <div class="right">
                <ul>
                    <li>Batch Size: {{ data.status.batchSize }}</li>
                    <li>Batch Timeout: {{ data.status.batchTimeout * data.status.interval }} seconds
                        ( Multiplier: {{ data.status.batchTimeout }} )
                    </li>
                    <li>Batch Interval: {{ data.status.interval }} seconds</li>
                    <li>Workflow Model: {{ data.status.workflowModel }}</li>
                    <li>Purge Workflow: {{ data.status.purgeWorkflow }}</li>
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
                ng-show="data.status.state.indexOf('stopped') === 0"
                style="float: left;"
                class="primary">Resume Bulk Workflow</button>

        <div    class="inline-input-wrapper"
                style="margin-left: 15em"
                ng-show="data.status.state.indexOf('stopped') === 0">
            Update batch interval to

            <input type="text"
                   class="inline-input"
                   ng-required="false"
                   ng-model="form.interval"
                   placeholder="{{ form.interval }}"/>
            seconds.
        </div>

    </div>


    <div    ng-show="data.status.state === 'running'"
            class="section current-batch-section">

        <h3>Current Batch</h3>

         <div class="inline-input-wrapper">
             Refresh status every
            <input type="text"
                   class="inline-input"
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
                <tr ng-repeat="item in data.status.currentBatchItems ">
                    <td class="{{ item.state }}">{{ item.state || 'NOT STARTED' }}</td>
                    <td>{{ item.path }}</td>
                </tr>
            </tbody>
        </table>
    </div>
</div>