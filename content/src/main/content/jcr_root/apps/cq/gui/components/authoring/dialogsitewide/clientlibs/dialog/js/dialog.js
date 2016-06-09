/*
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
*/
(function(window, document, $) {
    "use strict";
    
    var ui = $(window).adaptTo("foundation-ui"),
		$document = $(document);

    function gotoPageEditor(dialog) {
        if (dialog.data("cqDialogReturntoreferral")) {
            window.location = document.referrer;
        } else {
            if (dialog.closest(".cq-dialog-page").length) {
                window.location = dialog.data("cqDialogPageeditor");
            } else {
                dialog.remove();
            }
        }
        $document.trigger("dialog-closed");
    }

    $(document).on("click", ".cq-dialog-cancel", function(e) {
        e.preventDefault();
        
        var dialog = $(this).closest("form.cq-dialog");
        gotoPageEditor(dialog);
    });

    $(document).on("click", ".cq-dialog-layouttoggle", function(e) {
        e.preventDefault();
    });

    $(document).on("click", ".cq-dialog-help", function(e) {
        e.preventDefault();

        var el = $(this);
        window.open(el.data("href"), "_blank");
    });

    $(document).on("foundation-form-submitted", "form.foundation-form.cq-dialog", function(e, status, xhr) {
        if (status === true) {
            var dialog = $(this);
            
            $(document).trigger("dialog-success");
            gotoPageEditor(dialog);
        } else {
            $(document).trigger("dialog-fail", xhr);
        }
    });

    // Force blur on fields before submitting (CQ-55684)
    // Listen on body because form submit is already intercepted to handle tags (see properties.js)
    $("body").on("submit", ".cq-dialog", function (event) {
        var dialog = $(this);
        dialog.find("input, textarea").blur();
    });

})(window, document, Granite.$);
