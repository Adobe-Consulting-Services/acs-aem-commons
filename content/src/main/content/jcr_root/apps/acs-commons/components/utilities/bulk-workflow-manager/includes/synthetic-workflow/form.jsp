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

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Batch Size</label>

    <input name="batchSize"
           type="number"
           min="1"
           class="coral-Form-field coral-Textfield"
           ng-model="form.batchSize"
           placeholder="# of payloads to process per commit [ Default: 10 ]"/>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Auto-Throttle</label>

    <label acs-coral-checkbox>
        <input type="checkbox"
               name="autoThrottle"
               ng-model="form.autoThrottle"
               ng-init="form.autoThrottle=true">
        <span>Enable <a target="_blank" href="/system/console/configMgr/com.adobe.acs.commons.fam.impl.ThrottledTaskRunnerImpl" x-cq-linkchecker="skip">Throttled Task Runner</a> CPU/Memory-based throttling.</span>
    </label>
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