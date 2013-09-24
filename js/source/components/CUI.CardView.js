/*************************************************************************
*
* ADOBE CONFIDENTIAL
* ___________________
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
**************************************************************************/

(function($) {

    var DISPLAY_GRID = "grid";

    var DISPLAY_LIST = "list";

    var SELECTION_MODE_COUNT_SINGLE = "single";

    var SELECTION_MODE_COUNT_MULTI = "multiple";

    var DEFAULT_SELECTOR_CONFIG = {

        "itemSelector": "article",                      // selector for getting items
        "headerSelector": "header",                     // selector for headers
        "dataContainer": "grid-0",                      // class name of the data container
        "enableImageMultiply": true,                    // flag if images should be multiplied
        "view": {
            "selectedItem": {                           // defines what classes (cls) on what elements (selector; optional) are used to mark a selection
                "list": {
                    "cls": "selected"
                },
                "grid": {
                    "cls": "selected"
                }
            },
            "selectedItems": {                          // defines the selector that is used for determining the current selection; a resolver function may be specified that adjusts the selection (for exmaple by determining a suitable parent element)
                "list": {
                    "selector": "article.selected"
                },
                "grid": {
                    "selector": "article.selected"
                }
            }
        },
        "controller": {
            "selectElement": {                          // defines the selector that is used for installing the tap/click handlers
                "list": "article > i.select",
                /* "listNavElement": "article", */      // may be used to determine the element that is responsible for navigating in list view (required only if different from the Grid's select item)
                "grid": "article"
            },
            "moveHandleElement": {                      // defines the selector that is used to determine the object that is responsible for moving an item in list view
                "list": "article > i.move"
            },
            "targetToItem": {                           // defines methods that are used to resolve the event target of a tap/click event to a card view item
                "list": function($target) {
                    return $target.closest("article");
                },
                "grid": function($target) {
                    return $target.closest("article");
                },
                "header": function($target) {
                    return $target.closest("header");
                }
            },
            "gridSelect": {                             // defines the class that is used to trigger the grid selection mode
                "cls": "selection-mode"
            },
            "selectAll": {                              // defines the "select all" config (list view only)
                "selector": "header > i.select",
                "cls": "selected"
            },
            "sort": {                                   // defines the config for column sort
                "columnSelector": ".label > *"
            }
        }

    };

    var ensureItem = function(item) {
        if (item.jquery) {
            return item.data("cardView-item");
        }
        return item;
    };

    /**
     * @classdesc
     * Internal class that provides utility functionality to the card view.
     */
    var Utils = {

        /**
         * Check two jQuery objects for equality.
         * @param {jQuery} $1 first jQuery object
         * @param {jQuery} $2 second jQuery object
         * @return {boolean} True if both objects are considered equal
         */
        equals: function($1, $2) {
            return ($1.length === $2.length) && ($1.length === $1.filter($2).length);
        },

        /**
         * Gets a CardView widget for the specified jQuery object.
         * @param {jQuery} $el The jQuery object
         * @return {CUI.CardView} The widget
         */
        getWidget: function($el) {
            var widget;
            if ($el.length > 0) {
                widget = $($el[0]).data("cardView");
            }
            return widget;
        },

        /**
         * Mixes two objects so that for every missing property in object1 the properties of object2 are used. This is also done
         * for nested objects.
         * @param {object} Object 1
         * @param {object} Object 2
         * @return {object} The mixed object with all properties
        */
        mixObjects: function(object1, object2) {
            if (object1 === undefined) return object2;

            var result = {};
            for(var i in object2) {
                if (object1[i] === undefined) {
                    result[i] = object2[i];
                    continue;
                }
                var p = object1[i];

                // Go one step deeper in the object hierarchy if we find an object that is not a string.
                // Note: typeof returns "function" for functions, so no special testing for functions needed.
                if (typeof(object1[i]) == "object" && (!(object1[i] instanceof String))) {
                    p = this.mixObjects(object1[i], object2[i]);
                }
                result[i] = p;
            }
            return result;
        },

        /**
         * Resolves each of the DOM elements in the specified jQuery object with a given
         * function into a new jQuery object.
         * @param {jQuery} $el The jQuery object to resolve
         * @param {Function} fn The resolver function
         * @return {jQuery} The resolved jQuery object
         */
        resolve: function($el, fn) {
            var resolved = [ ];
            $el.each(function() {
                resolved.push.apply(resolved, fn($(this)).toArray());
            });
            return $(resolved);
        },

        /**
         * Multiplies the image with the provided color. This will insert a canvas element
         * before the img element.
         * @param {HTMLElement} $images image element to multiply with the color
         * @param {Number[]} color RGB array of values between 0 and 1
         */
        multiplyImages: function($images, color) {
            // Filter out images where the multiply effect has already been inserted to the DOM, or images that aren't visible
            $images = $images.filter(function () {
                var $image = $(this);
                return !$image.is(".multiplied") && !$image.prev().is(".multiplied") && $image.is(":visible");
            });

            var imageMaxCounter = $images.length;
            var imageIteratorCounter = 0;
            
            function multiplyNextImage() {
                if (imageIteratorCounter < imageMaxCounter) {
                    // Not adding the timeout for the first image will make it feel more reactive.
                    multiplyOneImage($images[imageIteratorCounter]);

                    imageIteratorCounter++;

                    // But adding a timeout for the other images will make it non-blocking.
                    setTimeout(multiplyNextImage, 0);
                }
            }

            function multiplyOneImage(image) {
                var width  = image.naturalWidth,
                    height = image.naturalHeight;

                // defer if image is not yet available
                if ((width === 0) && (height === 0)) {
                    window.setTimeout(function() {
                        multiplyOneImage(image);
                    }, 200);
                    return;
                }

                var canvas = $("<canvas width='" + width + "' height='" + height+"'></canvas>")[0];

                var context = canvas.getContext("2d");
                context.drawImage(image, 0, 0, width, height);

                var imageData = context.getImageData(0, 0, width, height);
                var data = imageData.data;

                for (var i = 0, l = data.length; i < l; i += 4) {
                    data[i]   *= color[0];
                    data[i+1] *= color[1];
                    data[i+2] *= color[2];
                }

                context.putImageData(imageData, 0, 0);

                // re-sizing of canvases are handled different in IE and Opera, thus we have to use an image
                $("<img class='" + image.className + " multiplied' " +
                    "width='" + width + "' height='" + height + "' " +
                    "src='" + canvas.toDataURL("image/png") + "'/>").insertBefore(image);
            }

            multiplyNextImage();
        }
    };

    var ListItemAutoScroller = new Class(/** @lends CUI.CardView.ListItemAutoScroller# */{

        /**
         * The item element
         * @type jQuery
         * @private
         */
        $el: null,

        /**
         * The scrolling container element (with overflow auto/visible and position
         * absolute)
         * @type jQuery
         * @private
         */
        $containerEl: null,

        /**
         * Size of a scrolling step (= number of pixels that get scrolled per autoscrolling
         * 'tick'
         * @type Number
         * @private
         */
        stepSize: 0,

        /**
         * Timeout ID
         * @type Number
         * @private
         */
        iid: undefined,

        /**
         * @private
         */
        autoMoveOffset: 0,

        /**
         * The maximum value that is allowed for scrollTop while autoscrolling
         * @type Number
         * @private
         */
        maxScrollTop: 0,


        /**
         * @ignore
         * @name CUI.CardView.ListItemAutoScroller
         * @classdesc
         * Internal class that implements auto scrolling while moving cards in list view.
         *
         * @desc
         * Creates a new auto scroller.
         *
         * @constructs
         *
         * @param {jQuery} $el The jQuery container element of the card item that is moved
         * @param {Number} stepSize The step size (number of pixels scrolled per 'tick')
         * @param {Function} autoMoveFn Function that gets executed on every auto scroll
         *        "event". The function must actually reposition the item element to
         *        the coordinate specified as parameters (first parameter: x; second
         *        parameter: y
         */
        construct: function($el, stepSize, autoMoveFn) {
            this.$el = $el;
            this.stepSize = stepSize;
            this.$containerEl = this._getScrollingContainer($el);
            var cont = this.$containerEl[0];
            this.maxScrollTop = Math.max(cont.scrollHeight - cont.clientHeight, 0);
            this.autoMoveFn = autoMoveFn;
        },

        /**
         * Gets a suitable container element that limits the scrolling area.
         * @param {jQuery} $el The card item's container element
         * @return {jQuery} The container element
         * @private
         */
        _getScrollingContainer: function($el) {
            while (($el.length > 0) && !$el.is("body")) {
                var ovflY =  $el.css("overflowY");
                var pos = $el.css("position");
                if (((ovflY === "auto") || (ovflY === "visible")) && (pos === "absolute")) {
                    return $el;
                }
                $el = $el.parent();
            }
            return $(window);
        },

        /**
         * Checks if scrolling is necessary according to the item element's current position
         * and the scroll container's state and executes a single scrolling step by
         * adjusting the scroll container's scrollTop property if necessary.
         * @return {Boolean} True if scrolling occured; false if no scrolling was necessary
         * @private
         */
        _execute: function() {
            var cont = this.$containerEl[0];
            var clientHeight = cont.clientHeight;
            var scrollTop = cont.scrollTop;
            var itemTop = this.$el.offset().top - this.$containerEl.offset().top;
            var itemBottom = itemTop + this.$el.height();
            var isAutoScroll = false;
            if (itemTop <= 0) {
                // auto scroll upwards
                if (scrollTop > 0) {
                    scrollTop -= this.stepSize;
                    this.autoMoveOffset = -this.stepSize;
                    if (scrollTop < 0) {
                        scrollTop = 0;
                    }
                    cont.scrollTop = scrollTop;
                    isAutoScroll = true;
                }
            } else if (itemBottom >= clientHeight) {
                // auto scroll downwards
                if (scrollTop < this.maxScrollTop) {
                    scrollTop += this.stepSize;
                    this.autoMoveOffset = this.stepSize;
                    if (scrollTop > this.maxScrollTop) {
                        scrollTop = this.maxScrollTop;
                    }
                    cont.scrollTop = scrollTop;
                    isAutoScroll = true;
                }
            }
            return isAutoScroll;
        },

        /**
         * Moves the card item's element by calculating its new position and calling
         * the function that was provided in the constructor as autoMoveFn.
         * @private
         */
        _autoMove: function() {
            if (this.autoMoveOffset && this.autoMoveFn) {
                var itemOffs = this.$el.offset();
                var itemTop = itemOffs.top + this.autoMoveOffset;
                this.autoMoveFn(itemOffs.left, itemTop);
            }
        },

        /**
         * Checks if autoscrolling is currently necessary; if so, the autoscrolling is
         * executed and a timer is started that will check again later if additional
         * auto scrolling is necessary (and execute again if so).
         */
        check: function() {
            var self = this;
            this.stop();
            var isAutoScroll = this._execute();
            if (isAutoScroll) {
                this.iid = window.setTimeout(function() {
                    self.iid = undefined;
                    self._autoMove();
                }, 50);
            }
        },

        /**
         * Stops autoscrolling.
         */
        stop: function() {
            if (this.iid !== undefined) {
                window.clearTimeout(this.iid);
                this.autoMoveOffset = 0;
                this.iid = undefined;
            }
        }

    });

    var ListItemMoveHandler = new Class(/** @lends CUI.CardView.ListItemMoveHandler# */{

        /**
         * The list's jQuery element
         * @type {jQuery}
         * @private
         */
        $listEl: null,

        /**
         * The moved item's jQuery element
         * @type {jQuery}
         * @private
         */
        $itemEl: null,

        /**
         * A jQuery element that contains all items of the list
         * @type {jQuery}
         * @private
         */
        $items: null,

        /**
         * The document
         * @type {jQuery}
         * @private
         */
        $doc: null,

        /**
         * The jQuery object that represents the card that is right before the moved card
         * just before the move; undefined, if the moved card is the first card of the list
         * @type {jQuery}
         * @private
         */
        $oldBefore: null,

        /**
         * The CSS class that is added to the item while it is moved
         * @type String
         * @private
         */
        dragCls: null,

        /**
         * Flag that determines if the horizontal position of an item should be fixed
         * while it is moved
         * @type {Boolean}
         * @private
         */
        fixHorizontalPosition: false,

        /**
         * The autoscroller module
         * @type {ListItemAutoScroller}
         * @private
         */
        autoScroller: null,

        /**
         * @ignore
         * @name CUI.CardView.ListItemMoveHandler
         *
         * @classdesc
         * Internal class that implements the reordering of cards in list view.
         *
         * @constructor
         * @desc
         * Creates a new handler for moving a item around in a list by drag &amp; drop (or
         * its touch counterpart).
         */
        construct: function(config) {
            var self = this;
            this.$listEl = config.$listEl;
            this.$itemEl = config.$itemEl;
            this.$items = config.$items;
            this.dragCls = config.dragCls;
            this.fixHorizontalPosition = (config.fixHorizontalPosition !== false);
            this.autoScroller = (config.autoScrolling ?
                    new ListItemAutoScroller(this.$itemEl, 8, function(x, y) {
                        self._autoMove(x, y);
                    }) : undefined);
        },

        /**
         * Gets the page coordinates of the specified event, regardless if it is a mouse
         * or a touch event.
         * @param {Object} e The event
         * @return {{x: (Number), y: (Number)}} The page coordinates
         * @private
         */
        _getEventCoords: function(e) {
            if (!e.originalEvent.touches) {
                return {
                    x: e.pageX,
                    y: e.pageY
                };
            }
            return (e.originalEvent.touches.length > 0 ? {
                x: e.originalEvent.touches[0].pageX,
                y: e.originalEvent.touches[0].pageY
            } : e.originalEvent.changedTouches.length > 0 ? {
                x: e.originalEvent.changedTouches[0].pageX,
                y: e.originalEvent.changedTouches[0].pageY
            } : {
                x: e.pageX,
                y: e.pageY
            });
        },

        /**
         * Limits the specified coordinates to the list's real estate.
         * @param {Number} top vertical coordinate to limit
         * @param {Number} left horizontal coordinate to limit
         * @return {{top: *, left: *}}
         * @private
         */
        _limit: function(top, left) {
            if (left < this.listOffset.left) {
                left = this.listOffset.left;
            }
            if (top < this.listOffset.top) {
                top = this.listOffset.top;
            }
            var right = left + this.size.width;
            var bottom = top + this.size.height;
            var limitRight = this.listOffset.left + this.listSize.width;
            var limitBottom = this.listOffset - top + this.listSize.height;
            if (right > limitRight) {
                left = limitRight - this.size.width;
            }
            if (bottom > limitBottom) {
                top = limitBottom - this.size.height;
            }
            if (this.fixHorizontalPosition) {
                left = this.listOffset.left;
            }
            return {
                "top": top,
                "left": left
            };
        },

        /**
         * Gets the coordinates of the specified event in a device (mouse, touch) agnostic
         * way.
         * @param {Object} e The event
         * @return {{x: Number, y: Number}} The coordinates
         * @private
         */
        _getEventPos: function(e) {
            var evtPos = this._getEventCoords(e);
            return {
                x: evtPos.x - this.delta.left,
                y: evtPos.y - this.delta.top
            };
        },

        /**
         * Adjust the position of the moved item by limiting it to the containing list
         * and executing autoscrolling.
         * @param {Number} x The original x coordinate
         * @param {Number} y The original y coordinate
         * @private
         */
        _adjustPosition: function(x, y) {
            this.$itemEl.offset(this._limit(y, x));
            if (this.autoScroller) {
                this.autoScroller.check();
            }
        },

        /**
         * Changes the order of the items in the list if necessary.
         * @private
         */
        _changeOrderIfRequired: function() {
            var itemPos = this.$itemEl.offset();
            var hotX = itemPos.left + (this.size.width / 2);
            var hotY = itemPos.top + (this.size.height / 2);
            var $newTarget = null;
            // check if we are overlapping another item at least 50% -> then we will take
            // its position
            var isInsertBefore = false;
            for (var i = 0; i < this.$items.length; i++) {
                var $item = $(this.$items[i]);
                if (!Utils.equals($item, this.$itemEl)) {
                    var offs = $item.offset();
                    var width = $item.width();
                    var height = $item.height();
                    var bottom = offs.top + height;
                    if ((hotX >= offs.left) && (hotX < offs.left + width) &&
                            (hotY >= offs.top) && (hotY < bottom)) {
                        isInsertBefore = ((hotY - offs.top) > (bottom - hotY));
                        $newTarget = $item;
                        break;
                    }
                }
            }
            if ($newTarget) {
                var _offs = this.$itemEl.offset();
                if (isInsertBefore) {
                    $newTarget.before(this.$itemEl);
                } else {
                    $newTarget.after(this.$itemEl);
                }
                this.$itemEl.offset(_offs);
            }
        },

        /**
         * Callback for auto move (called by auto scroller implementation)
         * @param x {Number} The horizontal position
         * @param y {Number} The vertical position
         * @private
         */
        _autoMove: function(x, y) {
            this._adjustPosition(x, y);
            this._changeOrderIfRequired();
        },

        /**
         * Starts moving the list item.
         * @param {Object} e The event that starts the move
         */
        start: function(e) {
            this.$oldPrev = this.$itemEl.prev();
            this.$oldNext = this.$itemEl.next();

            var evtPos = this._getEventCoords(e);
            if (this.dragCls) {
                this.$itemEl.addClass(this.dragCls);
            }
            var self = this;
            this.$doc = $(document);
            this.$doc.fipo("touchmove.listview.drag", "mousemove.listview.drag",
                function(e) {
                    self.move(e);
                });
            this.$doc.fipo("touchend.listview.drag", "mouseup.listview.drag",
                function(e) {
                    self.end(e);
                });
            this.offset = this.$itemEl.offset();
            this.delta = {
                "left": evtPos.x - this.offset.left,
                "top": evtPos.y - this.offset.top
            };
            this.size = {
                "width": this.$itemEl.width(),
                "height": this.$itemEl.height()
            };
            this.listOffset = this.$listEl.offset();
            this.listSize = {
                "width": this.$listEl.width(),
                "height": this.$listEl.height()
            };
            e.stopPropagation();
            e.preventDefault();
            /*
            console.log("offset", this.offset, "delta", this.delta, "size", this.size,
                "listoffs", this.listOffset, "listsize", this.listSize);
            */
        },

        /**
         * Moves the card item.
         * @param {Object} e The event that is responsible for the move
         */
        move: function(e) {
            // console.log("move", e);
            var pos = this._getEventPos(e);
            this._adjustPosition(pos.x, pos.y);
            this._changeOrderIfRequired();
            e.stopPropagation();
            e.preventDefault();
        },

        /**
         * Finishes moving the card item.
         * @param {Object} e The event that is responsible for finishing the move
         */
        end: function(e) {
            var pos = this._getEventPos(e);
            this._adjustPosition(pos.x, pos.y);
            // console.log("end", e);
            if (this.dragCls) {
                this.$itemEl.removeClass(this.dragCls);
            }
            if (this.autoScroller) {
                this.autoScroller.stop();
            }
            this.$itemEl.css("position", "");
            this.$itemEl.css("top", "");
            this.$itemEl.css("left", "");
            this.$doc.off("touchmove.listview.drag");
            this.$doc.off("mousemove.listview.drag");
            this.$doc.off("touchend.listview.drag");
            this.$doc.off("mouseup.listview.drag");
            var $newPrev = this.$itemEl.prev();
            var $newNext = this.$itemEl.next();

            this.$itemEl.trigger($.Event("item-moved", {
                newPrev: $newPrev,
                newNext: $newNext,
                oldPrev: this.$oldPrev,
                oldNext: this.$oldNext,
                hasMoved: !Utils.equals($newPrev, this.$oldPrev)
            }));
            e.stopPropagation();
            e.preventDefault();
        }

    });

    /*
     * This class represents a single item in the list model.
     */
    var Item = new Class(/** @lends CUI.CardView.Item# */{

        /**
         * The jQuery object that represents the card
         * @type {jQuery}
         * @private
         */
        $itemEl: null,

        /**
         * @ignore
         * @name CUI.CardView.Item
         *
         * @classdesc
         * Internal class that represents a single card/list item in a card view's data
         * model.
         *
         * @constructor
         * @desc
         * Create a new card/list item.
         */
        construct: function($itemEl) {
            this.$itemEl = $itemEl;
            this.reference();
        },

        /**
         * Get the card/list item's jQuery object.
         * @return {jQuery} The jQuery object
         */
        getItemEl: function() {
            return this.$itemEl;
        },

        /**
         * References the item's data model in the jQzery object.
         */
        reference: function() {
            this.$itemEl.data("cardView-item", this);
        }

    });

    var Header = new Class(/** @lends CUI.CardView.Header# */{

        /**
         * The jQuery object that represents the header
         * @type {jQuery}
         * @private
         */
        $headerEl: null,

        /**
         * The first item that follows the header
         * @type {CUI.CardView.Item}
         */
        itemRef: null,

        /**
         * @ignore
         * @name CUI.CardView.Header
         *
         * @classdesc
         * This class represents a list header (that is shown in list view only) in the
         * card view's data model.
         *
         * @constructor
         * @desc
         * Create a new list header.
         */
        construct: function($headerEl, itemRef) {
            this.$headerEl = $headerEl;
            this.itemRef = itemRef;
        },

        /**
         * Get the jQuery object that is assosciated with the list header.
         * @return {jQuery} The associated jQuery object
         */
        getHeaderEl: function() {
            return this.$headerEl;
        },

        /**
         * Get the list item that follows the header directly.
         * @return {CUI.CardView.Item} The item
         */
        getItemRef: function() {
            return this.itemRef;
        },

        /**
         * Set the list item that follows the header directly.
         * @param {CUI.CardView.Item} itemRef The item
         */
        setItemRef: function(itemRef) {
            this.itemRef = itemRef;
        }

    });

    /**
        Handles resort according to columns
    */
    var ColumnSortHandler = new Class(/** @lends CUI.CardView.ColumnSortHandler# */{
        construct: function(options) {
            this.model = options.model;
            this.comparators = options.comparators;
            this.selectors = options.selectors;
            this.columnElement = options.columnElement;

            this.headerElement = options.columnElement.closest(this.selectors.headerSelector);                    
            var header = this.model.getHeaderForEl(this.headerElement);
            this.items = this.model.getItemsForHeader(header);

            this.isReverse = this.columnElement.hasClass("sort-asc"); // switch to reverse?
            this.toNatural = this.columnElement.hasClass("sort-desc"); // back to natural order?
            this.fromNatural = !this.headerElement.hasClass("sort-mode");
            
            this.comparator = null;

            // Choose the right comparator
            if (this.comparators) {
                for(var selector in this.comparators) {
                    if (!this.columnElement.is(selector)) continue;
                    this.comparator = this.comparators[selector];
                }
            }

            if (!this.comparator) this.comparator = this._readComparatorFromMarkup();
        },
        _readComparatorFromMarkup: function() {
            var selector = this.columnElement.data("sort-selector");
            var attribute = this.columnElement.data("sort-attribute");
            var sortType = this.columnElement.data("sort-type");
            if (!selector && !attribute) return null;
            return new CUI.CardView.DefaultComparator(selector, attribute, sortType);

        },
        _adjustMarkup: function() {
            // Adjust general mode class
            if (this.fromNatural) this.headerElement.addClass("sort-mode");
            if (this.toNatural) this.headerElement.removeClass("sort-mode");
            
            // Adjust column classes
            this.headerElement.find(this.selectors.controller.sort.columnSelector).removeClass("sort-asc sort-desc");
            this.columnElement.removeClass("sort-desc sort-asc");
            if (!this.toNatural) this.columnElement.addClass(this.isReverse ? "sort-desc" : "sort-asc");

            // Show or hide d&d elements 
            var showMoveHandle = this.toNatural;
            $.each(this.items, function() {this.getItemEl().find(".move").toggle(showMoveHandle);});
        },
        sort: function() {
            if (!this.comparator && !this.toNatural) return;

            this._adjustMarkup();

            // Re-Sort items
            var items = this.items.slice(); // Make a copy before sorting
            // By default items are in their "natural" order, most probably defined by the user with d&d

            // Only sort if we have a comparator
            if (this.comparator) {
                this.comparator.setReverse(this.isReverse);
                var fn = this.comparator.getCompareFn();
                if (!this.toNatural) items.sort(fn);   // Only sort if we do not want to go back to natural order       
            }

            // Adjust DOM
            var prevItem = this.headerElement; // Use header as starting point;
            $.each(this.items, function() {this.getItemEl().detach();}); // First: Detach all items
           
            // Now: reinsert in new order
            for(var i = 0; i < items.length; i++) {
                var item = items[i].getItemEl();
                prevItem.after(item);
                prevItem = item;
            }
        }
    });



    var DirectMarkupModel = new Class(/** @lends CUI.CardView.DirectMarkupModel# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * List of items; original/current sorting order (without UI sorting applied)
         * @type {CUI.CardView.Item[]}
         * @private
         */
        items: null,

        /**
         * List of headers
         * @type {CUI.CardView.Header[]}
         * @private
         */
        headers: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupModel
         *
         * @classdesc
         * This class represents a data model that is created via a selector from an
         * existing DOM.
         *
         * @constructor
         * @desc
         * Create a new data model.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         */
        construct: function($el, selectors) {
            this.$el = $el;
            this.items = [ ];
            this.selectors = selectors;
            var $items = this.$el.find(selectors.itemSelector);
            var itemCnt = $items.length;
            for (var i = 0; i < itemCnt; i++) {
                this.items.push(new Item($($items[i])));
            }
            this.headers = [ ];
            var $headers = this.$el.find(selectors.headerSelector);
            var headerCnt = $headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var $header = $($headers[h]);
                var $itemRef = $header.nextAll(selectors.itemSelector);
                var itemRef = ($itemRef.length > 0 ?
                        this.getItemForEl($($itemRef[0])) : undefined);
                this.headers.push(new Header($header, itemRef));
            }
        },

        /**
         * Initialize the data model.
         */
        initialize: function() {
            var self = this;
            this.$el.on("item-moved", this.selectors.itemSelector, function(e) {
                if (e.hasMoved) {
                    self._reorder(e);
                }
            });
        },

        /**
         * Reorder the cards according to the specified event.
         * @param {Event} e The reordering event
         * @private
         */
        _reorder: function(e) {
            var itemToMove = this.getItemForEl($(e.target));
            var newBefore = this.getItemForEl(e.newPrev);
            var isHeaderInsert = false;
            var header;
            if (!newBefore) {
                header = this.getHeaderForEl(e.newPrev);
                if (header) {
                    isHeaderInsert = true;
                    var refPos = this.getItemIndex(header.getItemRef());
                    if (refPos > 0) {
                        newBefore = this.getItemAt(refPos - 1);
                    }
                }
            }
            var oldPos = this.getItemIndex(itemToMove);
            this.items.splice(oldPos, 1);
            // if the item to move is directly following a header, the header's item ref
            // has to be updated
            var headerRef = this._getHeaderByItemRef(itemToMove);
            if (headerRef) {
                headerRef.setItemRef(this.getItemAt(oldPos));
            }
            var insertPos = (newBefore ? this.getItemIndex(newBefore) + 1 : 0);
            this.items.splice(insertPos, 0, itemToMove);
            if (isHeaderInsert) {
                header.setItemRef(itemToMove);
            }
            // console.log(itemToMove, newBefore, isHeaderInsert);
        },

        /**
         * Get the number of cards/list items.
         * @return {Number} The number of cards/list items
         */
        getItemCount: function() {
            return this.items.length;
        },

        /**
         * Get the card/list item that is at the specified list position.
         * @param {Number} pos The position
         * @return {CUI.CardView.Item} The item at the specified position
         */
        getItemAt: function(pos) {
            return this.items[pos];
        },

        /**
         * Get the list position of the specified card/list item.
         * @param {CUI.CardView.Item} item The item
         * @return {Number} The list position; -1 if the specified item is not a part of
         *         the list
         */
        getItemIndex: function(item) {
            for (var i = 0; i < this.items.length; i++) {
                if (item === this.items[i]) {
                    return i;
                }
            }
            return -1;
        },

        /**
         * Get the card/list item that is associated with the specified jQuery object.
         * @param {jQuery} $el The jQuery object
         * @return {CUI.CardView.Item} The item; undefined if no item is associated with
         *         the specified jQuery object
         */
        getItemForEl: function($el) {
            var itemCnt = this.items.length;
            for (var i = 0; i < itemCnt; i++) {
                var item = this.items[i];
                if (Utils.equals(item.getItemEl(), $el)) {
                    return item;
                }
            }
            return undefined;
        },

        /**
         * <p>Inserts the specified card(s)/list item(s) at the given position.</p>
         * <p>Please note that you can specify multiple items either as an array of jQuery
         * objects or a single jQuery object that contains multiple DOM objects, each
         * representing an item.</p>
         * @param {jQuery|jQuery[]} $items The item(s) to insert
         * @param {Number} pos The position to insert
         * @param {Boolean} beforeHeader True if the items should added before headers (only
         *        applicable if the items are inserted directly at a position where also
         *        a header is present); needs to be false if the list has a single header
         *        that is placed at the top of the list
         */
        insertItemAt: function($items, pos, beforeHeader) {
            if (!$.isArray($items)) {
                $items = $items.toArray();
            }
            for (var i = $items.length - 1; i >= 0; i--) {

                var $item = $items[i];
                if (!$item.jquery) {
                    $item = $($item);
                }

                // adjust model
                var followupItem;
                var item = new Item($item);
                if ((pos === undefined) || (pos === null)) {
                    this.items.push(item);
                    pos = this.items.length - 1;
                } else {
                    followupItem = this.items[pos];
                    this.items.splice(pos, 0, item);
                }
                var insert = {
                    "item": followupItem,
                    "mode": "item"
                };

                // adjust header references if item is inserted directly behind a header
                var headerCnt = this.headers.length;
                for (var h = 0; h < headerCnt; h++) {
                    var header = this.headers[h];
                    if (header.getItemRef() === followupItem) {
                        if (beforeHeader) {
                            insert = {
                                "item": header,
                                "mode": "header"
                            };
                            break;
                        } else {
                            header.setItemRef(item);
                        }
                    }
                }

                // trigger event
                this.$el.trigger($.Event("change:insertitem", {
                    "insertPoint": insert,
                    "followupItem": followupItem,
                    "item": item,
                    "pos": pos,
                    "widget": Utils.getWidget(this.$el),
                    "moreItems": (i > 0)
                }));
            }
        },

        /**
         * Get the number of list headers.
         * @return {Number} The number of headers
         */
        getHeaderCount: function() {
            return this.headers.length;
        },

        /**
         * Get a list header by its position in the list of headers.
         * @param {Number} pos The list header
         * @return {CUI.CardView.Header} The list header at the specified position
         */
        getHeaderAt: function(pos) {
            return this.headers[pos];
        },

        /**
         * Get all list headers.
         * @return {CUI.CardView.Header[]} List headers
         */
        getHeaders: function() {
            var headers = [ ];
            headers.push.apply(headers, this.headers);
            return headers;
        },

        /**
         * Get the list header that is associated with the specified jQuery object.
         * @param {jQuery} $el The jQuery object
         * @return {CUI.CardView.Header} The list header; undefined if no header is
         *         associated with the jQuery object
         */
        getHeaderForEl: function($el) {
            var headerCnt = this.headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var header = this.headers[h];
                if (Utils.equals(header.getHeaderEl(), $el)) {
                    return header;
                }
            }
            return undefined;
        },

        /**
         * Get the header that directly precedes the specified list item.
         * @param {CUI.CardView.Item} itemRef The item
         * @return {CUI.CardView.Header} header The header
         * @private
         */
        _getHeaderByItemRef: function(itemRef) {
            for (var h = 0; h < this.headers.length; h++) {
                if (this.headers[h].getItemRef() === itemRef) {
                    return this.headers[h];
                }
            }
            return undefined;
        },

        /**
         * Get all list items that are preceded by the specified header.
         * @param header {CUI.CardView.Header} The header
         * @return {CUI.CardView.Item[]} The list items
         */
        getItemsForHeader: function(header) {
            // TODO does not handle empty headers yet
            var itemRef = header.getItemRef();
            var headerCnt = this.headers.length;
            var itemCnt = this.items.length;
            var itemsForHeader = [ ];
            var isInRange = false;
            for (var i = 0; i < itemCnt; i++) {
                var item = this.items[i];
                if (isInRange) {
                    for (var h = 0; h < headerCnt; h++) {
                        if (this.headers[h].getItemRef() === item) {
                            isInRange = false;
                            break;
                        }
                    }
                    if (isInRange) {
                        itemsForHeader.push(item);
                    } else {
                        break;
                    }
                } else {
                    if (item === itemRef) {
                        isInRange = true;
                        itemsForHeader.push(itemRef);
                    }
                }
            }
            return itemsForHeader;
        },

        /**
         * Get the list items (data model) from their associated DOM objects.
         * @param {jQuery} $elements The jQuery object that specifies the items' DOM
         *        objects
         * @return {CUI.CardView.Item[]} List items
         */
        fromItemElements: function($elements) {
            var items = [ ];
            $elements.each(function() {
                var item = $(this).data("cardView-item");
                if (item) {
                    items.push(item);
                }
            });
            return items;
        },

        /**
         * Write back references to the data model to the respective DOM objects.
         */
        reference: function() {
            var itemCnt = this.items.length;
            for (var i = 0; i < itemCnt; i++) {
                this.items[i].reference();
            }
        },

        /**
         * Removes all items without triggering respective events.
         */
        removeAllItemsSilently: function() {
            this.items.length = 0;
            for (var h = 0; h < this.headers.length; h++) {
                this.headers[h].setItemRef(undefined);
            }
        }

    });

    var DirectMarkupView = new Class(/** @lends CUI.CardView.DirectMarkupView# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupView
         *
         * @classdesc
         * This class represents a view for data represented by a DirectMarkupModel.
         *
         * @constructor
         * @desc
         * Create a new view.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         */
        construct: function($el, selectors) {
            this.$el = $el;
            this.selectors = selectors;
        },

        /**
         * Initializes the view.
         */
        initialize: function() {
            var self = this;
            this.$el.on("change:displayMode", function(e) {
                var oldMode = e.oldValue;
                var newMode = e.value;
                self.cleanupAfterDisplayMode(oldMode);
                self.prepareDisplayMode(newMode);
            });
            this.$el.on("change:insertitem", function(e) {
                self._onItemInserted(e);
            });
            this.$el.reflow({
                "small": function ($el, size) {
                    return $el.width() > 40*size.rem() && $el.width() < 50*size.rem();
                },
                "xsmall": function ($el, size) {
                    return $el.width() > 30*size.rem() && $el.width() < 40*size.rem();
                },
                "xxsmall": function ($el, size) {
                    return $el.width() < 30*size.rem();
                }
            });
        },

        /**
         * Handler that adjusts the view after a new card/list item has been inserted.
         * @param {Event} e The event
         * @private
         */
        _onItemInserted: function(e) {
            var $dataRoot = this.$el;
            if (this.selectors.dataContainer) {
                $dataRoot = $dataRoot.find("." + this.selectors.dataContainer);
            }
            var $item = e.item.getItemEl();
            var followupItem = e.followupItem;
            switch (this.getDisplayMode()) {
                case DISPLAY_LIST:
                    if (!followupItem) {
                        $dataRoot.append($item);
                    } else {
                        var insert = e.insertPoint;
                        var item = insert.item;
                        var $ref = (insert.mode === "item" ?
                            item.getItemEl() : item.getHeaderEl());
                        $ref.before($item);
                    }
                    break;
                case DISPLAY_GRID:
                    if (!e.moreItems) {
                        var widget = Utils.getWidget(this.$el);
                        widget._restore();
                        widget.layout();
                    }
                    break;
            }
        },

        /**
         * Get the current display mode (grid view/list view)
         * @return {String} The display mode; defined by constants prefixed by DISPLAY_
         */
        getDisplayMode: function() {
            return Utils.getWidget(this.$el).getDisplayMode();
        },

        /**
         * Set the selection state of the specified item.
         * @param {CUI.CardView.Item} item The item
         * @param {String} selectionState The selection state; currently supported:
         *        "selected", "unselected"
         */
        setSelectionState: function(item, selectionState) {
            var displayMode = this.getDisplayMode();
            var selectorDef = this.selectors.view.selectedItem[displayMode];
            var $itemEl = item.getItemEl();
            if (selectorDef.selector) {
                $itemEl = $itemEl.find(selectorDef.selector);
            }
            if (selectionState === "selected") {
                $itemEl.addClass(selectorDef.cls);
                if (displayMode === DISPLAY_GRID) {
                    this._drawSelectedGrid(item);
                }
            } else if (selectionState === "unselected") {
                $itemEl.removeClass(selectorDef.cls);
            }
        },

        /**
         * Get the selection state of the specified item.
         * @param {CUI.CardView.Item} item The item
         * @return {String} The selection state; currently supported: "selected",
         *         "unselected"
         */
        getSelectionState: function(item) {
            var selectorDef = this.selectors.view.selectedItem[this.getDisplayMode()];
            var $itemEl = item.getItemEl();
            if (selectorDef.selector) {
                $itemEl = $itemEl.find(selectorDef.selector);
            }
            var cls = selectorDef.cls.split(" ");
            for (var c = 0; c < cls.length; c++) {
                if (!$itemEl.hasClass(cls[c])) {
                    return "unselected";
                }
            }
            return "selected";
        },

        /**
         * Get a list of currently selected items.
         * @return {jQuery} The list of selected items
         */
        getSelectedItems: function() {
            var selectorDef = this.selectors.view.selectedItems[this.getDisplayMode()];
            var $selectedItems = this.$el.find(selectorDef.selector);
            if (selectorDef.resolver) {
                $selectedItems = selectorDef.resolver($selectedItems);
            }
            return $selectedItems;
        },

        /**
         * <p>Restors the card view.</p>
         * <p>The container is purged and the cards are re-inserted in original order
         * (note that this is necessary, because the item elements get reordered for
         * card view; original order has to be restored for list view),</p>
         * @param {CUI.CardView.DirectMarkupModel} model The data model
         * @param {Boolean} restoreHeaders True if header objects should be restored as
         *        well (for list view)
         */
        restore: function(model, restoreHeaders) {
            var $container = $("<div class='" + this.selectors.dataContainer + "'>");
            this.$el.empty();
            this.$el.append($container);
            var itemCnt = model.getItemCount();
            for (var i = 0; i < itemCnt; i++) {
                $container.append(model.getItemAt(i).getItemEl());
            }
            if (restoreHeaders) {
                var headerCnt = model.getHeaderCount();
                for (var h = 0; h < headerCnt; h++) {
                    var header = model.getHeaderAt(h);
                    var $headerEl = header.getHeaderEl();
                    var itemRef = header.getItemRef();
                    if (itemRef) {
                        itemRef.getItemEl().before($headerEl);
                    } else {
                        $container.append($headerEl);
                    }
                }
            }
        },

        /**
         * Prepares the specified display mode (grid vs. list view).
         * @param {String} displayMode The display mode ({@link CUI.CardView.DISPLAY_GRID},
         *        {@link CUI.CardView.DISPLAY_LIST})
         */
        prepareDisplayMode: function(displayMode) {
            if (displayMode === DISPLAY_GRID) {
                this._drawAllSelectedGrid();
            }
        },

        /**
         * Clean up before the specified display mode is left.
         * @param {String} displayMode The display mode ({@link CUI.CardView.DISPLAY_GRID},
         *        {@link CUI.CardView.DISPLAY_LIST})
         */
        cleanupAfterDisplayMode: function(displayMode) {
            // not yet required; may be overridden
        },

        /**
         * Draw the multiplied version (used for displaying a selection) of the specified
         * image.
         * @param {jQuery} $image The image
         * @private
         */
        _drawImage: function($image) {
            if ($image.length === 0) {
                return;
            }

            if (this._colorFloat === undefined) {
                var color256     = $image.closest("a").css("background-color");     // Let's grab the color form the card background
                this._colorFloat = $.map(color256.match(/(\d+)/g), function (val) { // RGB values between 0 and 1
                    return val/255;
                });
            }

            Utils.multiplyImages($image, this._colorFloat);
        },

        /**
         * Create the multiplied images for selected state (in grid view) for all cards.
         * @private
         */
        _drawAllSelectedGrid: function() {
            if (!this.selectors.enableImageMultiply) {
                return;
            }
            var self = this;
            var selector = this.selectors.view.selectedItems.grid.selector + " img";
            var $selector = $(selector);

            this._drawImage($selector);
            $selector.load(function() {
                self._drawImage($(this));
            });
        },

        /**
         * Create the multiplied image for the selected state of the specified card (in
         * grid view).
         * @param {CUI.CardView.Item} item The card/list item
         * @private
         */
        _drawSelectedGrid: function(item) {
            if (!this.selectors.enableImageMultiply) {
                return;
            }
            var self = this;
            var $img = item.getItemEl().find("img");

            this._drawImage($img);
            $img.load(function() {
                self._drawImage($(this));
            });
        },

        /**
         * Removes all items from the view without triggering respective events.
         */
        removeAllItemsSilently: function() {
            this.$el.find(this.selectors.itemSelector).remove();
        }

    });

    var DirectMarkupController = new Class(/** @lends CUI.CardView.DirectMarkupController# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,
        
        /**
         * comparator config for list sorting
         * @type {Object}
         * @private
         */
        comparators: null,

        /**
         * The selection mode
         * @type {String}
         * @private
         */
        selectionModeCount: null,

        /**
         * Flag that is used for a workaround for touch devices
         * @type {Boolean}
         * @private
         */
        _listSelect: false,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupController
         *
         * @classdesc
         * This class implements the controller for data represented by DirectMarkupModel
         * and displayed by DirectMarkupView.
         *
         * @constructor
         * @desc
         * Create a new controller.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         * @param {Object} comparators The comparator config for column sorting
         */
        construct: function($el, selectors, comparators) {
            this.$el = $el;
            this.selectors = selectors;
            this.comparators = comparators;
            this.selectionModeCount = SELECTION_MODE_COUNT_MULTI;
        },

        /**
         * Initializes the controller
         */
        initialize: function() {
            this.setDisplayMode(this.$el.hasClass("list") ? DISPLAY_LIST : DISPLAY_GRID);
            var self = this;

            // Selection
            this.$el.fipo("tap.cardview.select", "click.cardview.select",
                this.selectors.controller.selectElement.list, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    if (widget.getDisplayMode() === DISPLAY_LIST) {
                        var item = ensureItem(self.getItemElFromEvent(e));
                        if (widget.toggleSelection(item)) {
                            e.stopPropagation();
                            e.preventDefault();
                        }
                        if (e.type === "tap") {
                            self._listSelect = true;
                        }
                    }
                });
            this.$el.fipo("tap.cardview.select", "click.cardview.select",
                this.selectors.controller.selectElement.grid, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    if ((widget.getDisplayMode() === DISPLAY_GRID) &&
                            widget.isGridSelectionMode()) {
                        var item = ensureItem(self.getItemElFromEvent(e));
                        if (widget.toggleSelection(item)) {
                            e.stopPropagation();
                            e.preventDefault();
                        }
                    }
                });
            // list header
            this.$el.fipo("tap.cardview.selectall", "click.cardview.selectall",
                this.selectors.controller.selectAll.selector, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    if (widget.getDisplayMode() === DISPLAY_LIST) {
                        var cls = self.selectors.controller.selectAll.cls;
                        var $header = self.selectors.controller.targetToItem.header(
                                $(e.target));
                        var header = widget.getModel().getHeaderForEl($header);
                        if ($header.hasClass(cls)) {
                            widget.deselectAll(header);
                        } else {
                            widget.selectAll(header);
                        }
                    }
                });
                
            // list sorting
            this.$el.fipo("tap.cardview.sort", "click.cardview.sort",
                this.selectors.headerSelector + " " + this.selectors.controller.sort.columnSelector, function(e) {
                    
                    var widget = Utils.getWidget(self.$el);
                    var model = widget.getModel();

                    // Trigger a sortstart event
                    var event = $.Event("sortstart");
                    $(e.target).trigger(event);
                    if (event.isDefaultPrevented()) return;

                    var sorter = new ColumnSortHandler({
                        model: model,
                        columnElement: $(e.target),
                        comparators: self.comparators,
                        selectors: self.selectors
                    });
                    sorter.sort();

                    // Trigger an sortend event
                    event = $.Event("sortend");
                    $(e.target).trigger(event);
                });

            // Prevent text selection of headers!
            this.$el.on("selectstart.cardview", this.selectors.headerSelector + " " + this.selectors.controller.sort.columnSelector, function(e) {
                e.preventDefault();
            });

            // block click event for cards on touch devices
            this.$el.finger("click.cardview.select",
                this.selectors.controller.selectElement.grid, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    var dispMode = widget.getDisplayMode();
                    if ((dispMode === DISPLAY_GRID) && widget.isGridSelectionMode()) {
                        e.stopPropagation();
                        e.preventDefault();
                    }
                });
            // block click event for list items on touch devices if the click actually
            // represents a change in selection rather than navigating
            var listNavElement = this.selectors.controller.selectElement.listNavElement ||
                    this.selectors.controller.selectElement.grid;
            this.$el.finger("click.cardview.select",
                listNavElement, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    var dispMode = widget.getDisplayMode();
                    if ((dispMode === DISPLAY_LIST) && self._listSelect) {
                        e.stopPropagation();
                        e.preventDefault();
                    }
                    self._listSelect = false;
                });
            // reordering
            this.$el.fipo("touchstart.cardview.reorder", "mousedown.cardview.reorder",
                this.selectors.controller.moveHandleElement.list, function(e) {
                    var $itemEl = self.getItemElFromEvent(e);
                    var handler = new ListItemMoveHandler({
                        $listEl: self.$el,
                        $itemEl: $itemEl,
                        $items: $(self.selectors.itemSelector),
                        dragCls: "dragging",
                        autoScrolling: true
                    });
                    handler.start(e);
                });
            // handle select all state
            this.$el.on("change:selection", function(e) {
                if (e.moreSelectionChanges) {
                    return;
                }
                self._adjustSelectAllState(e.widget);
            });
            this.$el.on("change:insertitem", function(e) {
                if (e.moreItems) {
                    return;
                }
                self._adjustSelectAllState(e.widget);
            });
        },

        /**
         * Adjusts the state of the "select all" element of all list headers.
         * @param {CUI.CardView} widget The card view widget
         * @private
         */
        _adjustSelectAllState: function(widget) {
            var cls = this.selectors.controller.selectAll.cls;
            var selectionState = widget.getHeaderSelectionState();
            var headers = selectionState.headers;
            var headerCnt = headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var header = headers[h];
                var $header = header.header.getHeaderEl();
                if (header.hasUnselected) {
                    $header.removeClass(cls);
                } else {
                    $header.addClass(cls);
                }
            }
        },

        /**
         * Resolves the target of the specified event to a jQuery element that represents
         * a card.
         * @param {Event} e The event
         * @return {jQuery} The jQuery object that represents a card
         */
        getItemElFromEvent: function(e) {
            var $target = $(e.target);
            var resolver = this.selectors.controller.targetToItem[this.getDisplayMode()];
            if ($.isFunction(resolver)) {
                return resolver($target);
            }
            return $target.find(resolver);
        },

        /**
         * Checks if selection mode is enabled for grid view.
         * @return {Boolean} True if selection mode is enabled
         */
        isGridSelect: function() {
            var selectorDef = this.selectors.controller.gridSelect;
            var $el = this.$el;
            if (selectorDef.selector) {
                $el = $el.find(selectorDef.selector);
            }
            return $el.hasClass(selectorDef.cls);
        },

        /**
         * Set selection mode for grid view.
         * @param {Boolean} isGridSelect True to turn selection mode on
         */
        setGridSelect: function(isGridSelect) {
            if (this.isGridSelect() !== isGridSelect) {
                var selectorDef = this.selectors.controller.gridSelect;
                var $el = this.$el;
                if (selectorDef.selector) {
                    $el = $el.find(selectorDef.selector);
                }
                if (isGridSelect) {
                    $el.addClass(selectorDef.cls);
                } else {
                    $el.removeClass(selectorDef.cls);
                    Utils.getWidget($el).clearSelection();
                }
                this.$el.trigger($.Event("change:gridSelect", {
                    "widget": this.$el.data("cardView"),
                    "oldValue": !isGridSelect,
                    "value": isGridSelect
                }));
            }
        },

        /**
         * Get current display mode (grid/list view).
         * @return {String} Display mode ({@link CUI.CardView.DISPLAY_GRID},
         *         {@link CUI.CardView.DISPLAY_LIST})
         */
        getDisplayMode: function() {
            if (this.$el.hasClass("list")) {
                return DISPLAY_LIST;
            }
            if (this.$el.hasClass("grid")) {
                return DISPLAY_GRID;
            }
            return null;
        },

        /**
        * @return {boolean} true if this widget is currently in list mode and has a column sorting on any header applied
        */
        isColumnSorted: function() {
            return (this.getDisplayMode() == "list") && this.$el.find(this.selectors.headerSelector).filter(".sort-mode").length > 0;
        },

        /**
         * Set display mode.
         * @param {String} displayMode Display mode ({@link CUI.CardView.DISPLAY_GRID},
         *        {@link CUI.CardView.DISPLAY_LIST})
         */
        setDisplayMode: function(displayMode) {
            var oldValue = this.getDisplayMode();
            if (oldValue !== displayMode) {
                var widget = Utils.getWidget(this.$el);
                widget._restore(displayMode === DISPLAY_LIST);
                switch (displayMode) {
                    case DISPLAY_GRID:
                        this.$el.removeClass("list");
                        this.$el.addClass("grid");
                        if (oldValue !== null) {
                            var selection = widget.getSelection();
                            this.setGridSelect(selection.length > 0);
                            widget.layout();
                        }
                        break;
                    case DISPLAY_LIST:
                        this.$el.cuigridlayout("destroy");
                        this.$el.removeClass("grid");
                        this.$el.addClass("list");
                        break;
                }
                this.$el.trigger($.Event("change:displayMode", {
                    "widget": this.$el.data("cardView"),
                    "oldValue": oldValue,
                    "value": displayMode
                }));
            }
        },

        /**
         * Get selection mode (single/multiple).
         * @return {String} The selection mode;
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI}
         */
        getSelectionModeCount: function() {
            return this.selectionModeCount;
        },

        /**
         * Set selection mode (single/multiple).
         * @param {String} modeCount The selection mode;
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI}
         */
        setSelectionModeCount: function(modeCount) {
            this.selectionModeCount = modeCount;
        }

    });

    var DirectMarkupAdapter = new Class(/** @lends CUI.CardView.DirectMarkupAdapter# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,

        /**
         * comparator config
         * @type {Object}
         * @private
         */
        comparators: null,

        /**
         * The model
         * @type {CUI.CardView.DirectMarkupModel}
         * @private
         */
        model: null,

        /**
         * The view
         * @type {CUI.CardView.DirectMarkupView}
         * @private
         */
        view: null,

        /**
         * The controller
         * @type {CUI.CardView.DirectMarkupController}
         * @private
         */
        controller: null,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupAdapter
         *
         * @classdesc
         * Internal class that wires model, controller and view.
         *
         * @constructor
         * @desc
         * Create a new adapter.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         */
        construct: function(selectors, comparators) {
            this.selectors = selectors;
            this.comparators = comparators;
        },

        /**
         * Initialize the adapter (and the wrapped model, view & controller).
         * @param {jQuery} $el The card view's parent element
         */
        initialize: function($el) {
            this.$el = $el;
            this.setModel(new DirectMarkupModel($el, this.selectors));
            this.setView(new DirectMarkupView($el, this.selectors));
            this.setController(new DirectMarkupController($el, this.selectors, this.comparators));
            this.model.initialize();
            this.view.initialize();
            this.controller.initialize();
        },

        /**
         * Set the model.
         * @param {CUI.CardView.DirectMarkupModel} model The model
         */
        setModel: function(model) {
            this.model = model;
        },

        /**
         * Get the model.
         * @return {CUI.CardView.DirectMarkupModel} The model
         */
        getModel: function() {
            return this.model;
        },

        /**
         * Set the view.
         * @param {CUI.CardView.DirectMarkupView} view The view
         */
        setView: function(view) {
            this.view = view;
        },

        /**
         * Get the view.
         * @return {CUI.CardView.DirectMarkupView} The view
         */
        getView: function() {
            return this.view;
        },

        /**
         * Set the controller.
         * @param {CUI.CardView.DirectMarkupController} controller The controller
         */
        setController: function(controller) {
            this.controller = controller;
        },

        /**
         * Get the controller.
         * @return {CUI.CardView.DirectMarkupController} The controller
         */
        getController: function() {
            return this.controller;
        },

        /**
         * Check if the specified card/list item is selected.
         * @param {CUI.CardView.Item} item The card/item
         * @return {Boolean} True if it is selected
         */
        isSelected: function(item) {
            var selectionState = this.view.getSelectionState(item);
            return (selectionState === "selected");
        },

        /**
         * Set the selection state of zhe specified card/list item.
         * @param {CUI.CardView.Item} item The card/item
         * @param {Boolean} isSelected True if it is selected
         */
        setSelected: function(item, isSelected) {
            var selectionState = (isSelected ? "selected" : "unselected");
            this.view.setSelectionState(item, selectionState);
        },

        /**
         * Get a list of selected items
         * @param {Boolean} useModel True if {@link CUI.CardView.Item}s should be returned;
         *        false for jQuery objects
         * @return {CUI.CardView.Item[]|jQuery}
         */
        getSelection: function(useModel) {
            var selection = this.view.getSelectedItems();
            if (useModel === true) {
                selection = this.model.fromItemElements(selection);
            }
            return selection;
        },

        /**
         * Get the display mode.
         * @return {String} The display mode ({@link CUI.CardView.DISPLAY_GRID} or
         *         {@link CUI.CardView.DISPLAY_LIST})
         */
        getDisplayMode: function() {
            return this.controller.getDisplayMode();
        },

        /**
        * @return {boolean} true if this widget is currently in list mode and has a column sorting on any header applied
        */
        isColumnSorted: function() {
            return this.controller.isColumnSorted();
        },        

        /**
         * Set the display mode.
         * @param {String} selectionMode The display mode ({@link CUI.CardView.DISPLAY_GRID}
         *        or {@link CUI.CardView.DISPLAY_LIST})
         */
        setDisplayMode: function(selectionMode) {
            this.controller.setDisplayMode(selectionMode);
        },

        /**
         * Check if selection mode is active in grid view.
         * @return {Boolean} True if selection mode is active
         */
        isGridSelectionMode: function() {
            return this.controller.isGridSelect();
        },

        /**
         * Set if selection mode is active in grid view.
         * @param {Boolean} isSelectionMdoe True if selection mode is active
         */
        setGridSelectionMode: function(isSelectionMode) {
            this.controller.setGridSelect(isSelectionMode);
        },

        /**
         * Get the general selection mode (single/multiple items)
         * @return {String} The selection mode
         *         ({@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI})
         */
        getSelectionModeCount: function() {
            return this.controller.getSelectionModeCount();
        },

        /**
         * Set the general selection mode (single/multiple items)
         * @param {String} modeCount The selection mode
         *        ({@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *        {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI})
         */
        setSelectionModeCount: function(modeCount) {
            this.controller.setSelectionModeCount(modeCount);
        },

        /**
         * Restores the opriginal DOM structure of the widget.
         * @param {Boolean} restoreHeaders True if list headers should also be restored
         *        (list view)
         * @protected
         */
        _restore: function(restoreHeaders) {
            this.view.restore(this.model, restoreHeaders);
            this.model.reference();
        },

        /**
         * Removes all items from the card view.
         */
        removeAllItems: function() {
            var widget = Utils.getWidget(this.$el);
            widget.clearSelection();
            this.model.removeAllItemsSilently();
            this.view.removeAllItemsSilently();
        }

    });

    CUI.CardView = new Class(/** @lends CUI.CardView# */{

        toString: 'CardView',

        extend: CUI.Widget,

        adapter: null,


        /**
         * @extends CUI.Widget
         * @classdesc
         * <p>A display of cards that can either be viewed as a grid or a list.</p>
         * <p>The display mode - grid or list view - can be changed programmatically
         * whenever required.</p>
         * <p>Grid view has two modes: navigation and selection, which can also be switched
         * programmatically. In navigation mode, the user can use cards to navigate
         * hierarchical structures ("to another stack of cards"). In selection mode, the
         * cards get selected on user interaction instead. List view combines both selection
         * and navigation modes.</p>
         * <p>The card view uses a data model internally that abstracts the cards. This
         * data model is currently not opened as API. Therefore you will often encounter
         * unspecified objects that represent cards in the data model. You can use them
         * interchangibly (for example, if one method returns a card data object, you can
         * pass it to another method that takes a card data object as a parameter), but
         * you shouldn't assume anything about their internals. You may use
         * {@link CUI.CardView#prepend}, {@link CUI.CardView#append} and
         * {@link CUI.CardView#removeAllItems} to manipulate the data model.</p>
         * <p>Please note that the current implementation has some limitiations which are
         * documented if known. Subsequent releases of CoralUI will remove those limitations
         * bit by bit.</p>
         * <p>The following example shows two cards in grid view:</p>
         *
<div class="grid" data-toggle="cardview">
    <div class="grid-0">
        <article class="card-default">
            <i class="select"></i>
            <i class="move"></i>
            <a href="#">
                <span class="image">
                    <img class="show-grid" src="images/preview.png" alt="">
                    <img class="show-list" src="images/preview-small.png" alt="">
                </span>
                <div class="label">
                    <h4>A card</h4>
                    <p>Description</p>
                </div>
            </a>
        </article>
        <article class="card-default">
            <i class="select"></i>
            <i class="move"></i>
            <a href="#">
                <span class="image">
                    <img class="show-grid" src="images/preview.png" alt="">
                    <img class="show-list" src="images/preview-small.png" alt="">
                </span>
                <div class="label">
                    <h4>Another card</h4>
                    <p>See shell example page for more info.</p>
                </div>
            </a>
        </article>
    </div>
</div>
         *
         * @example
<caption>Instantiate with Class</caption>
// Currently unsupported.
         *
         * @example
<caption>Instantiate with jQuery</caption>
// Currently unsupported.
         *
         * @example
<caption>Markup</caption>
&lt;div class="grid" data-toggle="cardview"&gt;
    &lt;div class="grid-0"&gt;
        &lt;article class="card-default"&gt;
            &lt;i class="select"&gt;&lt;/i&gt;
            &lt;i class="move"&gt;&lt;/i&gt;
            &lt;a href="#"&gt;
                &lt;span class="image"&gt;
                    &lt;img class="show-grid" src="images/preview.png" alt=""&gt;
                    &lt;img class="show-list" src="images/preview-small.png" alt=""&gt;
                &lt;/span&gt;
                &lt;div class="label"&gt;
                    &lt;h4&gt;A card&lt;/h4&gt;
                    &lt;p&gt;Description&lt;/p&gt;
                &lt;/div&gt;
            &lt;/a&gt;
        &lt;/article&gt;
    &lt;/div&gt;
&lt;/div&gt;
         * @example
<caption>Defining comparators for column sorting</caption>
//  Define a selector for the column and then a comparator to be used for sorting
// The comparator
var comparatorConfig = {".label .main": new CUI.CardView.DefaultComparator(".label h4", null, false),
                   ".label .published": new CUI.CardView.DefaultComparator(".label .published", "data-timestamp", true)};
new CUI.CardView({comparators: comparatorConfig})

         * @example
<caption>Defining comparators via data API</caption>
&lt;!-- Page header for list view --&gt;
&lt;header class="card-page selectable movable"&gt;
    &lt;i class="select"&gt;&lt;/i&gt;
    &lt;i class="sort"&gt;&lt;/i&gt;
    &lt;div class="label"&gt;
        &lt;div class="main" data-sort-selector=".label h4"&gt;Title&lt;/div&gt;
        &lt;div class="published" data-sort-selector=".label .published .date" data-sort-attribute="data-timestamp" data-sort-type="numeric"&gt;Published&lt;/div&gt;
        &lt;div class="modified" data-sort-selector=".label .modified .date" data-sort-attribute="data-timestamp" data-sort-type="numeric"&gt;Modified&lt;/div&gt;
        &lt;div class="links" data-sort-selector=".label .links-number" data-sort-type="numeric"&gt;&lt;i class="icon-arrowright"&gt;Links&lt;/i&gt;&lt;/div&gt;
    &lt;/div&gt;
&lt;/header&gt;
&lt;!--
    Sorting is started when the user clicks on the corresponding column header.

    data-sort-selector   defines which part of the item to select for sorting
    data-sort-attribute  defines which attribute of the selected item element should be user for sorting. If not given, the inner text is used.
    data-sort-type       if set to "numeric", a numerical comparison is used for sorting, an alphabetical otherwise
--&gt;

         * @example
<caption>Switching to grid selection mode using API</caption>
$cardView.cardView("toggleGridSelectionMode");
         *
         * @example
<caption>Switching to grid selection mode using CSS contract</caption>
$cardView.toggleClass("selection-mode");
$cardView.find("article").removeClass("selected");
         *
         * @desc Creates a new card view.
         * @constructs
         *
         * @param {Object} [options] Component options
         * @param {Object} [options.selectorConfig]
         *        The selector configuration. You can also omit configuration values: Values not given will be used from
         *        the default selector configuration.
         * @param {String} options.selectorConfig.itemSelector
         *        The selector that is used to retrieve the cards from the DOM
         * @param {String} options.selectorConfig.headerSelector
         *        The selector that is used to retrieve the header(s) in list view from the
         *        DOM
         * @param {String} options.selectorConfig.dataContainer
         *        The class of the div that is used internally for laying out the cards
         * @param {Boolean} options.selectorConfig.enableImageMultiply
         *        Flag that determines if the images of cards should use the "multiply
         *        effect" for display in selected state
         * @param {Object} options.selectorConfig.view
         *        Configures the view of the CardView
         * @param {Object} options.selectorConfig.view.selectedItem
         *        Defines what classes on what elements are used to select a card
         * @param {Object} options.selectorConfig.view.selectedItem.list
         *        Defines the selection-related config in list view
         * @param {String} options.selectorConfig.view.selectedItem.list.cls
         *        Defines the CSS class that is used to select a card in list view
         * @param {String} [options.selectorConfig.view.selectedItem.list.selector]
         *        An additioonal selector if the selection class has to be set on a child
         *        element rather than the card's parent element
         * @param {Object} options.selectorConfig.view.selectedItem.grid
         *        Defines the selection-related config in grid view
         * @param {String} options.selectorConfig.view.selectedItem.grid.cls
         *        Defines the CSS class that is used to select a card in grid view
         * @param {String} [options.selectorConfig.view.selectedItem.grid.selector]
         *        An additioonal selector if the selection class has to be set on a child
         *        element rather than the card's parent element
         * @param {Object} options.selectorConfig.view.selectedItems
         *        Defines how to determine the currently selected cards
         * @param {Object} options.selectorConfig.view.selectedItems.list
         *        Defines how to determine the currently selected cards in list view
         * @param {String} options.selectorConfig.view.selectedItems.list.selector
         *        The selector that determines the DOM elements that represent all currently
         *        selected cards
         * @param {Function} [options.selectorConfig.view.selectedItems.list.resolver]
         *        A function that is used to calculate a card's parent element from the
         *        elements that are returned from the selector that is used for determining
         *        selected cards
         * @param {Object} options.selectorConfig.view.selectedItems.grid
         *        Defines how to determine the currently selected cards in grid view
         * @param {String} options.selectorConfig.view.selectedItems.grid.selector
         *        The selector that determines the DOM elements that represent all currently
         *        selected cards
         * @param {Function} [options.selectorConfig.view.selectedItems.grid.resolver]
         *        A function that is used to calculate a card's parent element from the
         *        elements that are returned from the selector that is used for determining
         *        selected cards
         * @param {Object} options.selectorConfig.controller
         *        Configures the controller of the CardView
         * @param {Object} options.selectorConfig.controller.selectElement
         *        The selector that defines the DOM element that is used for selecting
         *        a card (= targets for the respective click/tap handlers)
         * @param {String} options.selectorConfig.controller.selectElement.list
         *        The selector that defines the event targets for selecting a card in list
         *        view
         * @param {String} [options.selectorConfig.controller.selectElement.listNavElement]
         *        An additional selector that may be used to determine the element that is
         *        used for navigating in list view if it is different from the event target
         *        defined by options.selectorConfig.controller.selectElement.grid
         * @param {String} options.selectorConfig.controller.selectElement.grid
         *        The selector that defines the event targets for selecting a card in grid
         *        view
         * @param {Object} options.selectorConfig.controller.moveHandleElement
         *        The selector that defines the DOM elements that are used for moving
         *        cards in list view (= targets for the respective mouse/touch handlers)
         * @param {String} options.selectorConfig.controller.moveHandleElement.list
         *        The selector that defines the event targets for the handles that are used
         *        to move a card in list view
         * @param {Object} options.selectorConfig.controller.targetToItems
         *        Defines the mapping from event targets to cards
         * @param {Function|String} options.selectorConfig.controller.targetToItems.list
         *        A function that takes a jQuery object that represents the event target for
         *        selecting a card in list view and that has to return the jQuery object
         *        that represents the entire card; can optionally be a selector as well
         * @param {Function|String} options.selectorConfig.controller.targetToItems.grid
         *        A function that takes a jQuery object that represents the event target for
         *        selecting a card in grid view and that has to return the jQuery object t
         *        hat represents the entire card; can optionally be a selector as well
         * @param {Function|String} options.selectorConfig.controller.targetToItems.header
         *        A function that takes a jQuery object that represents the event target for
         *        the "select all" button of a header in list view and that has to return
         *        the jQuery object that represents the respective header; can optionally
         *        be a selector as well
         * @param {Object} options.selectorConfig.controller.gridSelect
         *        Defines the selection mode in grid view
         * @param {Object} options.selectorConfig.controller.gridSelect.cls
         *        Defines the class that is used to switch to selection mode in grid view
         * @param {Object} options.selectorConfig.controller.gridSelect.selector
         *        An additional selector that is used to define the child element where the
         *        selection mode class should be applied to/read from
         * @param {Object} options.selectorConfig.controller.selectAll
         *        Defines how to select all cards in list view
         * @param {Object} options.selectorConfig.controller.selectAll.selector
         *        The selector that is used to determine all "select all" buttons in a
         *        CardView
         * @param {Object} options.selectorConfig.controller.sort
         *        Defines selectors for the column sorting mechanism.
         * @param {Object} options.selectorConfig.controller.sort.columnSelector
         *        The selector for all column objects within the header 
         * @param {Object} options.gridSettings
         *        Custom options for jQuery grid layout plugin.
         * @param {Object} options.selectorConfig.controller.selectAll.cls
         *        The class that has to be applied to each card if "select all" is invoked
         * @param {Object} [options.comparators]
         *        An associative array of comparators for column sorting: Every object attribute is a CSS selector
         *        defining one column and its value has to be of type CUI.CardView.DefaultComparator (or your own derived class)      
        */
        construct: function(options) {
            // Mix given selector config with defaults: Use given config and add defaults, where no option is given
            var selectorConfig = Utils.mixObjects(options.selectorConfig, DEFAULT_SELECTOR_CONFIG);
            var comparators = options.comparators || null;

            this.adapter = new DirectMarkupAdapter(selectorConfig, comparators);
            this.adapter.initialize(this.$element);
            this.layout(options.gridSettings);
        },

        /**
         * Get the underlying data model.
         * @return {*} The underlying data model
         * @private
         */
        getModel: function() {
            return this.adapter.getModel();
        },

        /**
         * Set the underlying data model.
         * @param {*} model The underlying data model
         * @private
         */
        setModel: function(model) {
            this.adapter.setModel(model);
        },

        /**
         * Check if the specified item (part of the data model) is currently selected.
         * @param {*} item The item (data mode) to check
         * @return {Boolean} True if the specified item is selected
         * @private
         */
        isSelected: function(item) {
            return this.adapter.isSelected(item);
        },

        /**
         * Get the current display mode (grid or list view).
         * @return {String} The display mode; either {@link CUI.CardView.DISPLAY_GRID} or
         *         {@link CUI.CardView.DISPLAY_LIST}
         */
        getDisplayMode: function() {
            return this.adapter.getDisplayMode();
        },

       /**
        * @return {boolean} true if this widget is currently in list mode and has a column sorting on any header applied
        */
        isColumnSorted: function() {
            return this.adapter.isColumnSorted();
        },

        /**
        * @param {boolean} sortable     Set to true if this list should be sortable by click on column
        */
        setColumnSortable: function(sortable) {
            // TODO implement
        },

        /**
        * @return {boolean} True if this list is column sortable (does not say if it is currently sorted, use isColumnSorted() for this)
        */
        isColumnSortable: function() {
            // TODO implement
        },

        /**
         * Set the display mode (grid or list view).
         * @param {String} displayMode The display mode; either
         *        {@link CUI.CardView.DISPLAY_GRID} or {@link CUI.CardView.DISPLAY_LIST}
         */
        setDisplayMode: function(displayMode) {
            this.adapter.setDisplayMode(displayMode);
        },

        /**
         * Checks if selection mode is currently active in grid view.
         * @return {Boolean} True if selection mode is active
         */
        isGridSelectionMode: function() {
            return this.adapter.isGridSelectionMode();
        },

        /**
         * Set the selection mode in grid view.
         * @param {Boolean} isSelection True to switch grid selection mode on
         */
        setGridSelectionMode: function(isSelection) {
            this.adapter.setGridSelectionMode(isSelection);
        },

        /**
         * Toggle selection mode in grid view.
         */
        toggleGridSelectionMode: function() {
            this.setGridSelectionMode(!this.isGridSelectionMode());
        },

        getSelectionModeCount: function() {
            return this.adapter.getSelectionModeCount();
        },

        setSelectionModeCount: function(modeCount) {
            this.adapter.setSelectionModeCount(modeCount);
        },

        /**
         * <p>Select the specified item.</p>
         * <p>The second parameter should be used if multiple cards are selected/deselected
         * at once. It prevents some time consuming stuff from being executed more than
         * once.</p>
         * @param {jQuery|*} item The item to select; may either be from data model or a
         *        jQuery object
         * @param {Boolean} moreSelectionChanges True if there are more selection changes
         *        following directly
         */
        select: function(item, moreSelectionChanges) {
            // TODO implement beforeselect event
            item = ensureItem(item);
            var isSelected = this.adapter.isSelected(item);
            if (!isSelected) {
                if (this.getSelectionModeCount() === SELECTION_MODE_COUNT_SINGLE &&
                    this.getSelection().length > 0) {
                    this.clearSelection();
                }

                this.adapter.setSelected(item, true);
                this.$element.trigger($.Event("change:selection", {
                    "widget": this,
                    "item": item,
                    "isSelected": true,
                    "moreSelectionChanges": (moreSelectionChanges === true)
                }));
            }
        },

        /**
         * <p>Deselect the specified card.</p>
         * <p>The second parameter should be used if multiple cards are selected/deselected
         * at once. It prevents some time consuming stuff from being executed more than
         * once.</p>
         * @param {jQuery|*} item The item to deselect; may either be from data model or a
         *        jQuery object
         * @param {Boolean} moreSelectionChanges True if there are more selection changes
         *        following directly
         */
        deselect: function(item, moreSelectionChanges) {
            // TODO implement beforeselect event
            item = ensureItem(item);
            var isSelected = this.adapter.isSelected(item);
            if (isSelected) {
                this.adapter.setSelected(item, false);
                this.$element.trigger($.Event("change:selection", {
                    "widget": this,
                    "item": item,
                    "isSelected": false,
                    "moreSelectionChanges": moreSelectionChanges
                }));
            }
        },

        /**
         * <p>Toggle the selection state of the specified item.</p>
         * <p>The second parameter should be used if multiple cards are selected/deselected
         * at once. It prevents some time consuming stuff from being executed more than
         * once.</p>
         * @param {jQuery|*} item The item; may be either from data model or a jQuery object
         * @param {Boolean} moreSelectionChanges True if there are more selection changes
         *        following directly
         * @return {Boolean} True if the toggle requires the originating event (if any)
         *         to be stopped and to prevent browser's default behavior
         */
        toggleSelection: function(item, moreSelectionChanges) {
            item = ensureItem(item);

            // allow to cancel & stop the event
            var beforeEvent = $.Event("beforeselect", {

                selectionCancelled: false,

                stopEvent: false,

                item: item,

                cancelSelection: function(stopEvent) {
                    this.selectionCancelled = true;
                    this.stopEvent = (stopEvent === true);
                }
            });
            this.$element.trigger(beforeEvent);
            if (beforeEvent.selectionCancelled) {
                return beforeEvent.stopEvent;
            }

            var isSelected = this.isSelected(item);
            if (!isSelected &&
                    (this.getSelectionModeCount() === SELECTION_MODE_COUNT_SINGLE) &&
                    (this.getSelection().length > 0)) {
                this.clearSelection();
            }

            this.adapter.setSelected(item, !isSelected);
            this.$element.trigger($.Event("change:selection", {
                "widget": this,
                "item": item,
                "isSelected": !isSelected,
                "moreSelectionChanges": moreSelectionChanges
            }));
            return true;
        },

        /**
         * Gets the currently selected cards.
         * @param {Boolean} useModel True if items from the data model should be retured;
         *        false, if a jQuery object should be returned instead
         * @return {Array|jQuery} The selected items
         */
        getSelection: function(useModel) {
            return this.adapter.getSelection(useModel === true);
        },

        /**
         * Clears the current selection state by deselecting all selected cards.
         */
        clearSelection: function() {
            var selection = this.getSelection(true);
            var itemCnt = selection.length;
            var finalItem = (itemCnt - 1);
            for (var i = 0; i < itemCnt; i++) {
                this.deselect(selection[i], (i < finalItem));
            }
        },

        /**
         * @private
         */
        _headerSel: function(headers, selectFn, lastValidItemFn) {
            var model = this.adapter.getModel();
            if (headers == null) {
                headers = model.getHeaders();
            }
            if (!$.isArray(headers)) {
                headers = [ headers ];
            }
            var headerCnt = headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var header = headers[h];
                if (header.jquery) {
                    header = model.getHeaderForEl(header);
                }
                var itemsToSelect = model.getItemsForHeader(header);
                var itemCnt = itemsToSelect.length;
                for (var i = 0; i < itemCnt; i++) {
                    selectFn.call(this,
                            itemsToSelect[i], !lastValidItemFn(i, itemsToSelect));
                }
            }
        },

        /**
         * <p>Selects all cards.</p>
         * <p>If the headers parameter is specified, all items that are part of one
         * of the specified headers get selected. Items that are not assigned to one of the
         * specified headers are not changed.</p>
         * @param {Array} [headers] Header filter
         */
        selectAll: function(headers) {
            if (this.getSelectionModeCount() !== SELECTION_MODE_COUNT_MULTI) return;

            var self = this;
            this._headerSel(headers, this.select, function(i, items) {
                for (++i; i < items.length; i++) {
                    if (!self.isSelected(items[i])) {
                        return false;
                    }
                }
                return true;
            });
        },

        /**
         * <p>Deselect all cards.</p>
         * <p>If the headers parameter is specified, all items that are part of one
         * of the specified headers get deselected. Items that are not assigned to one of
         * the specified headers are not changed.</p>
         * @param {Array} [headers] Header filter
         */
        deselectAll: function(headers) {
            var self = this;
            this._headerSel(headers, this.deselect, function(i, items) {
                for (++i; i < items.length; i++) {
                    if (self.isSelected(items[i])) {
                        return false;
                    }
                }
                return true;
            });
        },

        /**
         * @private
         */
        getHeaderSelectionState: function() {
            var model = this.getModel();
            var curHeader = null;
            var state = {
                "selected": [ ],
                "hasUnselected": false,
                "headers": [ ]
            };
            var headerCnt = model.getHeaderCount();
            var itemCnt = model.getItemCount();
            for (var i = 0; i < itemCnt; i++) {
                var item = model.getItemAt(i);
                for (var h = 0; h < headerCnt; h++) {
                    var header = model.getHeaderAt(h);
                    if (header.getItemRef() === item) {
                        curHeader = {
                            "header": header,
                            "selected": [ ],
                            "hasUnselected": false
                        };
                        state.headers.push(curHeader);
                        break;
                    }
                }
                if (this.isSelected(item)) {
                    if (curHeader !== null) {
                        curHeader.selected.push(item);
                    } else {
                        state.selected.push(item);
                    }
                } else {
                    if (curHeader !== null) {
                        curHeader.hasUnselected = true;
                    } else {
                        state.hasUnselected = true;
                    }
                }
            }
            return state;
        },

        /**
         * Create and execute a layout of the cards if in grid view.
         */
        layout: function(settings) {
            if (this.getDisplayMode() !== DISPLAY_GRID) {
                return;
            }
            if (this.$element.data('cuigridlayout')) {
                this.$element.cuigridlayout("destroy");
            }
            this.$element.cuigridlayout(settings);
        },

        /**
         * Exexute a relayout of the cards if in grid view.
         */
        relayout: function() {
            if (this.getDisplayMode() !== DISPLAY_GRID) {
                return;
            }
            this.$element.cuigridlayout("layout");
        },

        /**
         * @protected
         */
        _restore: function(restoreHeaders) {
            this.adapter._restore(restoreHeaders);
        },

        /**
         * <p>Append the specified jQuery items as cards.</p>
         * <p>Note that if you are intending to add multiple cards at once, you should
         * either create a single jQuery object that matches the cards to append or an array
         * of jQuery objects, where each array element represents a single card.</p>
         * @param {jQuery|jQuery[]} $items The jQuery item(s) to append
         */
        append: function($items) {
            this.adapter.getModel().insertItemAt($items, null, false);
        },

        /**
         * Prepend the specified jQuery items as cards.
         * @param {jQuery} $items The jQuery item(s) to prepend
         */
        prepend: function($items) {
            this.adapter.getModel().insertItemAt($items, 0, false);
        },

        /**
         * Remove all cards from the view.
         */
        removeAllItems: function() {
            this.adapter.removeAllItems();
            if (this.getDisplayMode() === DISPLAY_GRID) {
                this.relayout();
            }
            this.$element.trigger($.Event("change:removeAll", {
                widget: this
            }));
        }

    });

    /** Comparator class for column sorting */
    CUI.CardView.DefaultComparator = new Class(/** @lends CUI.CardView.DefaultComparator# */{
        /**
        * This comparator can select any text or attribute of a given jQuery element and compares
        * it with a second item either numerical or alpahebtical
        *
        * @param {String}  selector   A CSS selector that matches the part of the item to be compared
        * @param {String}  attribute  The attribute of the item to be compared. If not given, the inner text of the item will be used for comparison.
        * @param {String}  type  "numeric" for numeric comparison, or "string" for alphabetical comparison
        */
        construct: function (selector, attribute, type) {
            this.selector = selector;
            this.attribute = attribute;
            this.isNumeric = (type == "numeric");
            this.reverseMultiplier = 1;
        },
        /**
        * Changes the order of the sort algorithm
        * @param {boolean} True for reverse sorting, false for normal
        */
        setReverse: function(isReverse) {
            this.reverseMultiplier = (isReverse) ? -1 : 1;
        },
        /**
        * Compares two items according to the configuration
        * @return {integer} -1, 0, 1
        */
        compare: function(item1, item2) {
            var $item1 = item1.getItemEl();
            var $item2 = item2.getItemEl();
            var $e1 = (this.selector) ? $item1.find(this.selector) : $item1;
            var $e2 = (this.selector) ? $item2.find(this.selector) : $item2;
            var t1 = "";
            var t2 = "";
            if (!this.attribute) {
                t1 = $e1.text();
                t2 = $e2.text();    
            } else if(this.attribute.substr(0, 5) == "data-") {
                t1 = $e1.data(this.attribute.substr(5));
                t2 = $e2.data(this.attribute.substr(5));
            } else {
                t1 = $e1.attr(this.attribute);
                t2 = $e2.attr(this.attribute);
            }

            if (this.isNumeric) {
                t1 = t1 * 1;
                t2 = t2 * 1;
                if (isNaN(t1)) t1 = 0;
                if (isNaN(t2)) t2 = 0;
            }

            if (t1 > t2) return 1 * this.reverseMultiplier;
            if (t1 < t2) return -1 * this.reverseMultiplier;
            return 0;            
        },
        /**
        * Return the compare function for use in Array.sort()
        * @return {function} The compare function (bound to this object)
        */
        getCompareFn: function() {
            return this.compare.bind(this);
        }
    });


    /**
     * Display mode: grid view; value: "grid"
     * @type {String}
     */
    CUI.CardView.DISPLAY_GRID = DISPLAY_GRID;

    /**
     * Display mode: list view; value: "list"
     * @type {String}
     */
    CUI.CardView.DISPLAY_LIST = DISPLAY_LIST;

    /**
     * Single selection mode; value: "single"
     * @type {String}
     */
    CUI.CardView.SELECTION_MODE_COUNT_SINGLE = "single";

    /**
     * Multi selection mode; value: "multiple"
     * @type {String}
     */
    CUI.CardView.SELECTION_MODE_COUNT_MULTI = "multiple";

    /**
     * Utility method to get a {@link CUI.CardView} for the specified jQuery element.
     * @param {jQuery} $el The jQuery element to get the widget for
     * @return {CUI.CardView} The widget
     */
    CUI.CardView.get = function($el) {
        var cardView = Utils.getWidget($el);
        if (!cardView) {
            cardView = Utils.getWidget($el.cardView());
        }
        return cardView;
    };

    CUI.util.plugClass(CUI.CardView);

    // Data API
    if (CUI.options.dataAPI) {
        $(function() {
            var cardViews = $('body').find('[data-toggle="cardview"]');
            for (var gl = 0; gl < cardViews.length; gl++) {
                var $cardView = $(cardViews[gl]);
                if (!$cardView.data("cardview")) {
                    $cardView.cardView();
                }
            }
        });
    }

    // additional JSdoc

    /**
     * Triggered when a new card has been inserted succesfully.
     * @name CUI.CardView#change:insertitem
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {*} evt.item The inserted item (data model)
     */

    /**
     * Triggered when the grid selection mode changes.
     * @name CUI.CardView#change:gridSelect
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {Boolean} evt.oldValue True if grid select mode was previously active
     * @param {Boolean} evt.value True if grid select mode is now active
     */

    /**
     * Triggered when the display mode (list/grid view) changes. Display modes are
     * defined by their respective String constants, see for example
     * {@link CUI.CardView.DISPLAY_GRID}.
     * @name CUI.CardView#change:displayMode
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {String} evt.oldValue The old display mode
     * @param {String} evt.value The new display mode
     */

    /**
     * Triggered when the selection changes.
     * @name CUI.CardView#change:selection
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {*} evt.item The card that is (de)selected (data model)
     * @param {Boolean} evt.isSelected True if the item is now selected
     * @param {Boolean} evt.moreSelectionChanges True if there are more selection changes
     *        following (multiple single selection changes can be treated as one big
     *        selection change)
     */

    /**
     * Triggered right before the selection changes if (and only if) the selection is
     * changed using {@link CUI.CardView#toggleSelection}. The selection change can be
     * vetoed by calling cancelSelection on the Event object.
     * @name CUI.CardView#beforeselect
     * @event
     * @param {Object} evt The event
     * @param {*} evt.item The card that is will get (de)selected (data model)
     * @param {Function} evt.changeSelection This function may be called to cancel the
     *        selection; if true is passed as an argument, the originating event (if
     *        applicable; for example if the selection change is triggered by a user
     *        interaction) is cancelled as well (no event propagation; no default browser
     *        behavior)
     */

    /**
     * Triggered after an item has been moved with drag&drop to a new place in the list by the user.
     * @name CUI.CardView#item-moved
     * @event
     * @param {Object} evt          The event
     * @param {Object} evt.oldPrev  The jQuery element that was previous to the item before dragging started, may be empty or a header
     * @param {Object} evt.oldNext  The jQuery element that was next to the item before dragging started, may be empty
     * @param {Object} evt.newPrev  The jQuery element that is now previous to the item, may be empty or a header
     * @param {Object} evt.newNext  The jQuery element that is now next to the item, may be empty
     * @param {boolean} evt.hasMoved  True if the item really moved or false if it has the some position after the drag action as before.
     */

    /**
     * Triggered right before a column sort action on the list is started (when the user clicks on a column). The client side
     * sorting can be vetoed by setting preventDefault() on the event object. The event target is set to the column header the user clicked on.
     * The sortstart event is always triggered, even if the column has no client side sort configuration.
     * @name CUI.CardView#sortstart
     * @event
     * @param {Object} evt The event
     */

    /**
     * Triggered right after a sorting action on the list has been finished (when the user has clicked on a column).
     * The event target is set to the column header the user clicked on. This event is always triggered, even if the column does not have
     * a client side sort configuration.
     * @name CUI.CardView#sortend
     * @event
     * @param {Object} evt The event
     */

    /**
     * Triggered when all cards are removed.
     * @name CUI.CardView#change:removeAll
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     */

}(window.jQuery));
