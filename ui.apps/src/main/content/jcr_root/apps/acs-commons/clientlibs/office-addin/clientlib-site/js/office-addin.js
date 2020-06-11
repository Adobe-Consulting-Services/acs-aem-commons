window.addEventListener("message", captureEvent, false);

function captureEvent(event){
    var imgPath=JSON.parse(event.data).data[0].url;
    getBase64FromImage(imgPath, function(base64Img){
        Office.context.document.setSelectedDataAsync(
            base64Img,
            {
                coercionType: Office.CoercionType.Image,
                imageWidth: 400
            },
            function(asyncResult) {

                Word.run(function (context) {
                    var range = context.document.getSelection();
                    range.select('end');
                    return context.sync();
                });
                if (asyncResult.status === Office.AsyncResultStatus.Failed) {
                    console.log(asyncResult.error.message);
                }
            }
        );

    });
}

function getBase64FromImage(url, onSuccess) {
    var xhr = new XMLHttpRequest();

    xhr.responseType = "arraybuffer";
    xhr.open("GET", url);

    xhr.onload = function () {
        var binary, bytes;

        bytes = new Uint8Array(xhr.response);

        binary = [].map.call(bytes, function (byte) {
            return String.fromCharCode(byte);//may cause "Maximum call stack size exceeded"
        }).join('');

        onSuccess(btoa(binary));
    };
    xhr.send();
}

$('.resp-iframe').load( function() {
    $('.resp-iframe').contents().find("button.asset-picker-clear.coral3-Button.coral3-Button--quiet")
        .css({
            'display': 'none'
        });
    $('.resp-iframe').contents().find("foundation-autocomplete.granite-pickerdialog-searchfield")
        .css({
            'flex-wrap': 'nowrap',
        });
});