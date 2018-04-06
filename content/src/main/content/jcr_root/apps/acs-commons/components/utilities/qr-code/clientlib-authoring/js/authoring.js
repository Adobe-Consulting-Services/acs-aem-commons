/*global JSON: false */
(function($, $document, Granite){
        var QR_CODE_ID = "acs-commons__qr-code",
            QR_CODE_NO_CONFIG_CSS_CLASS = "acs-commons__qr-code--no-config",
            QR_CODE_CONFIG_URL = '/etc/acs-commons/qr-code/_jcr_content/config.json',
            $button,
            $qrCode,
            isAEM62 = false;

    /**
     * Bind to the Page Editor load event; This is the main hook.
     */
    $document.on("cq-editor-loaded", function() {
        if (Granite.author.ContentFrame.currentLocation().indexOf("/content/") === 0) {
            $.when(getConfig(Granite.author.ContentFrame.currentLocation())).then(function (config) {
                if ($button) {
                    // Remove QR from previous editor loads
                    $button.remove();
                    $qrCode.remove();
                }

                if (config.enabled) {
                    $button = buildButton(config.publishURL);
                    $qrCode = buildQrCode($button, config);
                    bindToButton($button, $qrCode);
                } else {
                    $button.remove();
                    $qrCode.remove();
                }
            });
        }

    });

    /**
     * Get the Config from AEM.
     */
    function getConfig(contentPath) {
        var config = {
            enabled: false
        };

        return $.ajax({
            url: Granite.HTTP.externalize(QR_CODE_CONFIG_URL + contentPath),
            dataType: 'json'
        }).error(function(response) {
            console.warn("Loaded the ACS Commons QR Code JavaScript but could not locate an configuration resource at [ " +
                Granite.HTTP.externalize(QR_CODE_CONFIG_URL + contentPath) +
                "]. " +
                "Verify the config resource exists and and this user has access to said resource.");
        });
    }

    /**
     * Builds and inject the QR Code button to the end of the Page Editor action bar.
     */
    function buildButton(publishURL) {
        // Initial css class of 'coral-Icon--viewGrid' required to support AEM 6.2
        var qrCodeButton = Granite.author.ui.globalBar.addButton('', 'viewGrid',
            Granite.I18n.get("QR Code for") + " " + publishURL);
        qrCodeButton.attr('data-foundation-toggleable-control-target', "#" + QR_CODE_ID);

        // Hack required for AEM 6.2 backwards compatibility
        if (Granite.author.ui.globalBar.element.find('.editor-GlobalBar-leftContainer').length > 0) {
            qrCodeButton.find('i').addClass('coral-Icon--viewGrid');
        }

        return qrCodeButton;
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
        var $qrCode = $('<div>').attr('id', QR_CODE_ID);

        if (config.publishURL) {
            $qrCode.qrcode(config.publishURL);
        } else {
            // Configs are present but none of them matches with current host
            $qrCode
                .addClass(QR_CODE_NO_CONFIG_CSS_CLASS)
                .html('<p>The QR Code configurations is not available.</p>' +
                      '<p><a href="' + Granite.HTTP.externalize('/etc/acs-commons/qr-code.html') + '" target="_blank"><i>Configure QR Code generation</i></a></p>');
        }

        $button.parent().append($qrCode);

        return $qrCode;
    }

}(Granite.$, $(document), Granite));