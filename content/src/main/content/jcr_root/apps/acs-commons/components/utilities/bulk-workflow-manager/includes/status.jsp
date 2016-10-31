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
<div    ng-show="data.status.status === 'RUNNING'"
        acs-coral-alert
        data-alert-type="info"
        data-alert-size="large"
        data-alert-title="Running">
    Please be patient while Bulk Workflow executes.
</div>

<section>
    <div ng-show="isWorkflow()">
        <%@include file="aem-workflow/status.jsp"%>
    </div>

        <div ng-show="isTransientWorkflow()">
            <%@include file="aem-transient-workflow/status.jsp"%>
        </div>

    <div ng-show="isSynthetic()">
        <%@include file="synthetic-workflow/status.jsp"%>
    </div>

    <div ng-show="isFAM()">
        <%@include file="fast-action-manager/status.jsp"%>
    </div>

    <%-- Progress Bar --%>
    <div class="coral-Progress acs-progress-bar"
         style="margin: 1rem 0"
         ng-show="data.status.percentComplete || data.status.percentComplete === 0">
        <div class="coral-Progress-bar">
            <div class="coral-Progress-status"
                 style="width: {{ data.status.percentComplete }}%;"></div>
        </div>
        <label class="coral-Progress-label">{{ data.status.percentComplete }}%</label>
    </div>
</section>

<%-- Controls --%>
<section class="acs-section">
    <%@include file="controls.jsp"%>
</section>

<%-- Running Payloads Table --%>
<section  ng-show="data.status.status === 'RUNNING'">
    <hr/>

    <div class="acs-section" style="line-height: 2.5rem;">
        Refresh status every
        <input type="text"
               style="width: 5rem"
               class="coral-Form-field coral-Textfield"
               ng-blur="updatePollingInterval(form.pollingInterval)"
               ng-model="form.pollingInterval"
               placeholder="{{ dfault.pollingInterval }}"/> seconds, or

        <button ng-click="status(true)"
                role="button"
                class="coral-Button inline-button">Refresh now</button>
    </div>


    <div ng-show="isWorkflow()">
        <%@include file="aem-workflow/status-table.jsp"%>
    </div>

    <div ng-show="isSynthetic()">
        <%@include file="synthetic-workflow/status-table.jsp"%>
    </div>

    <div ng-show="isTransientSynthetic()">
        <%@include file="aem-transient-workflow/status-table.jsp"%>
    </div>

    <div ng-show="isFAM()">
        <%@include file="fast-action-manager/status-table.jsp"%>
    </div>
</section>

<!-- Failure Table -->
<section
    style="text-align: center;"
    ng-show="isTransientWorkflow()">
    AEM Transient workflow does not track failures. Please review the logs and/or audit the Workflow application.
</section>

<section ng-hide="isTransientWorkflow()">
    <%@include file="failures-table.jsp"%>
</section>
