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
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>
<form class="no-separator" novalidate>
    <div class="form-row">
        <div class="form-left-cell">&nbsp;</div>
        <button
                ng-show="form.enabled"
                ng-click="form.enabled = false; saveConfig();"
                is="coral-button" variant="warning" icon="closeCircle" iconsize="S">
            Disable Instant Package Feature
        </button>

        <button
                ng-hide="form.enabled"
                ng-click="form.enabled = true; saveConfig();"
                is="coral-button" variant="primary" icon="checkCircle" iconsize="S">
            Enable Instant Package Feature
        </button>

    </div>
</form>
