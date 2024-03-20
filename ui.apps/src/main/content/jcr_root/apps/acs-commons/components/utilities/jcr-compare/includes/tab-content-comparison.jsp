
<section class="coral-Form-fieldset">

    <p>Select the AEM Instances to compare using the options defined in the Configuration tab</p>


    <div class="coral-Form-fieldwrapper" style="float: left; width: 50%;">
        <label class="coral-Form-fieldlabel">Left</label>

        <select ng-model="diff.left"
                style="width: 90%"
                ng-options="host as host.name for host in hosts | filter: validHost "></select>
    </div>


    <div class="coral-Form-fieldwrapper" style="float: left; width: 45%;">
        <label class="coral-Form-fieldlabel">Right</label>

        <select ng-model="diff.right"
                style="width: 100%"
                ng-options="host as host.name for host in hosts | filter: validHost"></select>
    </div>

    <div class="clear"></div>
</section>


<button type="submit"
        role="button"
        ng-disabled="!diff.left || !diff.right || app.running"
        ng-click="compare()"
        class="coral-Button coral-Button--primary">Compare</button>

<p></p>


<div contentdiff
     left="diff.left"
     right="diff.right"></div>

<div jsondiff
     path="{{ jsonData.path }}"
     left="jsonData.left"
     right="jsonData.right"></div>