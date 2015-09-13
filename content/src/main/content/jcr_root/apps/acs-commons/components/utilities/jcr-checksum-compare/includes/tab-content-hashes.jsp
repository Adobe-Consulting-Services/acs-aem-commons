<section class="coral-Form-fieldset">
    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Left</label>

        <select ng-model="diff.left"
                ng-options="host as host.name for host in hosts"></select>
    </div>


    <div class="coral-Form-fieldwrapper">
        <label class="coral-Form-fieldlabel">Right</label>

        <select ng-model="diff.right"
                ng-options="host as host.name for host in hosts"></select>
    </div>
</section>

<button type="submit"
        role="button"
        ng-hide="!diff.left || !diff.right || app.running"
        ng-click="compare()"
        class="coral-Button coral-Button--primary">Compare</button>

<p></p>

<div diff
     inline="true"
     base-data="diff.left"
     new-data="diff.right"></div>