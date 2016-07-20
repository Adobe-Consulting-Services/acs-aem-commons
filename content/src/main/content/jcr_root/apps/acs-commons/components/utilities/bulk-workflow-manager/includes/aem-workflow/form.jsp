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
           min="2"
           class="coral-Form-field coral-Textfield"
           ng-pattern="/(^[2-9]\d*)|(^[1-9]\d+)/"
           ng-model="form.batchSize"
           placeholder="# of payloads to process at once [ Default: 10 ]"/>
            <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
                  data-quicktip-content="Batch size must be greater than 1"></span>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Batch Interval</label>

    <input name="interval"
           type="number"
           min="1"
           class="coral-Form-field coral-Textfield"
           ng-pattern="/\d+/"
           ng-model="form.interval"
           placeholder="in seconds [ Default: 10 ]"/>
    <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
        data-quicktip-content="The minimum number of seconds to wait before trying to process the next batch. If unsure: [ Batch Size ] x [ Seconds for One WF to Complete ] / 2"></span>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Timeout (in seconds)</label>

    <input name="timeout"
           type="number"
           min="0"
           class="coral-Form-field coral-Textfield"
           ng-pattern="/\d*/"
           ng-model="form.timeout"
           placeholder="Amount of time to wait for each workflow to finish in seconds. 0 to disable. [ Default: 30 ]"/>
    <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
        data-quicktip-content="Any active workflows that are still active after this amount of time will be marked as 'FORCE TERMINATED'."></span>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Purge Workflows</label>

    <label acs-coral-checkbox>
        <input type="checkbox"
               name="purgeWorkflow"
               ng-model="form.purgeWorkflow"
               checked>
        <span>Delete completed workflow instances after each batch is processed.</span>
    </label>
</div>