/*global JSON: false, angular: false */

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
        jQuery('#qrcodeTable').html("No Configurations are available for this Host, Add it from <a href='/etc/acs-tools/qr-code-config.html' target='_blank' ><i>here</i></a");

    }
    $("#qrcodeTable").toggle();

});
