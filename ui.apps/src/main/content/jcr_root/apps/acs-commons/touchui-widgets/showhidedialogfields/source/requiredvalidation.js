/**
 * Register required field validation.
 * 
 * The validator keys off of the ".acs-commons-field-required-allow-hidden" selector. To use in Touch UI
 * dialogs, add the class "field-required" to a textfield. It is implemented to
 * get around the problem that a hidden required field cannot be submitted. The
 * default AEM validator validates the field even if it is hidden.
 * 
 */
(function($, Granite) {
  "use strict";
  
  $(window)
      .adaptTo("foundation-registry")
      .register(
          "foundation.validation.validator",
          {
            selector : ".acs-commons-field-required-allow-hidden",
            validate : function(el) {
              var field, value;

              field = $(el).closest(".coral-Form-field");
              value = $(el).val();

              // check if the field or or its parent is hidden
              var hidden = $(field).hasClass('hide') || $(field).closest('.hide').length;

              // if field or its parent not hidden, validate
              if (!hidden && (value === null || value === '')) {
                return Granite.I18n.get('The field is required');
              }
              return null;
            },
            show : function(el, message, ctx) {
              var fieldErrorEl, field, error;

              fieldErrorEl = $("<span class='coral-Form-fielderror coral-Icon coral-Icon--alert coral-Icon--sizeS' data-init='quicktip' data-quicktip-type='error' />");
              field = $(el).closest(".coral-Form-field");

              field.attr("aria-invalid", "true").toggleClass("is-invalid", true);

              field.nextAll(".coral-Form-fieldinfo").addClass("u-coral-screenReaderOnly");

              error = field.nextAll(".coral-Form-fielderror");

              if (error.length === 0) {
                var arrow = field.closest("form").hasClass("coral-Form--vertical") ? "right" : "top";

                fieldErrorEl.attr("data-quicktip-arrow", arrow).attr("data-quicktip-content", message).insertAfter(field);
              } else {
                error.data("quicktipContent", message);
              }
            },
            clear : function(el, ctx) {
              var field = $(el).closest(".coral-Form-field");

              field.removeAttr("aria-invalid").removeClass("is-invalid");

              field.nextAll(".coral-Form-fielderror").tooltip("hide").remove();
              field.nextAll(".coral-Form-fieldinfo").removeClass("u-coral-screenReaderOnly");
            }
          });
})(Granite.$, Granite);