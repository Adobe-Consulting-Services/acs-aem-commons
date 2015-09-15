<section class="coral-Form-fieldset">
    <ul class="coral-List">
        <li     ng-repeat="host in hosts track by $index"
                class="coral-List-item">
            <a download
               class="coral-Link"
               href="{{ host.uri }}/bin/acs-commons/jcr-compare.dump.json?path={{form.path}}"
               x-cq-linkchecker="skip">{{ host.name }}</a>
        </li>
    </ul>
</section>