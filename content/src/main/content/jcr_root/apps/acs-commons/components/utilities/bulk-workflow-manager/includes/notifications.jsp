<div ng-show="notifications.length > 0">
    <div ng-repeat="notification in notifications">
        <div class="alert {{ notification.type }}">
            <button class="close" data-dismiss="alert">&times;</button>
            <strong>{{ notification.title }}</strong>

            <div>{{ notification.message }}</div>
        </div>
    </div>
</div>

<div class="fixed-notifications">
    <div    ng-show="data.status.state === 'stopped'"
            class="large alert notice">
        <strong>Stopped</strong>

        <div>
            The execution of this bulk workflow process was stopped.
            The stopping may have been intentional, or may have resulted from a restart of the parent bundle/OSGi
            Component.
        </div>
    </div>


    <div    ng-show="data.status.state === 'complete'"
            class="large alert success">
        <strong>Complete</strong>

        <div>The execution of this bulk run is complete. Please review the
            <a target="_blank" href="/libs/cq/workflow/content/console.html">workflow history</a>
            for any unsuccessful Workflow executions.</div>
    </div>
</div>
