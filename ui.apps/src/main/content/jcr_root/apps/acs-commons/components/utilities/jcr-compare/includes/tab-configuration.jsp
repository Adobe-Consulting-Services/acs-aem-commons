<%-- Hosts --%>
<section class="coral-Form-fieldset acsCommons-Form-multifieldset">
    <h3 class="coral-Form-fieldset-legend">Hosts</h3>


    <p>Set a Host to "localhost" with blank user name and password to access it the accessed host w/ the executor's
        credentials. In a clustered or load balanced environments use host names/IPs explicitly tied to an AEM
        instance.</p>

    <div  ng-repeat="item in hosts track by $index">

        <div class="acsCommons-Panel">
            <div class="acsCommons-Panel-wrapper">
                <button class="coral-Button coral-Button--square coral-Button--quiet acsCommons-Form-multifieldset-remove"
                        ng-show="hosts.length > 1"
                        ng-click="hosts.splice($index, 1)">
                    <i class="coral-Icon coral-Icon--delete"></i>
                </button>

                <h3>Host &#35;{{ $index + 1 }}</h3>

                <label class="coral-Form-fieldlabel">Host</label>
                <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
                       ng-model="item.name"
                       type="text"
                       ng-change="item.uri=item.name"
                       placeholder="Ex. http://localhost:4503"/>

                <label class="coral-Form-fieldlabel">User name</label>
                <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
                       ng-model="item.user"
                       type="text"/>

                <label class="coral-Form-fieldlabel">Password</label>
                <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
                       ng-model="item.password"
                       type="password"/>
            </div>
        </div>
    </div>

    <div style="clear:left;"></div>

    <a  class="acsCommons-Form-multifieldset-add coral-Icon coral-Icon--add"
        ng-click="hosts.push({ name: '' });">Add Host</a>
</section>

<%-- Paths --%>
<section class="coral-Form-fieldset acsCommons-Form-multifieldset">
    <h3 class="coral-Form-fieldset-legend">Paths</h3>

    <div ng-repeat="item in config.paths track by $index">
        <button class="coral-Button coral-Button--square coral-Button--quiet acsCommons-Form-multifieldset-remove"
                ng-click="config.paths.splice($index, 1)">
            <i class="coral-Icon coral-Icon--delete"></i>
        </button>

        <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
               ng-model="item.value"
               type="text"
               placeholder="Absolute path to compare">
    </div>

    <a  class="acsCommons-Form-multifieldset-add coral-Icon coral-Icon--add"
        ng-click="config.paths.push({ value: '' });">Add Path</a>
</section>

<%-- Query --%>
<section class="coral-Form-fieldset">
    <h3 class="coral-Form-fieldset-legend">Query</h3>

    <select ng-options="language for language in ['None', 'xpath', 'JCR-SQL', 'JCR-SQL2']"
            ng-model="config.queryType"></select>

    <p>
                <textarea ng-hide="config.queryType === 'None'"
                          class="coral-Textfield coral-Textfield--multiline"
                          rows="4"
                          style="width: 100%"
                          ng-model="config.query"
                          type="text"></textarea>
    </p>
</section>


<%-- Exclude Properties --%>
<section class="coral-Form-fieldset acsCommons-Form-multifieldset">
    <h3 class="coral-Form-fieldset-legend">Node types</h3>

    <div ng-repeat="item in config.nodeTypes track by $index">
        <button class="coral-Button coral-Button--square coral-Button--quiet acsCommons-Form-multifieldset-remove"
                ng-click="config.nodeTypes.splice($index, 1)">
            <i class="coral-Icon coral-Icon--delete"></i>
        </button>

        <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
               ng-model="item.value"
               type="text"
               placeholder="Node type to calculate checksum">
    </div>

    <a      class="acsCommons-Form-multifieldset-add coral-Icon coral-Icon--add"
            ng-click="config.nodeTypes.push({ value: '' });">Add Node Type</a>
</section>


<%-- Exclude Node Types --%>
<section class="coral-Form-fieldset acsCommons-Form-multifieldset">
    <h3 class="coral-Form-fieldset-legend">Node types to exclude</h3>

    <div ng-repeat="item in config.excludeNodeTypes track by $index">
        <button class="coral-Button coral-Button--square coral-Button--quiet acsCommons-Form-multifieldset-remove"
                ng-click="config.excludeNodeTypes.splice($index, 1)">
            <i class="coral-Icon coral-Icon--delete"></i>
        </button>

        <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
               ng-model="item.value"
               type="text"
               placeholder="Node type to exclude"/>
    </div>

    <a  class="acsCommons-Form-multifieldset-add coral-Icon coral-Icon--add"
        ng-click="config.excludeNodeTypes.push({ value: '' });">Add Node Type to Exclude</a>

</section>


<%-- Exclude Properties --%>
<section class="coral-Form-fieldset acsCommons-Form-multifieldset">
    <h3 class="coral-Form-fieldset-legend">Properties to exclude</h3>

    <div ng-repeat="item in config.excludeProperties track by $index">
        <button class="coral-Button coral-Button--square coral-Button--quiet acsCommons-Form-multifieldset-remove"
                ng-click="config.excludeProperties.splice($index, 1)">
            <i class="coral-Icon coral-Icon--delete"></i>
        </button>


        <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
               ng-model="item.value"
               type="text"
               placeholder="Property to exclude"/>
    </div>

    <a  class="acsCommons-Form-multifieldset-add coral-Icon coral-Icon--add"
        ng-click="config.excludeProperties.push({ value: '' });">Add Property to Exclude</a>
</section>


<%-- Sorted Properties --%>
<section class="coral-Form-fieldset acsCommons-Form-multifieldset">
    <h3 class="coral-Form-fieldset-legend">Sorted multi-value properties</h3>

    <div ng-repeat="item in config.sortedProperties track by $index">
        <button class="coral-Button coral-Button--square coral-Button--quiet acsCommons-Form-multifieldset-remove"
                ng-click="config.sortedProperties.splice($index, 1)">
            <i class="coral-Icon coral-Icon--delete"></i>
        </button>


        <input class="coral-Form-field coral-Textfield acsCommons-Form-multifieldset-input"
               ng-model="item.value"
               type="text"
               placeholder="Property to respect sort order of values"/>
    </div>

    <a  class="acsCommons-Form-multifieldset-add coral-Icon coral-Icon--add"
        ng-click="config.sortedProperties.push({ value: '' });">Add Sorted Property</a>
</section>

<hr/>

<p>
    Use the
    <a href="#"
       onclick="$('#tabs').data('tabs')._setActive(1); return false;">Content Comparison</a>
            or
    <a href="#"
       onclick="$('#tabs').data('tabs')._setActive(2); return false;">Download JSON</a>
     tabs to execution the corresponding operations using this configuration set.
</p>