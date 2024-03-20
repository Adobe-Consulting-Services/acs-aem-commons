(function ($, document) {

    // we need a small delay to wait for the iFrame to load
    $(document).ready(window.setTimeout(addStylesToIframe, 500));

    function addStylesToIframe() {
        var iFrame = document.getElementById("acs-commons__asset-selectors__ms-office--iframe");
        if (iFrame) {
            var iFrameHead = iFrame.contentWindow.document.head;
            // hide 'More Details' button. This would allow the author to break out of the asset picker.
            addCssToElement(iFrameHead, "#asset-details-link-wrapper {display: none;}");
        }
    }

    function addCssToElement(element, css) {
        var style = document.createElement('style');
        style.innerText = css;
        if (element) {
            element.appendChild(style);
        }
    }

    // Handle AEM Asset to PowerPoint
    Office.onReady(function (info) {
        function captureEvent(event) {
            // don't add the image twice
            event.stopImmediatePropagation();
            var eventJson, url;

            try {
                eventJson = JSON.parse(event.data);
            } catch (e) {
                console.log("Unable to parse JSON from event data: " + event.data);
            }

            if (!(eventJson && eventJson.config && eventJson.config.action && eventJson.config.action === 'done')) {
                // This is not a selection event so discard
                return;
            }

            if (eventJson.data && Array.isArray(eventJson.data) && eventJson.data.length === 1 && eventJson.data[0].url) {
                url = eventJson.data[0].url;
            } else {
                console.error("An error occurred. The Asset URL could not be collected.");
                return;
            }

            console.debug("Syncing asset [ " + url + " ] from AEM to MS Office");

            if (Office.context.document) {
                getBase64FromAsset(url, function (base64Img) {
                    Office.context.document.setSelectedDataAsync(base64Img, {
                        coercionType: Office.CoercionType.Image, imageWidth: 400
                    }, function (asyncResult) {
                        Word.run(function (context) {
                            var range = context.document.getSelection();
                            return context.sync();
                        });

                        if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                            console.log(asyncResult.error.message);
                        }
                    });
                });
            } else {
                console.error("Cannot complete selection because the browser is not operating in the context of a MS Office application. Please load this page in an MS Office application as an add-in.");
            }
        }

        function getBase64FromAsset(url, onSuccess) {
            var xhr = new XMLHttpRequest();

            xhr.responseType = "arraybuffer";
            xhr.open("GET", url);

            xhr.onload = function () {
                var binary, bytes;

                bytes = new Uint8Array(xhr.response);

                binary = [].map.call(bytes, function (byte) {
                    return String.fromCharCode(byte); // May cause "Maximum call stack size exceeded"
                }).join('');

                onSuccess(btoa(binary));
            };
            xhr.send();
        }

        window.addEventListener("message", captureEvent, false);
    });
})(jQuery, document);
