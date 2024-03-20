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
    <label class="coral-Form-fieldlabel">Max # Sling Jobs</label>

    <input name="interval"
           type="number"
           min="1"
           class="coral-Form-field coral-Textfield"
           ng-pattern="/\d+/"
           ng-model="form.batchSize"
           ng-init="form.batchSize=100"
           placeholder="[ Default: 100 ]"/>
    <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
        data-quicktip-content="The maximum number of Sling Jobs to add to the Granite Workflow Job queue at any give time."></span>
</div>


<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Workflow Job Queue Width</label>

    <input name="interval"
           type="number"
           min="1"
           class="coral-Form-field coral-Textfield"
           ng-pattern="/\d+/"
           ng-model="calc.queueWidth"
           ng-init="calc.queueWidth=2"
           placeholder="[ Default: 2 ]"/>
    <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
        data-quicktip-content="The Granite Workflow Job Queue's width."></span>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Avg Time to Process 1 Asset (in seconds)</label>

    <input name="interval"
           type="number"
           min="1"
           class="coral-Form-field coral-Textfield"
           ng-pattern="/\d+/"
           ng-model="calc.avgTime"
           ng-init="calc.avgTime=4"
           placeholder="[ Default: 4 ]"/>
    <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
        data-quicktip-content=""></span>
</div>

<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Batch Interval (in seconds)</label>

    <input name="interval"
           type="number"
           min="1"
           class="coral-Form-field coral-Textfield"
           ng-pattern="/\d+/"
           ng-model="form.interval"
           placeholder="in seconds [ Default: 10 ]"/>
    <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
        data-quicktip-content="Computed based on above fields. ((Max # Sling Jobs) / (Workflow Job Queue Width)) * Avg Time to Process 1 Asset"></span>
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
