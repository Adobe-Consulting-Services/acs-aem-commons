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
        ng-submit="start(params.$valid)"
        class="coral-Form coral-Form--vertical">

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">JCR-SQL2 Query</label>
    
        <textarea
                class="coral-Form-field coral-Textfield coral-Textfield--multiline"
                name="query"
                ng-required="true"
                ng-model="form.query"
                ng-pattern="/^SELECT\s.*/"
                placeholder="SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([/content])"></textarea>
    
        <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
            data-quicktip-content="Ensure that this query is correct prior to submitting form as it will collect the resources for processing which can be an expensive operation for large bulk workflow processes. Example: SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([/content])"></span>
    </div>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Search Node Rel Path</label>
        
        <input
           type="text"
           class="coral-Form-field coral-Textfield"
           ng-model="form.relativePath"
           placeholder="Relative path to append to search results [ Optional ]"/>
        <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
            data-quicktip-content="This can be used to select otherwise difficult to search for resources. Examples: jcr:content/renditions/original OR ../renditions/original"></span>
    </div>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Workflow Model</label>

        <select
                name="workflowModel"
                ng-required="true"
                ng-model="form.workflowModel"
                ng-options="workflowModel.value as workflowModel.label for workflowModel in formOptions.workflowModels">
        </select>
    </div>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Total Size</label>

        <input name="estimatedTotal"
               type="text"
               class="coral-Form-field coral-Textfield"
               ng-required="true"
               ng-pattern="/\d+/"
               ng-model="form.estimatedTotal"
               placeholder="Total number of payloads to process. If unsure, make this larger than the actual number of items to process."/>
    </div>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Batch Size</label>

        <input name="batchSize"
               type="text"
               class="coral-Form-field coral-Textfield"
               ng-required="true"
               ng-pattern="/(^[2-9]\d*)|(^[1-9]\d+)/"
               ng-model="form.batchSize"
               placeholder="# of payloads to process at once [ Default: 10 ]"/>
        <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
            data-quicktip-content="Batch size must be greater than 1"></span>
    </div>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Batch Interval</label>

        <input name="interval"
               type="text"
               class="coral-Form-field coral-Textfield"
               ng-pattern="/\d+/"
               ng-model="form.interval"
               placeholder="in seconds [ Default: 10 ]"/>
        <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
            data-quicktip-content="The minimum number of seconds to wait before trying to process the next batch. If unsure: [ Batch Size ] x [ Seconds for One WF to Complete ] / 2"></span>
    </div>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Batch Timeout</label>

        <input name="batchTimeout"
               type="text"
               class="coral-Form-field coral-Textfield"
               ng-pattern="/\d*/"
               ng-model="form.batchTimeout"
               placeholder="Number of batch intervals to wait for entire batch to complete [ Default: 20 ]"/>
        <span class="coral-Form-fieldinfo coral-Icon coral-Icon--infoCircle coral-Icon--sizeS" data-init="quicktip" data-quicktip-type="info" data-quicktip-arrow="right"
            data-quicktip-content="Any active workflows in a batch after this duration will be terminated and marked as \"FORCE TERMINATED\". [ Time to Process One Batch ] x [ Batch Interval ] should be sufficient for all workflows to complete for the entire batch under normal conditions."></span>
    </div>

    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Purge Workflows</label>

        <label class="coral-Checkbox">
            <input class="coral-Checkbox-input" type="checkbox"
                    name="purgeWorkflows"
                    ng-model="form.checkWorkflow"
                    checked>
            <span class="coral-Checkbox-checkmark"></span>
            <span class="coral-Checkbox-description">Delete completed workflow instances after each batch is processed.</span>
        </label>
    </div>


    <div class="coral-Form-fieldwrapper"
            ng-show="data.status.state === 'not started'">

        <button type="submit"
                role="button"
                class="coral-Button coral-Button--primary"
                ng-show="params.$valid && !params.$pristine"
                class="primary">Start Bulk Workflow</button>

        <button type="submit"
                role="button"
                class="coral-Button coral-Button--primary"
                ng-show="params.$invalid || params.$pristine"
                disabled>Start Bulk Workflow</button>

    </div>
</form>