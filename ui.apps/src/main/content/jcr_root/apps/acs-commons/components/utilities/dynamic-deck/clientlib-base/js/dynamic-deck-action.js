var Coral = window.Coral || {},
    Granite = window.Granite || {};

(function (window, document, $, Coral) {
    "use strict";
    var operationMode = "COLLECTION";
    $(document).on("foundation-contentloaded", function (e) {

        var SITE_PATH = "acs-commons/content/dynamic-deck-initiator.html",
            ui = $(window).adaptTo("foundation-ui");

        if (window.location.href.indexOf(SITE_PATH) < 0) {
            return;
        }

        /* Disabled query selection on page load */
        if (operationMode === "COLLECTION") {
            $("coral-select[name='./assetQuery']").attr("disabled", true);
            $("foundation-autocomplete[name='./assetTag']").attr("disabled", true);
        }

        $(document).off("change", ".coral-RadioGroup").on("change", ".coral-RadioGroup", function (event) {
            operationMode = event.target.value;
            if (operationMode === 'COLLECTION') {
                $("coral-select[name='./assetQuery']").attr("disabled", true);
                $("coral-select[name='./collectionPath']").attr("disabled", false);
                $("foundation-autocomplete[name='./assetTag']").attr("disabled", true);
            } else if (operationMode === 'QUERY') {
                $("coral-select[name='./assetQuery']").attr("disabled", false);
                $("coral-select[name='./collectionPath']").attr("disabled", true);
                $("foundation-autocomplete[name='./assetTag']").attr("disabled", true);
            } else if (operationMode === 'TAGS') {
                $("coral-select[name='./assetQuery']").attr("disabled", true);
                $("coral-select[name='./collectionPath']").attr("disabled", true);
                $("foundation-autocomplete[name='./assetTag']").attr("disabled", false);
            }
        });


        function getValueByName(fieldName, isMandatory) {
            var fieldValue = ($("input[name='" + fieldName + "']").val()).trim();
            if (!isMandatory) {
                return fieldValue;
            }
            if (!fieldValue || fieldValue.length === 0) {
                //for input fields
                $("input[name='" + fieldName + "']").attr('aria-invalid', 'true');
                $("input[name='" + fieldName + "']").attr('invalid', 'invalid');

                //for select fields
                $("coral-select[name='" + fieldName + "']").attr('aria-invalid', 'true');
                $("coral-select[name='" + fieldName + "']").attr('invalid', 'invalid');

                return;
            } else {
                return fieldValue;
            }
        }

        $(document).off("click", ".create-deck-initiator").on("click", ".create-deck-initiator", function (event) {
            event.preventDefault();

            var tagValues = $("coral-taglist[name='./assetTag'] > coral-tag").map(function () {
                return $(this).val();
            }).get().join(',');

            var deckTitle = getValueByName('./deckTitle', true),
                templatePath = getValueByName('./templatePath', true),
                collectionPath, queryString,
                masterAssetPath = getValueByName('./masterAssetPath', false),
                destinationPath = getValueByName('./destinationPath', true);

            if (operationMode === 'COLLECTION') {
                collectionPath = getValueByName('./collectionPath', true);
                if (!collectionPath) return;
            } else if (operationMode === 'QUERY') {
                queryString = getValueByName('./assetQuery', true);
                if (!queryString) return;
            } else if (operationMode === 'TAGS') {
                tagValues = $("coral-taglist[name='./assetTag'] > coral-tag").map(function () {
                    return $(this).val();
                }).get().join(',');
                if (!tagValues) return;
            }

            if (!deckTitle || !templatePath || !destinationPath) {
                return;
            }


            $.ajax({
                url: "acs-commons/content/dynamic-deck-initiator.triggerDeckDynamo.json",
                method: "POST",
                cache: false,
                data: {
                    deckTitle: deckTitle,
                    templatePath: templatePath,
                    masterAssetPath: masterAssetPath,
                    destinationPath: destinationPath,
                    collectionPath: collectionPath,
                    operationMode: operationMode,
                    queryString: queryString,
                    tagValues: tagValues
                }
            }).done(function (data) {
                if (data && data.message){
                    ui.notify("Success", data.message, "success");
                }else{
                    ui.notify("Error", "Unable to process deck generation", "error");
                }
            }).fail(function (data) {
                console.log(data.responseJSON.message);
                if (data && data.responseJSON && data.responseJSON.message){
                    ui.notify("Error", data.responseJSON.message, "error");
                }else{
                    ui.notify("Error", "Unable to process deck generation", "error");
                }
            });
        });
    });
})(window, document, $, Coral);
