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

    <form class="no-separator" novalidate ng-submit="saveConfig()">
        <div class="form-row">
            <h3>Enable QR Code generator</h3>

            <span>
            <label acs-coral-checkbox><input
                ng-model="form.enable"
                type="checkbox"><span>
                Enable QR code generation</span></label>
            </span>
        </div>

        <div class="form-row" qr-code-config>
            <h3>AEM Author / Publish host mappings</h3>

            <table class="coral-Table coral-Table--hover coral-Table--bordered properties-table">
                <thead>
                    <tr class="coral-Table-row">
                        <th class="coral-Table-headerCell property-name">AEM Author Host</th>
                        <th class="coral-Table-headerCell property-value">AEM Publish Host</th>
                        <th class="coral-Table-headerCell property-remove"></th>
                    </tr>
                </thead>
                <tbody>
                    <tr class="acs-commons__qr-code__mappings coral-Table-row" ng-repeat="property in form.properties">
                        <td class="coral-Table-cell property-name">
                            <input type="text" class="coral-Textfield" ng-model="property.name" placeholder="aem-author.example.com" /></td>
                        <td class="coral-Table-cell property-value">
                            <input type="text" class="coral-Textfield" ng-model="property.value" placeholder="www.example.com" /></td>
                        <td class="coral-Table-cell property-remove"><span ng-show="form.properties.length > 1" ng-click="removeProperty(form.properties, $index)">
                            <i class="coral-Icon coral-Icon--minusCircle"></i></span>
                        </td>
                    </tr>

                    <tr class="coral-Table-row">
                        <td colspan="4" class="coral-Table-cell property-add">
                            <span ng-click="addProperty(form.properties)">
                       <i class="coral-Icon coral-Icon--addCircle"></i>&nbsp;Add Host Mapping</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="form-row">
            <div class="form-left-cell">&nbsp;</div>
            <button class="coral-Button coral-Button--primary save-config">Save Configuration</button>
        </div>
    </form>
