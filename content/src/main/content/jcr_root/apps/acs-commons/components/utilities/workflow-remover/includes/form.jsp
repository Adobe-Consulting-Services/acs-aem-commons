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
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%>

<form
        novalidate
        name="params"
        ng-show="!app.running"
        ng-submit="remove(params.$valid)">

    <div class="form-row">
        <h4>Statuses</h4>

        <span>
            <div class="selector">
                <label ng-repeat="status in formOptions.statuses">
                    <input type="checkbox"
                           name="selectedStatuses[]"
                           value="{{status}}"
                           ng-checked="form.selection.indexOf(status) > -1"
                           ng-click="toggleStatusSelection(status)"><span>{{status}}</span></label>
            </div>

            <div class="instructions">{{data.status.status}}
            </div>
        </span>
    </div>


    <div class="form-row">
        <h4>Models</h4>

        <span>

            <%-- First Col --%>
            <table class="data table-col-1">
                <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>Workflow Model</th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="workflowModel in formOptions.workflowModels.slice(0, formOptions.workflowModels.length / 2)">
                    <td class="action-col"><label><input
                            ng-checked="form.models.indexOf(workflowModel.id) >= 0"
                            ng-click="toggleModelSelection(workflowModel.id)"
                            type="checkbox"><span></span></label></td>
                    <td
                            ng-click="toggleModelSelection(workflowModel.id)">{{ workflowModel.title }}</td>
                </tr>

                </tbody>
            </table>

            <%-- Second Col --%>

            <table class="data table-col-2">
                <thead>
                <tr>
                    <th>&nbsp;</th>
                    <th>Workflow Model</th>
                </tr>
                </thead>
                <tbody>
                <tr
                        ng-repeat="workflowModel in formOptions.workflowModels.slice((formOptions.workflowModels.length / 2) + 1, formOptions.workflowModels.length)">
                    <td class="action-col"><label><input
                            ng-checked="form.models.indexOf(workflowModel.id) >= 0"
                            ng-click="toggleModelSelection(workflowModel.id)"
                            type="checkbox"><span></span></label></td>
                    <td
                            ng-click="toggleModelSelection(workflowModel.id)">{{ workflowModel.title }}</td>
                </tr>

                </tbody>
            </table>

            <div style="clear: both;"></div>

            <div class="instructions">
                If no Workflow Models are selected, Workflow Instances will not be filtered by Workflow Model.
            </div>
        </span>
    </div>


    <div class="form-row">
        <h4>Payload Paths</h4>

        <span>
            <table class="data">
                <thead>
                    <tr>
                        <th>Payload Path Regex</th>
                        <th>&nbsp;</th>
                    </tr>
                </thead>
                <tbody>
                    <tr ng-repeat="payload in form.payloads">
                        <td><input type="text"
                                   ng-model="payload.pattern"/></td>
                        <td class="action-col property-remove">
                            <i      ng-show="form.payloads.length > 1"
                                    ng-click="form.payloads.splice($index, 1)"
                                    class="icon-minus-circle">Remove</i>
                        </td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <td colspan="2" class="property-add">
                            <i ng-click="form.payloads.push({})"
                               class="icon-add-circle withLabel">Add Payload Path Pattern</i>
                        </td>
                    </tr>
                </tfoot>
            </table>
        </span>
    </div>

    <div class="form-row">
        <h4>Older Than</h4>

        <span>
            <input type="text"
                   ng-model="form.olderThan"/>

            <div class="instructions">
                UTC time in seconds (<a href="http://www.epochconverter.com/" target="_blank">epochconverter.com</a>)
            </div>
        </span>
    </div>


    <div class="form-row">
        <div class="form-left-cell">&nbsp;</div>

        <button type="submit"
                role="button"
                class="primary">Remove Workflows</button>
    </div>
</form>