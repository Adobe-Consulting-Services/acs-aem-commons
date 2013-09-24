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
(function(document, $) {
    /*jshint es5:true */
    "use strict";

    var registry = (function() {
        // Currently these selectors are designed to be fixed. i.e. in order to be validated, the field MUST either use standard element or leverage ARIA.

        // http://www.w3.org/TR/html5/forms.html#category-submit
        var submittableSelector = "input, textarea, select, button, keygen, object, [role=checkbox], [role=radio], [role=combobox], [role=listbox], [role=radiogroup], [role=tree], [role=slider], [role=spinbutton], [role=textbox]";

        // http://www.w3.org/TR/html5/forms.html#candidate-for-constraint-validation
        // It makes sense to create a pseudo selector ":validable" for this
        var candidateSelector = "input:not([readonly],[disabled],[type=hidden],[type=reset],[type=button]), select, textarea:not([readonly]), button[type=submit], [role=checkbox]:not([aria-disabled=true]), [role=radio]:not([aria-disabled=true]), [role=combobox]:not([aria-disabled=true]), [role=listbox]:not([aria-disabled=true]), [role=radiogroup]:not([aria-disabled=true]), [role=tree]:not([aria-disabled=true]), [role=slider]:not([aria-disabled=true]), [role=spinbutton]:not([aria-disabled=true]), [role=textbox]:not([aria-disabled=true],[aria-readonly=true])";

        var validators = [];

        return {
            get submittableSelector() {
                return submittableSelector;
            },

            isSummittable: function(el) {
                return el.is(this.submittableSelector);
            },

            submittables: function(form) {
                return form.find(this.submittableSelector);
            },

            isCandidate: function(el) {
                return el.is(candidateSelector);
            },

            api: function(el) {
                if (!this.isSummittable(el)) return;

                return el.data("validator") || (function(el, registry) {
                    var api = createValidationAPI(el, registry);
                    el.data("validator", api);
                    return api;
                })(el, this);
            },

            register: function(validator) {
                validators.push(validator);
            },

            validators: function(el) {
                return validators.filter(function(v) {
                    return el.is(v.selector);
                });
            }
        };
    })();

    function createValidationAPI(el, registry) {
        return new HTMLValidation(el, registry);
    }

    /**
     * @see {@link http://www.w3.org/TR/html5/forms.html#the-constraint-validation-api}
     */
    function HTMLValidation(el, registry) {
        this.el = el;
        this.registry = registry;
        this.message = null;
        this.customMessage = null;

        this.state = (function(outer) {
            return {
                get customError() {
                    return !!outer.customMessage;
                },
                get valid() {
                    return !outer.customMessage && !outer.message;
                }
            };
        })(this);
    }

    function everyReverse(array, callback, thisArg) {
        for (var i = array.length - 1; i >= 0; i--) {
            if (!callback.call(thisArg, array[i], i, array)) {
                return false;
            }
        }
        return true;
    }

    function createInvalidEvent() {
        return $.Event("invalid", {
            isJqueryValidator: true
        });
    }

    HTMLValidation.prototype = {
        get willValidate() {
            return this.registry.isCandidate(this.el);
        },

        setCustomValidity: function(message) {
            this.customMessage = message;
        },

        get validity() {
            return this.state;
        },

        checkValidity: function(options) {
            options = options || {};

            if (!this.willValidate) {
                return true;
            }

            if (this.customMessage) {
                if (!options.suppressEvent) this.el.trigger(createInvalidEvent());
                return false;
            }

            this.message = null;
            everyReverse(this.registry.validators(this.el), function(v) {
                if (!v.validate) return true;

                var m = v.validate(this.el);
                if (m) {
                    this.message = m;
                    return false;
                } else {
                    return true;
                }
            }, this);

            if (this.message) {
                if (!options.suppressEvent) this.el.trigger(createInvalidEvent());
                return false;
            }

            return true;
        },

        get validationMessage() {
            if (!this.willValidate) return "";

            return this.customMessage || this.message || "";
        },

        updateUI: function() {
            var f;
            if (this.validity.valid) {
                f = function(v) {
                    return !v.clear || v.clear(this.el) === $.validator.CONTINUE;
                };
            } else {
                f = function(v) {
                    return !v.show || v.show(this.el, this.validationMessage) === $.validator.CONTINUE;
                };
            }

            everyReverse(this.registry.validators(this.el), f, this);
        }
    };

    $.extend($.expr[":"], {
        /**
         * Selects elements that can be used during form submission.
         * Currently it is "input, textarea, select, button, keygen, object, [role=checkbox], [role=radio], [role=combobox], [role=listbox], [role=radiogroup], [role=tree], [role=slider], [role=spinbutton], [role=textbox]".
         *
         * @see {@link http://www.w3.org/TR/html5/forms.html#category-submit}
         * @ignore
         */
        submittable: function (element) {
            return registry.isSummittable($(element));
        }
    });

    /**
     * Returns <code>true</code> if the element will be validated when the form is submitted; <code>false</code> otherwise.
     *
     * @memberof jQuery.fn
     * @name willValidate
     * @return {Boolean}
     * @see {@link http://www.w3.org/TR/html5/forms.html#dom-cva-willvalidate}
     */
    $.fn.willValidate = function() {
        var api = registry.api(this.first());
        if (api) {
            return api.willValidate;
        } else {
            return false;
        }
    };

    /**
     * Returns the error message that would be shown to the user if the element was to be checked for validity.
     *
     * @memberof jQuery.fn
     * @name validationMessage
     * @return {String} The error message or an empty string
     * @see {@link http://www.w3.org/TR/html5/forms.html#dom-cva-validationmessage}
     */
    $.fn.validationMessage = function() {
        var api = registry.api(this.first());
        if (api) {
            return api.validationMessage;
        } else {
            return "";
        }
    };

    /**
     * Returns <code>true</code> if the element's value has no validity problems; <code>false</code> otherwise.
     * Fires an invalid event at the element in the latter case.
     *
     * @memberof jQuery.fn
     * @name checkValidity
     * @return {Boolean} The validity
     * @see {@link http://www.w3.org/TR/html5/forms.html#dom-cva-checkvalidity}
     */
    $.fn.checkValidity = function() {
        var api = registry.api(this.first());
        if (api) {
            return api.checkValidity();
        } else {
            return true;
        }
    };

    /**
     * Sets a custom error, so that the element would fail to validate.
     * The given message is the message to be shown to the user when reporting the problem to the user.
     * If the argument is the empty string, clears the custom error.
     *
     * @memberof jQuery.fn
     * @name setCustomValidity
     * @param {String} message The error message or empty string
     * @see {@link http://www.w3.org/TR/html5/forms.html#dom-cva-setcustomvalidity}
     */
    $.fn.setCustomValidity = function(message) {
        return this.each(function() {
            var api = registry.api($(this));
            if (api) {
                api.setCustomValidity(message);
            }
        });
    };

    /**
     * Shows error UI if the element is invalid; hide the UI otherwise.
     *
     * @memberof jQuery.fn
     * @name updateErrorUI
     */
    $.fn.updateErrorUI = function() {
        return this.each(function() {
            var api = registry.api($(this));
            if (api) {
                api.updateUI();
            }
        });
    };

    /**
     * @namespace jQuery
     */

    /**
     * <code>jQuery.validator</code> is a jQuery plugin that provides form validation, which is designed to replicate {@link http://www.w3.org/TR/html5/forms.html#constraints|HTML Forms Constraint Validation API}.
     * <p>It will capture the form submit event to cancel and stop propagating the event when the form is invalid. If there is <code>novalidate</code> attribute, the validation is skipped.
     * It also doesn't actually provide any validation rule, such as validating required field.
     * Rather the rule is separated and made pluggable, which can be registered using {@link jQuery.validator.register}.</p>
     *
     * @namespace jQuery.validator
     */
    $.validator = (function() {
        return {
            /**
             * A flag to indicate that the validation process should continue to the next validator
             */
            CONTINUE: 1,

            /**
             * A flag to indicate that the validation process should stop. This is default behavior.
             */
            STOP: 2,

            /**
             * <p>Registers the given validator(s).</p>
             * <p>Each validator will be iterated to check the validity of submittable elements, where the iteration stopped when the first matching validator says invalid.
             * The order of the registration is important, where the last one registered will be used first.</p>
             *
             * <h6>Design Suggestion</h6>
             * <p>It is recommended to use existing standard vocabulary (e.g. {@link http://www.w3.org/TR/html5/forms.html#attr-input-required|required}, {@link http://www.w3.org/TR/html5/forms.html#attr-input-pattern|pattern} attribute), to define the rule.
             * For advance rule, the following markup convention is recommended to be used:
             * 
             * <pre class="prettyprint"></code>&lt;input type="text" data-validation="cui.url" />
&lt;input type="text" data-validation="granite.path" />
&lt;input type="text" data-validation="granite.relativepath" />
&lt;input type="text" data-validation="myrule1" /></code></pre>
             *
             * where <code>data-validation</code> attribute value is a namespaced name. The validator can be then registered as usual:
             *
             * <pre class="prettyprint"></code>jQuery.validator.register({
    selector: "form input[data-validation='cui.url']",
    validate: function(el) {}
});</code></pre></p>
             *
             * @memberof jQuery.validator
             *
             * @param {...Object} validator One or more validator objects.
             * @param {String|Function} validator.selector Only the element satisfying the selector will be validated using this validator. It will be passed to <code>jQuery.fn.is</code>.
             * @param {Function} validator.validate The actual validation function. It must return a string of error message if the element fails.
             * @param {Function} validator.show The function to show the error. The function can return {@link jQuery.validator.CONTINUE} or {@link jQuery.validator.STOP}.
             * @param {Function} validator.clear The function to clear the error. The function can return {@link jQuery.validator.CONTINUE} or {@link jQuery.validator.STOP}.
             *
             * @example
jQuery.validator.register({
    selector: "form input",
    validate: function(el) {
        if (el.attr("aria-required") === "true" && el.val().length === 0) {
            return "This field is required";
        }
    },
    show: function(el, message) {
        // show the error UI
    },
    clear: function(el) {
        // clear the error UI
    }
});
             */
            register: function() {
                $.each(arguments, function() {
                    registry.register(this);
                });
            },

            /**
             * Returns <code>true</code> if all submittable elements under the given root element have no validity problems; <code>false</code> otherwise.
             * Fires an invalid event at the invalid submittable element.
             *
             * @memberof jQuery.validator
             * 
             * @param {jQuery} root The root element
             *
             * @return {Boolean} The validity
             */
            isValid: function(root) {
                var allValid = true;
                root.find(":submittable").each(function() {
                    var el = $(this);
                    if (el.willValidate && !el.checkValidity()) {
                        allValid = false;
                        return false;
                    }
                });

                return allValid;
            }
        };
    })();


    /**
     * Statically validate the constraints of form.
     * @see {@link http://www.w3.org/TR/html5/forms.html#statically-validate-the-constraints}
     */
    function staticallyValidate(form, registry) {
        return registry.submittables(form)
            .map(function() {
                var api = registry.api($(this));
                if (!api || !api.willValidate || api.checkValidity({
                    suppressEvent: true
                })) return;
                return this;
            }).map(function() {
                var e = createInvalidEvent();
                $(this).trigger(e);

                if (!e.isDefaultPrevented()) {
                    return this;
                }
            });
    }

    /**
     * Interactively validate the constraints of form.
     * @see {@link http://www.w3.org/TR/html5/forms.html#interactively-validate-the-constraints}
     */
    function interactivelyValidate(form, registry) {
        var unhandleds = staticallyValidate(form, registry);

        if (unhandleds.length > 0) {
            unhandleds.each(function() {
                var api = registry.api($(this));
                if (api) {
                    api.updateUI();
                }
            });
            return false;
        }

        return true;
    }

    // Use event capturing to cancel and stop propagating the event when form is invalid
    // This way no other event handlers are executed
    document.addEventListener("submit", function(e) {
        var form = $(e.target);

        // TODO TBD if we want to do validation only when there is a certain class or based on config of $.validator

        if (!form.is("form") ||
            form.prop("noValidate") === true ||
            (form.prop("noValidate") === undefined && form.attr("novalidate") !== undefined)) {
            return;
        }

        if (!interactivelyValidate(form, registry)) {
            e.stopPropagation();
            e.preventDefault();
        }
    }, true);
})(document, jQuery);
