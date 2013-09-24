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
    "use strict";

    function createDefault(provider) {
        return function(el, key, params) {
            if (provider.message.hasOwnProperty(key)) {
                var string = provider.message[key];
                (params || []).forEach(function(p, i) {
                    string = string.replace(new RegExp("\\{" + i + "\\}", "g"), p);
                });
                return string;
            }
            return undefined;
        };
    }

    var registry = (function() {
        var providers = [];

        return {
            register: function(provider) {
                providers.push(provider);
            },

            message: function(el, key, params) {
                for (var i = providers.length - 1; i >= 0; i--) {
                    var provider = providers[i];

                    if (!el.is(provider.selector)) {
                        continue;
                    }

                    var message = ($.isFunction(provider.message) ? provider.message : createDefault(provider)).call(el, el, key, params);

                    if (message) {
                        return message;
                    }
                }

                if (console) console.warn("Message not found:", key, el[0]);

                return undefined;
            }
        };
    })();


    /**
     * @namespace jQuery
     */
    /**
     * @namespace jQuery.fn
     */

    /**
     * Returns the message for the given key.
     * 
     *
     * @memberof jQuery.fn
     *
     * @param {String} key The key used to look up.
     * @param {Array} params The params to replace the placeholders
     *
     * @example
$("#myElement").message("range-min", [1]);

If "range-min" is resolved to "Please fill out with minimum value of {0}", the result would be "Please fill out with minimum value of 1",
     */
    $.fn.message = function(key, params) {
        return registry.message(this.first(), key, params);
    };


    /**
     * Provides a hook for customization of message plugin.
     *
     * @namespace jQuery.message
     */
    $.message = (function() {
        return {
            /**
             * Registers the given message provider.
             *
             * @memberof jQuery.message
             *
             * @param {Object} provider
             * @param {String|Function} provider.selector The provider will be used only when the element is satisfying the selector. It will be passed to <code>jQuery.fn.is</code>.
             * @param {Object|Function} provider.message The object acting as a lookup table or the function returning the resolved string.
             *
             * @example
jQuery.message.register({
    selector: ":lang(en)",
    message: {
        required: "Please fill out this field.",
        range-min: "Please fill out with minimum value of {0}",
        range-between: "Please fill out with value between {0} and {1}"
    }
});

jQuery.message.register({
    selector: ":lang(en)",
    message: function(el, key, params) {
        if (key === "required") return "Please fill out this field.";
        else return undefined;
    }
});
             */
            register: function(provider) {
                $.each(arguments, function() {
                    registry.register(this);
                });
            }
        };
    })();
})(jQuery, window.console);
