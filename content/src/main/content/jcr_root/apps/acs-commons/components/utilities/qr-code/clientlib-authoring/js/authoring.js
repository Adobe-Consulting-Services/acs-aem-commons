/*global JSON: false */

(function($, $document){
        var QR_CODE_TABLE_ID = "acs-commons--qr-code",
            QR_CODE_CONFIG_URL = '/etc/acs-commons/qr-code/_jcr_content/config.json',
            initialized = false;

        $document.on("foundation-contentloaded", function() {
            var $button;
            if (!initialized) {

                getConfig().then(function(config) {
                    config = JSON.parse(config);
                    if (config.enabled) {
                        $button = buildButton();
                        $qrCode = buildQrCode(config);
                        bindToButton($button, $qrCode);
                    }
                });

                initialized = true;
            }
        });


    /**
     * Get the Config from AEM...
     */
    function getConfig() {
        var config = {
            enabled: false
        };

        return $.ajax({
            url: QR_CODE_CONFIG_URL,
            dataType: 'json'
        }).error(function(response) {
            console.warn("Loaded the ACS Commons QR Code JavaScript but could not locate an configuration resource at [ " +
                QR_CODE_CONFIG_URL +
                "]. " +
                "Verify the config resource exists and and this user has access to said resource.");
        });
    }


    /**
     * Builds and inject the QR Code button to the end of the Page Editor action bar.
     */
    function buildButton() {
            var $button = $('<a ' +
                'id="acs-commons--qr-code-button" ' +
                'class="editor-GlobalBar-item foundation-toggleable-control coral-Button coral-Button--minimal" ' +
                'data-foundation-toggleable-control-target="#' + QR_CODE_TABLE_ID + '" ' +
                'href="#qr-code" ' +
                'title="CQ Code" ' +
                'aria-label="QR Code" ' +
                'is="coral-anchorbutton" ' +
                'icon="properties" ' +
                'iconsize="S" ' +
                'variant="minimal" ' +
                'size="M" ' +
                'role="button" ' +
                'aria-disabled="false">' +
                '<coral-icon class="coral-Icon coral-Icon--sizeS coral-Icon--properties" icon="properties" size="S" role="img" aria-label="properties"></coral-icon>' +
                '<coral-anchorbutton-label></coral-anchorbutton-label>' +
                '</a>');

            $($button).appendTo($("coral-actionbar-primary"));

            return $button;
        }


        function bindToButton($button, $qrCode) {
            $button.on("click", function () {
                if (publishHost && $qrCode.length == 1) {

                    url = window.location.pathname;
                    if (url.indexOf('/editor.html') > -1) {
                        // Remove <servletContext>/editor.html from URL
                        url = url.substring(url.indexOf('/editor.html') + '/editor.html'.length, url.length);
                    }
                    url = publishHost + url;

                    $qrCodeTable.empty().qrcode(url);
                } else {
                    // Configs are present but none of them matches with current host
                    $qrCode
                        .css('color', 'black')
                        .html('No Configurations are available for this Host; <a href="/etc/acs-commons/qr-code-config.html" target="_blank"><i>Configure here</i></a');
                }
                $qrCode.toggle();
            });
        }


        function buildQrCode() {
            var $qrCode = $('<div>').attr('id', QR_CODE_TABLE_ID);

            $('body').append($qrCode);

            return $qrCode;
        }

        function getMappedHost(mappingConfig) {
            var host;

            for (host in mappingConfig) {
                if (mappingConfig[host].name.indexOf(window.location.host) !== -1) {
                    if (mappingConfig[host].value) {
                       return mappingConfig[host].value;
                    }
                }
            }

            return null;
        }

}($, $(document)));