<section class="coral-Form-fieldset">
    <p>Click the link to download the JSON dump for the path defined above on the selected server.</p>

    <ul class="coral-List">
        <li     ng-repeat="host in hosts track by $index"
                class="coral-List-item">
            <a download
               class="coral-Link"
               href="{{ host.uri }}/bin/acs-commons/jcr-compare.dump.json?{{ configAsParams() }}"
               x-cq-linkchecker="skip">{{ host.name }}</a>
        </li>
    </ul>
</section>