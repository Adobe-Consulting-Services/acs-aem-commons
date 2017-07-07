/*global JSON: false */

(function($, $document){
        var QR_CODE_ID = "acs-commons__qr-code",
            QR_CODE_NO_CONFIG_CSS_CLASS = "acs-commons__qr-code--no-config",
            QR_CODE_CONFIG_URL = '/etc/acs-commons/qr-code/_jcr_content/config.json',
            EDITOR_PATH_PREFIX = '/editor.html',
            initialized = false;

    /**
     * Bind to the Page Editor load event; This is the main hook.
     */
    $document.on("foundation-contentloaded", function() {
        var $button;

        if (!initialized) {
            $.when(getConfig()).then(function(data) {
                var config = JSON.parse(data.config),
                    $qrCode;

                if (config.enable) {
                    $button = buildButton();
                    $qrCode = buildQrCode($button, config);
                    bindToButton($button, $qrCode);
                }
            });

            initialized = true;
        }
    });

    /**
     * Get the Config from AEM.
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
        var $button = $(new Coral.Button().set({
                variant: "minimal",
                icon: "viewGrid",
                iconSize: "S",
                title: "QR Code",
                'data-foundation-toggleable-control-target': "#" + QR_CODE_ID
        }));

        // Hack to fit the aesthetic of Page Editor
        $button.addClass("editor-GlobalBar-item");

        $button.appendTo($("coral-actionbar-primary"));

        return $button;
     }

    /**
     * Bind click behavior to button.
     *
     * @param $button
     * @param $qrCode
     */
    function bindToButton($button, $qrCode) {
        $button.on("click", function () {
           if ($qrCode.length == 1) {
               $qrCode.toggle();
           }
        });
    }

    /**
     * Builds the QR Code DOM element and injects into the DOM.
     *
     * @param config
     * @returns {*|jQuery}
     */
    function buildQrCode($button, config) {
        var $qrCode = $('<div>').attr('id', QR_CODE_ID),
            url = window.location.pathname,
            publishHost = getMappedHost(config.properties);

        if (publishHost) {
            if (url.indexOf(EDITOR_PATH_PREFIX) > -1) {
                // Remove <servletContext>/editor.html from URL
                url = url.substring(url.indexOf(EDITOR_PATH_PREFIX) + EDITOR_PATH_PREFIX.length, url.length);
            }
            url = publishHost + url;

            $qrCode.empty().qrcode(url);
        } else {
            // Configs are present but none of them matches with current host
            $qrCode.empty();

            $qrCode
                .addClass(QR_CODE_NO_CONFIG_CSS_CLASS)
                .html('<p>No QR configurations are available for ' + window.location.host + '.</p>' +
                        '<p><a href="/etc/acs-commons/qr-code.html" target="_blank"><i>Configure QR code generation</i></a></p>');
        }


        $button.parent().append($qrCode);

        return $qrCode;
    }

    /**
     * Determines what the mapped hostname is based on the current windows address.
     *
     * @param mappingConfig
     * @returns {null}
     */
    function getMappedHost(mappingConfig) {
        var host;

        if (mappingConfig) {
            for (host in mappingConfig) {
                if (mappingConfig[host].name.indexOf(window.location.host) !== -1) {
                    if (mappingConfig[host].value) {
                       return mappingConfig[host].value;
                    }
                }
            }
        }

        return null;
    }

}($, $(document)));