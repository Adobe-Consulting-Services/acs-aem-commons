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
<%-- Status Boxes --%>
<div    ng-show="data.status.status === 'STOPPED'"
        acs-coral-alert
        data-alert-type="notice"
        data-alert-size="large"
        data-alert-title="Stopped">
    The execution of this bulk workflow process was stopped.
    <br/>
    <br/>
    Press the &quot;Resume Bulk Worklfow&quot; button below to resume bulk workflow processing.
</div>

<div    ng-show="data.status.status === 'COMPLETED'"
        acs-coral-alert
        data-alert-type="success"
        data-alert-size="large"
        data-alert-title="Complete">
    The execution of this bulk run is complete.
    <span ng-show="data.status.failures.length > 0">Please review the list below to identify any payloads that could not be processed.</span>
    <br/>
    <br/>
    To execute other workflow in bulk, create a new Bulk Workflow Manager page.
</div>

<%-- Status Summary --%>
<h2>Bulk Workflow Execution Summary</h2>

<div style="width: 48%; padding-right: 2%; float: left;">
    <table class="coral-Table coral-Table--bordered">
        <tbody>
            <tr class="coral-Table-row">
                <td class="coral-Table-cell">Runner</td>
                <td class="coral-Table-cell">Synthetic Workflow</td>
            </tr>

            <tr class="coral-Table-row">
                <td class="coral-Table-cell">Status</td>
                <td class="coral-Table-cell">
                    {{ data.status.status }}
                    <span ng-show="data.status.subStatus && data.status.subStatus !== 'SLEEPING'"> ( {{ data.status.subStatus }} )</span>
            </tr>

            <tr class="coral-Table-row">
                <td class="coral-Table-cell">Completed</td>
                <td class="coral-Table-cell">{{ data.status.completeCount }}</td>
            </tr>

            <tr class="coral-Table-row">
                <td class="coral-Table-cell">Failed</td>
                <td class="coral-Table-cell">{{ data.status.failCount }}</td>
            </tr>

            <tr class="coral-Table-row" ng-show="data.status.remainingCount > 0">
                <td class="coral-Table-cell">Remaining</td>
                <td class="coral-Table-cell">{{ data.status.remainingCount }}</td>
            </tr>

            <tr class="coral-Table-row">
                <td class="coral-Table-cell">Total</td>
                <td class="coral-Table-cell">{{ data.status.totalCount }}</td>
            </tr>

            <tr class="coral-Table-row">
                <td class="coral-Table-cell">
                <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="bottom"
                      data-quicktip-content="When true, execution is throttled via 'ACS AEM Commons - Throttled Task Runner Service' configurable via OSGi configuration"></span>
                    Auto-Throttle</td>
                <td class="coral-Table-cell">{{ data.status.autoThrottle }}</td>
            </tr>


            <tr class="coral-Table-row"
                ng-show="data.status.startedAt">
                <td class="coral-Table-cell">
                <span   ng-show="data.status.autoThrottle"
                        class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="bottom"
                        data-quicktip-content="CPU usage throttled via 'ACS AEM Commons - Throttled Task Runner Service' configurable via OSGi configuration"></span>
                    CPU Usage
                </td>
                <td class="coral-Table-cell">{{ data.status.systemStats.cpu }} <span ng-show="data.status.autoThrottle">/ {{ data.status.systemStats.maxCpu }}</span></td>
            </tr>

            <tr class="coral-Table-row"
                ng-show="data.status.startedAt">
                <td class="coral-Table-cell">
                    <span   ng-show="data.status.autoThrottle"
                            class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="top"
                            data-quicktip-content="Memory (Heap) usage throttled via 'ACS AEM Commons - Throttled Task Runner Service' configurable via OSGi configuration"></span>
                    Memory (Heap) Usage
                </td>
                <td class="coral-Table-cell">{{ data.status.systemStats.mem }} <span ng-show="data.status.autoThrottle">/ {{ data.status.systemStats.maxMem }}</span></td>
            </tr>

        </tbody>
    </table>
</div>

<div style="width: 48%; padding-left: 2%; float: left;">
    <table class="coral-Table coral-Table--bordered">
    <tbody>
        <tr class="coral-Table-row">
            <td class="coral-Table-cell">Query Type</td>
            <td class="coral-Table-cell" style="text-transform: capitalize;">{{ data.status.queryType }}</td>
        </tr>

        <tr class="coral-Table-row" ng-hide="data.status.queryType === 'list'">
            <td class="coral-Table-cell">Query Statement</td>
            <td class="coral-Table-cell" style="white-space: pre;">{{ data.status.queryStatement }}</td>
        </tr>

        <tr class="coral-Table-row">
            <td class="coral-Table-cell">Batch Size</td>
            <td class="coral-Table-cell">{{ data.status.batchSize }}</td>
        </tr>

        <tr class="coral-Table-row">
            <td class="coral-Table-cell">Workflow Model</td>
            <td class="coral-Table-cell">{{ data.status.workflowModel }}</td>
        </tr>

        <tr class="coral-Table-row"
            ng-show="data.status.startedAt">
            <td class="coral-Table-cell">Started At</td>
            <td class="coral-Table-cell">{{ data.status.startedAt }}</td>
        </tr>

        <tr class="coral-Table-row"
            ng-show="data.status.stoppedAt && !data.status.completedAt">
            <td class="coral-Table-cell">Stopped At</td>
            <td class="coral-Table-cell">{{ data.status.stoppedAt }}</td>
        </tr>

        <tr class="coral-Table-row"
            ng-show="data.status.completedAt">
            <td class="coral-Table-cell">Completed At</td>
            <td class="coral-Table-cell">{{ data.status.completedAt }}</td>
        </tr>

        <tr class="coral-Table-row"
            ng-show="data.status.timeTakenInMillis">
            <td class="coral-Table-cell">Time Taken</td>
            <td class="coral-Table-cell">{{ timeTaken() }}</td>
        </tr>

        <tr class="coral-Table-row"
            ng-show="data.status.timeTakenInMillis && !data.status.completedAt">
            <td class="coral-Table-cell">Projected Time Remaining</td>
            <td class="coral-Table-cell">{{ projectedTimeRemaining() }}</td>
        </tr>
        </tbody>
    </table>
</div>
