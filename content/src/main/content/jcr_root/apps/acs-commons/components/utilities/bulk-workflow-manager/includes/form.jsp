
<form ng-show="data.status.state === 'not started'">

    <div class="form-row">
        <h4>JCR-SQL2 Query</h4>

        <span>
            <textarea
                    ng-required="true"
                    ng-model="form.query"
                    placeholder="SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([/content])"></textarea>

            <div class="instructions">
                Example: SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([/content])
                <br/>
                Please ensure that this query is correct prior to submitting form as it will collect the resources
                for processing which can be an expensive operation for large bulk workflow processes.
            </div>
        </span>

    </div>

    <div class="form-row">
        <h4>Workflow Model</h4>

        <span>
            <select
                    required="true"
                    ng-model="form.workflowModel"
                    ng-options="workflowModel.value as workflowModel.label for workflowModel in formOptions.workflowModels">
            </select>
        </span>
    </div>

    <div class="form-row">
        <h4>Total Size</h4>

        <span>
            <input type="text"
                   ng-required="true"
                   ng-model="form.estimatedTotal"
                   placeholder="Total size of payloads to process. If unsure, make this larger than the actual number of items to process."/>
        </span>
    </div>

    <div class="form-row">
        <h4>Batch Size</h4>

        <span>
            <input type="text"
                   ng-required="true"
                   ng-model="form.batchSize"
                   placeholder="# of payloads to process at once [ Default: 10 ]"/>
        </span>
    </div>

    <div class="form-row">
        <h4>Batch Period</h4>

        <span>
            <input type="text"
                   ng-required="false"
                   ng-model="form.period"
                   placeholder="in seconds [ Default: 10 ]"/>
            <div class="instructions">
                The minimum number of seconds to wait before trying to process the next batch.
                <br/>
                If unsure use approximately: [ Batch Size ] x  [ Seconds for 1 WF to complete ] / 2
            </div>
        </span>
    </div>

    <div class="form-row">
        <h4>Purge Workflows</h4>

        <span>
            <label><input type="checkbox" name="autoPurgeWorkflows" checked><span></span></label>
        </span>
    </div>


    <div class="form-row">
        <div class="form-left-cell">&nbsp;</div>
        <button ng-click="start()"
                ng-show="data.status.state === 'not started'"
                class="primary">Start Bulk Workflow</button>
    </div>
</form>