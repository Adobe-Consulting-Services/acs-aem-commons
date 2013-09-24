/*
 *
 * ADOBE CONFIDENTIAL
 * __________________
 *
 *  Copyright 2012 Adobe Systems Incorporated
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated and its
 * suppliers and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
(function (window, $, undefined) {

    // ========================= smartresize ===============================

    /*
     * smartresize: debounced resize event for jQuery
     *
     * latest version and complete README available on Github:
     * https://github.com/louisremi/jquery.smartresize.js
     *
     * Copyright 2011 @louis_remi
     * Licensed under the MIT license.
     */

    var $event = $.event,
            resizeTimeout;

    $event.special.smartresize = {
        setup:function () {
            $(this).bind("resize", $event.special.smartresize.handler);
        },
        teardown:function () {
            $(this).unbind("resize", $event.special.smartresize.handler);
        },
        handler:function (event, execAsap) {
            // Save the context
            var context = this,
                    args = arguments;

            // set correct event type
            event.type = "smartresize";

            if (resizeTimeout) {
                clearTimeout(resizeTimeout);
            }
            resizeTimeout = setTimeout(function () {
                jQuery.event.handle.apply(context, args);
            }, execAsap === "execAsap" ? 0 : 10);
        }
    };

    $.fn.smartresize = function (fn) {
        return fn ? this.bind("smartresize", fn) : this.trigger("smartresize", ["execAsap"]);
    };

    // constructor
    $.CUIGridLayout = function (options, element, callback) {
        this.element = $(element);
        this._create(options);
        this._init(callback);
    };

    var $window = $(window);

    $.CUIGridLayout.settings = {
        colWidth: 240,
        gutterX: 15,
        gutterY: 15,
        marginLeft:0,
        marginRight:0,
        selector:"article"
//        itemClass:"macboard-card"
    };

    // TODO layout only takes image sizes into account; may fail when cards have differing "legends"

    $.CUIGridLayout.prototype = {

        option:function(options) {
            this.options = $.extend({}, $.CUIGridLayout.settings, options);
        },

        // sets up widget
        _create:function (options) {
            
            this.options = $.extend({}, $.CUIGridLayout.settings, options);

            this.items = [];

            this.itemsByPath = {};

            this.numCols = -1;

            this.pendingImages = 0;

            this.update();

            
            // bind resize method
            var self = this;
            $window.bind('smartresize.cui.gridlayout', function () {
                self.updateDimensions();
                self.layout();
            });

        },

        _init:function (callback) {
            this.updateDimensions();
            this.layout(callback);

            // Give browser a chance to lay out elements and calculate layout a second time after
            // all CSS is applied correctly by the browser. Without this second, timed calculation is sometimes wrong due
            // to race conditions with the rendering engine of the browser.
            setTimeout(function() {
                this.numCols = -1;
                this.updateDimensions();
                this.layout(callback);

                this.element.trigger($.Event("cui-gridlayout-initialized", {
                    "widget": this
                }));
            }.bind(this), 1); 
        },

        update:function () {
            var self = this;
            this.items = [];
            this.itemsByPath = {};
            this.element.find(self.options.selector).each(function (i) {
                var $card = $(this);
                var $img = $("img", $card);
                if ($img.length == 0) {
                    $img = null;
                }
                var item = {
                    path: $card.data().path,
                    i:i,
                    $el:$card,
                    $img:$img
                };
                self.items.push(item);
                self.itemsByPath[item.path] = item;
            });
        },

        _imageLoaded: function() {

            if (--this.pendingImages == 0) {
//                console.log("all images loaded");
                // force relayout
                this.numCols = -1;
                this.layout();
            }

        },
        updateDimensions: function() {
            var self = this;
            this.items.every(function (i) {
                var $el = i.$el;

                i.w = $el.width();
                i.h = $el.height();

                // check if card has an image and if it's loaded
                if (i.$img) {
                    // Hack: Recalculate element size if browser has wrong values. This sometimes occurs with loaded
                    // images when the elements are not yet displayed on screen.
                    if (i.$img.width() > i.w) {
                        i.h = (i.h - i.$img.height()) + (i.$img.height() / i.$img.width() * i.w);
                    }

                    if (i.$img.height() == 0) {
                        // just assume 1:1 for now
                        i.h += i.w;
                        self.pendingImages++;
                        i.$img.on("load.cui.gridlayout", function() {
                            i.$img = null;
                            i.w = $el.width(); // Set width AND height to ensure proper ratio
                            i.h = $el.height();
                            
//                            console.log("image loaded.", i);
                            self._imageLoaded();
                        })
                    } else {
                        // we don't need to know this info anymore
                        i.$img = null;
                    }
                }

                // debug
//                $("h4", i.$el).html("Card Nr " + i.i + " (" + i.w + "x" + i.h + ")");

                return true;
            });

        },

        layout:function () {
            var self = this;
            var $this = this.element;
            var colWidth = this.options.colWidth;
            var marginLeft = this.options.marginLeft;
            var marginRight = this.options.marginRight;
            var gx = this.options.gutterX;
            if ($this.width() === 0) {
	        //need not to layout the div whose width is 0
                return;
            }
            // calculate # columns:
            // containerWidth = marginLeft + (colWidth + gx) * n - gx + marginRight;
            // use: "round" for avg width, "floor" for minimal width, "ceil" for maximal width
            var n = Math.floor(($this.width() - marginLeft - marginRight + gx) / (colWidth + gx));

            if (n < 1) n = 1; // Minimum 1 column!

            if (n == this.numCols) {
                // nothing to do. CSS takes care of the scaling
                return;
            }
            
            this.numCols = n;

            // calculate actual column width:
            // containerWidth = marginLeft + (cw + gx) * n - gx + marginRight;
            var cw =  (($this.width() - marginLeft - marginRight + gx) / n) - gx;

            // initialize columns
            var cols = [];
            var colHeights = [];
            while (cols.length < n) {
                cols.push([]);
                colHeights.push(0);
            }

            this.items.every(function (i) {
                // determine height of card, based on the ratio
                var height = (i.h / i.w) * cw;
                
                // find lowest column
                var min = colHeights[0];
                var best = 0;
                for (var c = 0; c < colHeights.length; c++) {
                    var h = colHeights[c];
                    if (h < min) {
                        min = h;
                        best = c;
                    }
                }

                // update columns and heights array
                cols[best].push(i);
                colHeights[best] += height + self.options.gutterY;
                return true;
            });

            // detach all the cards first
            $this.detach(this.options.selector);

            // remember old columns. because otherwise the
            // event handlers bound on the cards would be removed
            var $cols = $this.contents();

            // now fill up all the columns
            for (var c=0; c<cols.length; c++) {
                var $col = $('<div class="grid-'+n+'"></div>').appendTo($this);
                for (var j=0; j<cols[c].length; j++) {
                    $col.append(cols[c][j].$el);
                }
            }

            // remove old columns
            $cols.remove();
        },

        destroy: function() {
            $window.unbind("smartresize.cui.gridlayout");
            this.element.removeData("cuigridlayout");
        }
    };

    var logError = function (message) {
        if (window.console) {
            window.console.error(message);
        }
    };

    // plugin bridge
    $.fn.cuigridlayout = function (options, callback) {
        if (typeof options === 'string') {
            // call method
            var args = Array.prototype.slice.call(arguments, 1);

            this.each(function () {
                var instance = $.data(this, 'cuigridlayout');
                if (!instance) {
                    logError("cannot call methods on cuigridlayout prior to initialization; " +
                            "attempted to call method '" + options + "'");
                    return;
                }
                if (!$.isFunction(instance[options]) || options.charAt(0) === "_") {
                    logError("no such method '" + options + "' for cuigridlayout instance");
                    return;
                }
                // apply method
                instance[ options ].apply(instance, args);
            });
        } else {
            this.each(function () {
                var instance = $.data(this, 'cuigridlayout');
                if (instance) {
                    // apply options & init
                    instance.option(options);
                    instance._init(callback);
                } else {
                    // initialize new instance
                    $.data(this, 'cuigridlayout', new $.CUIGridLayout(options, this, callback));
                }
            });
        }
        // return jQuery object
        // so plugin methods do not have to
        return this;
    };
})(window, jQuery);
