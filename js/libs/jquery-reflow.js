
(function($, window, undefined) {

    var defaults = {
        "threshold": 200, // How often the resize and reflow events should be considered
        "applyClassToElement": undefined
    };

    // Utility functions to help calculating sizes
    var size = {
        "rem": function () {
            // This caches the rem value to calculate it only once, but might lead to wrong results if the font size gets changed
            if (size._rem === undefined) {
                size._rem = parseInt($("body").css("font-size"));
            }
            return size._rem;
        },
        "em": function (elem) {
            return parseFloat(elem.css("font-size"));
        }
    };

    // Adds and removes classes to the given element depending on the result of the associated functions.
    // Can be called with or without parameters:
    // When a breakpoints object is provided, the reflow listener gets setup to the given element.
    // The options parameter is optional, it allows to change the default settings.
    // When no parameters are provided it triggers a reflow event on the provided object.
    $.fn.reflow = function reflow(breakpoints, options) {
        return this.each(function reflowEach() {
            var elem = $(this),
                didApplyClassNames = false,
                scheduledReflowCheck = false,
                settings;

            if (breakpoints) {
                settings = $.extend({}, defaults, options);
                settings.applyClassToElement = settings.applyClassToElement || elem;

                function reflowEventHandler() {
                    if (elem.is(":visible")) {
                        if (!scheduledReflowCheck) {
                            applyClassNames();
                            scheduledReflowCheck = setTimeout(function reflowCheck() {
                                scheduledReflowCheck = false;
                                if (!didApplyClassNames) {
                                    applyClassNames();
                                }
                            }, settings.threshold);
                        } else {
                            didApplyClassNames = false;
                        }
                    }
                }

                function applyClassNames() {
                    didApplyClassNames = true;
                    for (var className in breakpoints) {
                        settings.applyClassToElement.toggleClass(className, breakpoints[className](elem, size));
                    }
                }

                elem.on("reflow", reflowEventHandler);
                $(window).on("resize.reflow", reflowEventHandler);
            }

            elem.trigger("reflow");

        });
    }

}(jQuery, this));
