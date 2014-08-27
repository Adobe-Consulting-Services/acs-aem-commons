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
        ng-show="data.status.state === 'not started'"
        ng-submit="start(params.$valid)">

    <div class="form-row">
        <h4>JCR-SQL2 Query</h4>

        <span>
            <textarea
                    name="query"
                    ng-required="true"
                    ng-model="form.query"
                    ng-pattern="/^SELECT\s.*/"
                    placeholder="SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([/content])"></textarea>

            <div class="instructions">
                Example: SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([/content])
                <br/>
                <br/>
                Ensure that this query is correct prior to submitting form as it will collect the resources
                for processing which can be an expensive operation for large bulk workflow processes.
            </div>
        </span>
    </div>


    <div class="form-row">
        <h4>Search Node Rel Path</h4>

        <span>
            <input
                   type="text"
                   ng-model="form.relativePath"
                   placeholder="Relative path to append to search results [ Optional ]"/>
            <div class="instructions">
                Examples: jcr:content/renditions/original OR ../renditions/original
                <br/>
                This can be used to select otherwise difficult to search for resources.
            </div>
        </span>
    </div>


    <div class="form-row">
        <h4>Workflow Model</h4>

        <span>
            <select
                    name="workflowModel"
                    ng-required="true"
                    ng-model="form.workflowModel"
                    ng-options="workflowModel.value as workflowModel.label for workflowModel in formOptions.workflowModels">
            </select>
        </span>
    </div>

    <div class="form-row">
        <h4>Total Size</h4>

        <span>
            <input name="estimatedTotal"
                   type="text"
                   ng-required="true"
                   ng-pattern="/\d+/"
                   ng-model="form.estimatedTotal"
                   placeholder="Total number of payloads to process. If unsure, make this larger than the actual number of items to process."/>
        </span>
    </div>

    <div class="form-row">
        <h4>Batch Size</h4>

        <span>
            <input name="batchSize"
                   type="text"
                   ng-required="true"
                   ng-pattern="/(^[2-9]\d*)|(^[1-9]\d+)/"
                   ng-model="form.batchSize"
                   placeholder="# of payloads to process at once [ Default: 10 ]"/>

            <div class="instructions">
                Batch size must be greater than 1
            </div>
        </span>
    </div>

    <div class="form-row">
        <h4>Batch Interval</h4>

        <span>
            <input name="interval"
                   type="text"
                   ng-pattern="/\d+/"
                   ng-model="form.interval"
                   placeholder="in seconds [ Default: 10 ]"/>
            <div class="instructions">
                The minimum number of seconds to wait before trying to process the next batch.
                <br/>
                If unsure: [ Batch Size ] x [ Seconds for One WF to Complete ] / 2
            </div>
        </span>
    </div>

    <div class="form-row">
        <h4>Batch Timeout</h4>

        <span>
            <input name="batchTimeout"
                   type="text"
                   ng-pattern="/\d*/"
                   ng-model="form.batchTimeout"
                   placeholder="Number of batch intervals to wait for entire batch to complete [ Default: 20 ]"/>
            <div class="instructions">
                Any active workflows in a batch after this duration will be terminated and marked as "FORCE
                TERMINATED".
                <br/>
                [ Time to Process One Batch ] x [ Batch Interval ] should be sufficient for all workflows to complete
                for the entire batch under normal conditions.
            </div>
        </span>
    </div>

    <div class="form-row">
        <h4>Purge Workflows</h4>

        <span>
            <label><input
                    type="checkbox"
                    name="purgeWorkflows"
                    ng-model="form.checkWorkflow"
                    checked><span>Delete completed workflow instances after each batch is processed.</span></label>
        </span>
    </div>


    <div class="form-row"
            ng-show="data.status.state === 'not started'">

        <div class="form-left-cell">&nbsp;</div>

        <button type="submit"
                ng-show="params.$valid && !params.$pristine"
                class="primary">Start Bulk Workflow</button>

        <button type="submit"
                ng-show="params.$invalid || params.$pristine"
                disabled>Start Bulk Workflow</button>

    </div>
</form>