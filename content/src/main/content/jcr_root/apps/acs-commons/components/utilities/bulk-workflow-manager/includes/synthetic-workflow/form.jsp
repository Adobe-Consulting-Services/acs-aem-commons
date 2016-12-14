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