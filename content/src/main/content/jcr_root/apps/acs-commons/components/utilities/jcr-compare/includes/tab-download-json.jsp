<section class="coral-Form-fieldset">
    <p>Download the JSON dump from the selected server using the configuration defined on that tab.</p>

    <ul class="coral-List">
        <li     ng-repeat="host in hosts track by $index"
                ng-if="validHost(host)"
                class="coral-List-item">
            <a download
               target="_blank"
               class="coral-Link"
               href="{{ host.uri }}/bin/acs-commons/jcr-compare.dump.json?{{ configAsParams(config) }}"
               x-cq-linkchecker="skip">{{ host.name }}</a>
        </li>
    </ul>

</section>