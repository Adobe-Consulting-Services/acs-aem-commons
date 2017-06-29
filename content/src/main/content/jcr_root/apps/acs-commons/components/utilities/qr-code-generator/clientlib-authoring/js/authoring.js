/*global JSON: false */

console.log("QR");

$(function() {
    var qrCode = {
            pageURL: "/etc/acs-commons/qr-code-config/jcr:content/config.json",
            qrElement: $(".qr-code-url")[0]
        },
        publishHost, urlElement, isEnabled, url, mappingConfig, parsedResponse, host;

    // Get all the configurations
    $.ajax({
        url: qrCode.pageURL,
        dataType: "json"
    }).done(function (response) {
        parsedResponse = JSON.parse(response.config);
        isEnabled = parsedResponse.enable;

        if (isEnabled) {
            $('.qr-code-url').removeAttr('disabled');
            mappingConfig = parsedResponse.properties;
            var host;
            for (host in mappingConfig) {
                if (mappingConfig[host].name.indexOf(window.location.host) !== -1) {
                    publishHost = mappingConfig[host].value;
                    break;
                }
            }
        }
    });





    // Create QR code element on page
    urlElement = document.createElement('div');
    urlElement.id = "qrcodeTable";
    $(".qr-code-url").append(urlElement);

    $(qrCode.qrElement).on("click", function () {
        if (publishHost) {
            url = publishHost + window.location.pathname;

            // Remove editor.html from URL
            url = url.replace("/editor.html", "");
            jQuery('#qrcodeTable').empty();
            jQuery('#qrcodeTable').qrcode(url);

        } else {
            // Configs are present but none of them matches with current host
            $("#qrcodeTable").css('color', 'black');
            jQuery('#qrcodeTable').html("No Configurations are available for this Host, Add from <a href='/etc/acs-commons/qr-code-config.html' target='_blank' ><i>here</i></a");

        }
        $("#qrcodeTable").toggle();

    });
});


(function($, $document){
    var FOUNDATION_CONTENT_LOADED = "foundation-contentloaded",
        initialized = false;

        $document.on(FOUNDATION_CONTENT_LOADED, function() {
            if (!initialized) {

                initialized = true;
                var html = '<a id="qr-code-trigger"' +
                    'class="editor-GlobalBar-item foundation-toggleable-control coral-Button coral-Button--minimal" ' +
                    'title="CQ Code"' +
                    'data-foundation-toggleable-control-target="#cq-code"' +
                    'href="#qr-code" ' +
                    'aria-label="QR Code" ' +
                    'is="coral-anchorbutton" ' +
                    'icon="properties" iconsize="S" ' +
                    'variant="minimal" ' +
                    'size="M" ' +
                    'role="button" ' +
                    'tabindex="0" ' +
                    'aria-disabled="false">' +
                    '<coral-icon class="coral-Icon coral-Icon--sizeS coral-Icon--properties" icon="properties" size="S" role="img" aria-label="properties"></coral-icon>' +
                    '<coral-anchorbutton-label></coral-anchorbutton-label>' +
                    '</a>';
                $(html).prependTo($("coral-actionbar-primary"));
                console.log("added icon");
            }
        })

}($, $(document)));