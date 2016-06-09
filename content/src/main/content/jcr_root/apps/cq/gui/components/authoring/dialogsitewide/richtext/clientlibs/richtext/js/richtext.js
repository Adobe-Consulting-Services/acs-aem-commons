(function(window, document, $) {
    "use strict";

    $(document).on("foundation-contentloaded", function() {

        var $container = $(".richtext-container");

        $container.each(function () {
            $(this).closest(".coral-FixedColumn-column").addClass("coral-RichText-FixedColumn-column");
        });

        // Copy hidden text field to RTE
        $container.each(function() {
            var html = $(this).find("input[type=hidden].coral-Textfield").val();
            $(this).find(".coral-RichText-editable").empty().append(html);
        });

        // Copy RTE text to hidden field
        $container.on("editing-finished", ".coral-RichText-editable", function() {
            var el = $(this).closest(".richtext-container");
            el.find("input[type=hidden].coral-Textfield").val(el.find(".coral-RichText-editable").html());
        });

        // Register ...
        CUI.util.plugClass(CUI.RichText, "richEdit", function(rte) {
            CUI.rte.Utils.setI18nProvider(new CUI.rte.GraniteI18nProvider());
            CUI.rte.ConfigUtils.loadConfigAndStartEditing(rte, $(this));
            $(this).data("rteinstance", rte);
        });

        var $richTextDiv = $(".richtext-container>.coral-RichText");
        if ($richTextDiv.data("useFixedInlineToolbar")) {
            $richTextDiv.richEdit();
        }
    });

    var canFinishEditing = function(e) {
        // trigger items present in the dialog, which when clicked would start exit process for RTE
        var $finishTriggers = $(e.currentTarget).find('.cq-dialog-cancel, .coral-Icon--close, .cq-dialog-submit, .coral-Icon--check');
        var isNextButton = $(e.currentTarget).hasClass('coral-Wizard-nextButton');
        var canFinishEditing = isNextButton; // RTE is inside a form/wizard whose next button is clicked
        if (!canFinishEditing) {
            var triggerCount = $finishTriggers.size();
            // return true if the click was inside cancel/submit button or
            for (var index = 0; index < triggerCount; index++) {
                if ($finishTriggers[index] == e.target) {
                    canFinishEditing = true;
                    break;
                }
            }
        }
        return canFinishEditing;
    };

    $(document).on("click", ".cq-dialog-actions, .coral-Wizard-nextButton", function(e) {
        if (canFinishEditing(e)) {
            $(this).closest(".foundation-form").find(".richtext-container>.coral-RichText").each(function(){
                var rteInstance = $(this).data("rteinstance");
                if (rteInstance) {
                    rteInstance.finish(false);
                }
            });
        }
    });

})(window, document, Granite.$);
