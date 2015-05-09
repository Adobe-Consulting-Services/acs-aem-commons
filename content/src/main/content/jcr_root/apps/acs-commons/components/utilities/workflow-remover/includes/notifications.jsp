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

<div class="fixed-notifications">
    <div class="running-notification"
            ng-show="app.running">
        <div class="alert large notice">
            <strong>Workflow Removal Executing</strong>
            <div>
                Please be patient as workflow removal runs. The removal status below will update removal progress.
            </div>
        </div>
    </div>
</div>

<div class="notifications" ng-show="notifications.length > 0">
    <div ng-repeat="notification in notifications">
        <div class="alert large {{ notification.type }}">
            <button class="close" data-dismiss="alert">&times;</button>
            <strong>{{ notification.title }}</strong>

            <div>{{ notification.message }}</div>
        </div>
    </div>
</div>
