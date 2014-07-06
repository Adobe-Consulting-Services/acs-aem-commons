
<form ng-show="data.status.state === 'not started'">

    <div class="form-row">
        <h4>JCR-SQL2 Query</h4>

        <span>
            <textarea
                    ng-required="true"
                    ng-model="form.query"
                    placeholder="SELECT * FROM [cq:Page] WHERE ISDESCENDANTNODE([/content])"></textarea>
        </span>
    </div>

    <div class="form-row">
        <h4>Workflow Model</h4>

        <span>
            <input type="text"
                   ng-required="true"
                   ng-model="form.workflowModel"
                   placeholder="/etc/workflow/models/..."/>
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