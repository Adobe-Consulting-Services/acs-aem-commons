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

<div ng-show="notifications.length > 0">
    <div ng-repeat="notification in notifications">
        <div acs-coral-alert
             data-alert-type="{{ notification.type }}"
             data-alert-size="large"
             data-alert-title="{{ notification.title }}"
             data-alert-message="{{ notification.message }}">
        </div>
    </div>
</div>

<div class="fixed-notifications">
    <div    ng-show="data.status.state === 'stopped'"
            acs-coral-alert
            data-alert-type="notice"
            data-alert-size="large"
            data-alert-title="Stopped">
            The execution of this bulk workflow process was stopped.
            Press the &quot;Resume Bulk Worklfow&quot; button below to resume bulk workflow processing.
    </div>


    <div    ng-show="data.status.state === 'complete'"
            acs-coral-alert
            data-alert-type="success"
            data-alert-size="large"
            data-alert-title="Complete">
        The execution of this bulk run is complete. Please review the
            <a target="_blank" href="/libs/cq/workflow/content/console.html">workflow history</a>
            for any unsuccessful Workflow executions.
    </div>
</div>
