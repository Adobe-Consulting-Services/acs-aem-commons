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
            <h4 acs-coral-heading>Enable QR Code Generator</h4>

            <span>
            <label acs-coral-checkbox><input
                ng-model="form.enable"
                type="checkbox"><span>
           Enabling this will allow you to generate QR code for URL</span></label>
            </span>
        </div>

        <div class="form-row" qr-code-config>
            <h4>Multiple AEM Environments</h4>

            <table class="coral-Table coral-Table--hover coral-Table--bordered properties-table">
                <thead>
                    <tr class="coral-Table-row">
                        <th class="coral-Table-headerCell property-name">Author</th>
                        <th class="coral-Table-headerCell property-value">Publish</th>
                        <th class="coral-Table-headerCell property-remove"></th>
                    </tr>
                </thead>
                <tbody>
                    <tr class="coral-Table-row" ng-repeat="property in form.properties">
                        {{property.name}}
                        <td class="coral-Table-cell property-name"><input type="text" class="coral-Textfield" ng-model="property.name" placeholder="Author Host URL" /></td>

                        <td class="coral-Table-cell property-value"><input type="text" class="coral-Textfield" ng-model="property.value" placeholder="Publish Host URL" /></td>

                        <td class="coral-Table-cell property-remove"><span ng-show="form.properties.length > 1" ng-click="removeProperty(form.properties, $index)">
                            <i class="coral-Icon coral-Icon--minusCircle"></i>&nbsp;Remove</span>
                        </td>
                    </tr>

                    <tr class="coral-Table-row">
                        <td colspan="4" class="coral-Table-cell property-add">
                            <span ng-click="addProperty(form.properties)">
                       <i class="coral-Icon coral-Icon--addCircle"></i>&nbsp;Add Property</span>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>
        <div class="form-row">
            <div class="form-left-cell">&nbsp;</div>
            <button class="coral-Button coral-Button--primary save-config">Save Configurations</button>
        </div>
    </form>
