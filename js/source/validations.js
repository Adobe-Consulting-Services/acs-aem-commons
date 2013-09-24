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
(function($, console) {
    // This is the place where the validation rules and messages are defined.
    // But only for validation that is generic or make sense at Coral level.
    // Otherwise please put your validation in your own file at your level.

    "use strict";

    if (!$.message || !$.validator) {
        if (console) console.warn("$.message and/or $.validator are not available, thus nothing is registered.");
        return;
    }


    // Register all the messages for validation
    // IMPORTANT: the order is important, where the last one will be checked first; when in doubt check the source of jquery-message

    $.message.register({
        selector: "*",
        message: {
            "validation.required": "Please fill out this field."
        }
    });

    $.message.register({
        selector: ":lang(de)",
        message: {
        }
    });


    // Register all the validation rules
    // IMPORTANT: the order is important, where the last one will be checked first; when in doubt check the source of jquery-validator

    // TODO Currently we will only use the standard markup for validation rules (e.g. @required, @pattern) due to time constraints and no clear demand yet.
    // But we can certainly define more advance validations. For example the following markup:
    //
    // <input type="text" data-validation="cui.url" />
    // <input type="text" data-validation="granite.path" />
    // <input type="text" data-validation="granite.relativepath" />
    // <input type="text" data-validation="myrule1" />
    //
    // where @data-validation's value is a name (namespaced). The validator can be registered as usual:
    //
    // $.validator.register({
    //     selector: "form input[data-validation='cui.url']",
    //     validate: function(el) {}
    // });
    //
    // We can do all this without modifying jquery-validator and jquery-message at all.
    // So later when you need a fancy validation, please bring this issue to the mailing list first.


    // Cancel the native invalid event (which is triggered by the browser supporting native validation)
    // to show our own UI instead
    $(document).on("cui-contentloaded", function(e) {
        $(":submittable", e.target).on("invalid", function(e) {
            if (e.isJqueryValidator) return;

            e.preventDefault();

            var el = $(this);
            el.checkValidity();
            el.updateErrorUI();
        });
    });


    function simpleShow(el, message) {
        var error = el.next(".form-error");

        el.attr("aria-invalid", "true").toggleClass("error", true);

        if (error.length === 0) {
            el.after($("<span class='form-error' data-init='quicktip' data-quicktip-arrow='top' data-quicktip-type='error' />").html(message));
        } else {
            error.html(message);
        }
    }

    function simpleClear(el) {
        el.removeAttr("aria-invalid").removeClass("error");
        el.next(".form-error").remove();
    }


    // TODO TBD if we want to do validation only when then form is having a certain class
    // e.g. using selector "form.validate input" instead of just "form input"

    $.validator.register({
        selector: "form *",
        show: simpleShow,
        clear: simpleClear
    });

    // Check required & aria-required of input and textarea
    $.validator.register({
        selector: "form input, form textarea",
        validate: function(el) {
            var isRequired = el.prop("required") === true ||
                (el.prop("required") === undefined && el.attr("required") !== undefined) ||
                el.attr("aria-required") === "true";

            if (isRequired && el.val().length === 0) {
                return el.message("validation.required") || "required";
            }
        }
    });

    $(document).on("input", "form input, form textarea", function(e) {
        var el = $(this);
        el.checkValidity();
        el.updateErrorUI();
    });


    // Check role=listbox
    $.validator.register({
        selector: "[role=listbox]",
        validate: function(el) {
            if (el.attr("aria-required") !== "true") {
                return;
            }

            var selected = false;
            el.find("[role=option]").each(function() {
                if ($(this).attr("aria-selected") === "true") {
                    selected = true;
                    return false;
                }
            });

            if (!selected) {
                return el.message("validation.required") || "required";
            }
        }
    });
})(jQuery, window.console);
