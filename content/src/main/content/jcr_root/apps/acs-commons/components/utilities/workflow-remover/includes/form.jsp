<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2015 Adobe
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

<form   class="coral-Form coral-Form--vertical acs-form"
        novalidate
        name="params"
        ng-hide="app.running">

    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Workflow statuses</h3>

        <div class="coral-Form-fieldwrapper acs-checkbox-set">
            <label acs-coral-checkbox ng-repeat="status in formOptions.statuses">
                <input type="checkbox"
                       name="selectedStatuses[]"
                       value="{{status}}"
                       ng-checked="form.selection.indexOf(status) > -1"
                       ng-click="toggleStatusSelection(status)"><span>{{status}}</span></label>
        </div>

        <div class="instructions">{{data.status.status}}</div>
    </section>


    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Workflow payload paths</h3>

        <table class="coral-Table acs-table">
            <thead>
                <tr class="coral-Table-row">
                    <th class="coral-Table-headerCell">Payload path regex</th>
                    <th class="coral-Table-headerCell">&nbsp;</th>
                </tr>
            </thead>
            <tbody>
                <tr class="coral-Table-row"
                    ng-repeat="payload in form.payloads">
                    <td class="coral-Table-cell acs-table-cell">
                        <input type="text"
                               class="coral-Form-field coral-Textfield"
                               placeholder="/content/dam/.*"
                               ng-model="payload.pattern"/></td>
                    <td class="coral-Table-cell acs-table-cell-action">
                        <i ng-show="form.payloads.length > 1"
                           ng-click="form.payloads.splice($index, 1)"
                           class="coral-Icon coral-Icon--minusCircle"></i>
                    </td>
                </tr>
            </tbody>
            <tfoot>
            <tr class="coral-Table-row">
                <td colspan="2" class="coral-Table-cell property-add">
                    <span ng-click="form.payloads.push({})">
                        <i class="coral-Icon coral-Icon--addCircle withLabel"></i>
                        Add payload path pattern
                     </span>
                </td>
            </tr>
            </tfoot>
        </table>
    </section>

    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Batch size</h3>

        <p class="instructions">
            Persist removals to the JCR batches of this size. Defaults to 1000.
        </p>

        <div class="coral-InputGroup" data-init="numberinput" data-min="1">
          <span class="coral-InputGroup-button">
            <button type="button" class="js-coral-NumberInput-decrementButton coral-Button coral-Button--secondary coral-Button--square" title="Decrement">
                <i class="coral-Icon coral-Icon--sizeS coral-Icon--minus"></i>
            </button>
          </span>
          <input ng-model="form.batchSize"
                  type="text"
                  class="js-coral-NumberInput-input coral-InputGroup-input coral-Textfield">
          <span class="coral-InputGroup-button">
            <button type="button" class="js-coral-NumberInput-incrementButton coral-Button coral-Button--secondary coral-Button--square" title="Increment">
                <i class="coral-Icon coral-Icon--sizeS coral-Icon--add"></i>
            </button>
          </span>
        </div>
    </section>

    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Max duration</h3>

        <p class="instructions">
            In minutes. Force terminate this workflow process after the specified duration. Set to 0 to disable.
        </p>

        <div class="coral-InputGroup" data-init="numberinput" data-min="0">
          <span class="coral-InputGroup-button">
            <button type="button" class="js-coral-NumberInput-decrementButton coral-Button coral-Button--secondary coral-Button--square" title="Decrement">
                <i class="coral-Icon coral-Icon--sizeS coral-Icon--minus"></i>
            </button>
          </span>
            <input ng-model="form.maxDuration"
                   type="text"
                   class="js-coral-NumberInput-input coral-InputGroup-input coral-Textfield">
          <span class="coral-InputGroup-button">
            <button type="button" class="js-coral-NumberInput-incrementButton coral-Button coral-Button--secondary coral-Button--square" title="Increment">
                <i class="coral-Icon coral-Icon--sizeS coral-Icon--add"></i>
            </button>
          </span>
        </div>
    </section>


    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Workflows older than</h3>

        <div class="coral-Datepicker coral-InputGroup" data-init="datepicker">
          <input class="coral-InputGroup-input coral-Textfield" ng-model="form.olderThan" type="date">
          <span class="coral-InputGroup-button">
            <button class="coral-Button coral-Button--secondary coral-Button--square" type="button" title="Datetime Picker">
              <i class="coral-Icon coral-Icon--sizeS coral-Icon--calendar"></i>
            </button>
          </span>
        </div>
    </section>

    <section class="coral-Form-fieldset">
        <h3 class="coral-Form-fieldset-legend">Workflow models</h3>

        <div class="instructions">
            If no Workflow Models are selected, Workflow Instances will not be filtered by Workflow Model.
        </div>

        <%-- First Col --%>
       <ul class="coral-List coral-List--minimal acs-column-33-33-33">
            <li class="coral-List-item"
                ng-repeat="workflowModel in formOptions.workflowModels.slice(0, (formOptions.workflowModels.length / 3))">
                <label class="coral-Checkbox">
                    <input class="coral-Checkbox-input"
                           ng-checked="form.models.indexOf(workflowModel.id) >= 0"
                           ng-click="toggleModelSelection(workflowModel.id)"
                           type="checkbox">
                    <span class="coral-Checkbox-checkmark"></span>
                    <span class="coral-Checkbox-description">{{ workflowModel.title }}</span>
                </label>
       </ul>
        
        <%-- Second Col --%>
        <ul class="coral-List coral-List--minimal acs-column-33-33-33">
            <li class="coral-List-item"
                ng-repeat="workflowModel in formOptions.workflowModels.slice(((formOptions.workflowModels.length / 3) + 1), ((2 * formOptions.workflowModels.length) / 3))">
                <label class="coral-Checkbox">
                    <input class="coral-Checkbox-input"
                           ng-checked="form.models.indexOf(workflowModel.id) >= 0"
                           ng-click="toggleModelSelection(workflowModel.id)"
                           type="checkbox">
                    <span class="coral-Checkbox-checkmark"></span>
                    <span class="coral-Checkbox-description">{{ workflowModel.title }}</span>
                </label>
            </li>
        </ul>

        <%-- Third Col --%>
        <ul class="coral-List coral-List--minimal acs-column-33-33-33">
            <li class="coral-List-item"
                ng-repeat="workflowModel in formOptions.workflowModels.slice(((2 * formOptions.workflowModels.length) / 3 + 1), formOptions.workflowModels.length)">
                <label class="coral-Checkbox">
                    <input class="coral-Checkbox-input"
                           ng-checked="form.models.indexOf(workflowModel.id) >= 0"
                           ng-click="toggleModelSelection(workflowModel.id)"
                           type="checkbox">
                    <span class="coral-Checkbox-checkmark"></span>
                    <span class="coral-Checkbox-description">{{ workflowModel.title }}</span>
                </label>
            </li>
        </ul>        
    </section>

</form>