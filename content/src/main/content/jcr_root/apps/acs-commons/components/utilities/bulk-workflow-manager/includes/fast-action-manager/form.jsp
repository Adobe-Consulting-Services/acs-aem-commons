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


<div class="coral-Form-fieldwrapper"
    ng-show="!form.retryCount">
    <label class="coral-Form-fieldlabel">Batch Size</label>

    <input name="batchSize"
           type="number"
           min="1"
           class="coral-Form-field coral-Textfield"
           ng-model="form.batchSize"
           placeholder="# of payloads to process per commit [ Default: 10 ]"/>
</div>

<div    ng-hide="!form.retryCount"
        class="coral-Alert coral-Alert--info">
    <i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>
    <strong class="coral-Alert-title">Info</strong>
    <div class="coral-Alert-message">If Retry Count is > 0, then Batch Size is auto-set to 1</div>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Retry Count</label>

    <input name="retryCount"
           type="number"
           min="0"
           class="coral-Form-field coral-Textfield"
           ng-model="form.retryCount"
           ng-change="if (form.retryCount > 1) { form.batchSize = 0 }"
           ng-init="form.retryCount=3"
           placeholder="# of time to retry processing a payload [ Default: 0 ]"/>
            <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
                  data-quicktip-content="0 disabled retries"></span>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Retry Pause</label>

    <input name="interval"
           type="number"
           min="0"
           class="coral-Form-field coral-Textfield"
           ng-model="form.interval"
           placeholder="# of seconds between retries [ Default: 10 ]"/>
</div>


<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Set user-event-data</label>

    <select
            class="acs-select"
            ng-required="true"
            ng-model="form.selectUserEventData"
            ng-options="userEventData as userEventData.label for userEventData in formOptions.userEventData"
            ng-change="form.userEventData = form.selectUserEventData.value">
    </select>

    <br/>
    <br/>

    <input
            type="text"
            name="userEventData"
            class="coral-Form-field coral-Textfield"
            ng-model="form.userEventData"
            ng-readonly="form.selectUserEventData.value"
            placeholder="Leave blank to not set user-event-data"/>

    <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
          data-quicktip-content="user-event-data can be used to prevent WF Launchers from being invoked from the modifications made by this execution of Bulk Workflow Manager."></span>
</div>