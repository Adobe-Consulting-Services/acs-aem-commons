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
        <div class="coral-Alert coral-Alert--large coral-Alert--{{ notification.type }}">
            <button type="button" class="coral-MinimalButton coral-Alert-closeButton" title="Close" data-dismiss="alert">
                <i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i>
            </button>
            <i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>
            <strong class="coral-Alert-title">{{ notification.title }}</strong>
            <div class="coral-Alert-message">{{ notification.message }}</div>
        </div>
    </div>
</div>

<div class="fixed-notifications">
    <div    ng-show="data.status.state === 'stopped'"
            class="coral-Alert coral-Alert--large coral-Alert--notice">
        <button type="button" class="coral-MinimalButton coral-Alert-closeButton" title="Close" data-dismiss="alert">
            <i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i>
        </button>
        <strong class="coral-Alert-title">Stopped</strong>

        <div class="coral-Alert-message">
            The execution of this bulk workflow process was stopped.
            Press the &quot;Resume Bulk Worklfow&quot; button below to resume bulk workflow processing.
        </div>
    </div>


    <div    ng-show="data.status.state === 'complete'"
            class="coral-Alert coral-Alert--large coral-Alert--success">
        <button type="button" class="coral-MinimalButton coral-Alert-closeButton" title="Close" data-dismiss="alert">
            <i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i>
        </button>
        <strong class="coral-Alert-title">Complete</strong>

        <div class="coral-Alert-message">The execution of this bulk run is complete. Please review the
            <a target="_blank" href="/libs/cq/workflow/content/console.html">workflow history</a>
            for any unsuccessful Workflow executions.</div>
    </div>
</div>
