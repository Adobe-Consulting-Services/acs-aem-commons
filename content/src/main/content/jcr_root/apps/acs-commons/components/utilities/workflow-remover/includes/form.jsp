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
        ng-show="data.status.state !== 'running'"
        ng-submit="remove(params.$valid)">

    <div class="form-row">
        <h4>Statuses</h4>

        <span>
            <div class="selector">
                <label><input ng-model="form.statuses" type="checkbox" name="complete"><span>COMPLETE</span></label>
                <label><input ng-model="form.statuses" type="checkbox" name="aborted"><span>ABORTED</span></label>
                <label><input ng-model="form.statuses" type="checkbox" name="running"><span>RUNNING</span></label>
                <label><input ng-model="form.statuses" type="checkbox" name="stale"><span>STALE</span></label>
                <label><input ng-model="form.statuses" type="checkbox" name="failure"><span>FAILURE</span></label>
            </div>

            <div class="instructions">
            </div>
        </span>
    </div>


    <div class="form-row">
        <h4>Models</h4>

        <span>

            <table class="data">
                <thead>
                <tr>
                    <th class="check"><label><input type="checkbox"><span></span></label></th>
                    <th>Workflow Model</th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="workflowModel in formOptions.workflowModels">
                    <td><label><input ng-bind="{{ workflowModel.id }}" type="checkbox"><span></span></label></td>
                    <td>{{ workflowModel.title }}</td>
                    <td></td>
                </tr>
                </tbody>

            </table>

            <div class="instructions">
            </div>
        </span>
    </div>


    <div class="form-row">
        <h4>Payload Paths</h4>

        <span>
            <table class="data">
                <thead>
                <tr>
                    <th class="check"><label><input type="checkbox"><span></span></label></th>
                    <th>Payload Path Regex</th>
                </tr>
                </thead>
                <tbody>
                <tr ng-repeat="payload in form.payloads">
                    <td><input type="text" value="payload.pattern"/></td>
                    <td><a ng-click=""</td>
                </tr>
                </tbody>
                <tfoot>
                <tr>
                    <td></td>
                    <td><a ng-click="form.payloads.push({}}">Add</a></td>
                </tr>
                </tfoot>
            </table>
        </span>
    </div>


    <div class="form-row">
        <div class="form-left-cell">&nbsp;</div>

        <button type="submit"
                role="button"
                class="primary">Remove Workflows</button>
    </div>
</form>