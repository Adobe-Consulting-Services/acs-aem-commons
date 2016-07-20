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
           ng-model="form.batchSize"
           value="100"
           placeholder="# of payloads to process per commit [ Default: 10 ]"/>
            <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
                  data-quicktip-content="Batch size must be greater than 1"></span>
</div>


<div class="coral-Form-fieldwrapper">
    <label class="coral-Form-fieldlabel">Retry Count</label>

    <input name="retryCount"
           type="number"
           min="0"
           class="coral-Form-field coral-Textfield"
           ng-model="form.retryCount"
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