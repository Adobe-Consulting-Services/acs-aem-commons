<section class="coral-Form-fieldset">

    <h3 class="coral-Form-fieldset-legend">Path</h3>

    <input class="coral-Form-field coral-Textfield"
           ng-model="form.json.path"
           type="text"
           placeholder="Absolute path to dump to JSON">

    <div ng-show="form.json.path">
        <p>Click the link to download the JSON dump for the path defined above on the selected server.</p>
        <ul class="coral-List">
            <li     ng-repeat="host in hosts track by $index"
                    class="coral-List-item">
                <a download
                   class="coral-Link"
                   href="{{ host.uri }}/bin/acs-commons/jcr-compare.dump.json?path={{form.json.path}}"
                   x-cq-linkchecker="skip">{{ host.name }}</a>
            </li>
        </ul>
    </div>
</section>