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
  --%><button ng-click="stop()"
        role="button"
        class="coral-Button coral-Button--warning"
        ng-show="data.status.status === 'RUNNING' && data.status.subStatus !== 'STOPPING'">Stop Bulk Workflow</button>

<button role="button"
        class="coral-Button coral-Button--disabled"
        ng-show="data.status.subStatus === 'STOPPING'">Stopping...</button>

<button ng-click="resume()"
        role="button"
        class="coral-Button coral-Button--primary"
        ng-show="data.status.status === 'STOPPED'"
        style="float: left;"
        class="primary">Resume Bulk Workflow</button>

<div ng-show="isWorkflow()">
    <%@include file="aem-workflow/interval-update.jsp"%>
</div>

<div style="clear: both;"></div>
