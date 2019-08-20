/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
(function ($) {
    var dropZoneClassName = "acs-commons-draggablelist-dropzone";

    /* Define some private helpers */
    function boundingBox(element) {
        return {
            l: element.offset().left,
            t: element.offset().top,
            w: element.outerWidth(),
            h: element.outerHeight()
        };
    }

    function currentPagePosition(event) {
        var touch = {};
        if (event.originalEvent) {
            var o = event.originalEvent;
            if (o.changedTouches && o.changedTouches.length > 0) touch = event.originalEvent.changedTouches[0];
            if (o.touches && o.touches.length > 0) touch = event.originalEvent.touches[0];
        }
        var x = touch.pageX || event.pageX;
        var y = touch.pageY || event.pageY;
        return {
            x: x,
            y: y
        };
    }


    CUI.CopyDragAction = new Class({
        extend: CUI.DragAction,
        construct: function (event, source, dragElement, dropZones, restrictAxis) {
            this.clone = dragElement.clone();
            this.clone.hide();
            this.clone.removeClass("is-dragging");
            this.clone.insertBefore(dragElement);
            this.sourceZone = source;
            this.dragElement = dragElement; //temp
        },

        triggerDrop: function (event) {
            var dropElement = this._getCurrentDropZone(event);
            var that = this;
            var isReordering = function (element) {
                var draggable = $(element).find(".draggable");
                var isReorder = draggable[0] == that.sourceZone[0];
                return isReorder;
            };
            if (!dropElement || isReordering(dropElement)) {
                this.clone.remove();
                this.clone = undefined;
            } else {
                this.clone.show();
            }
            this.superClass.triggerDrop.call(this, event);
        }
    });

    /* Global array of drop zones */
    var dropZones = [];

    CUI.DraggableList = new Class(/** @lends CUI.DraggableList# */ {
        toString: 'DraggableList',

        extend: CUI.Widget,
        
        construct: function (options) {
            this.$element.addClass("draggable");

            if (this.$element.data("allow")) {
                var allow = this.$element.data("allow").split(" ");
                if (jQuery.inArray("reorder", allow) >= 0) this.options.allowReorder = true;
                if (jQuery.inArray("drag", allow) >= 0) this.options.allowDrag = true;
                if (jQuery.inArray("drop", allow) >= 0) this.options.allowDrop = true;
            }
            if (this.$element.data("closeable")) this.options.closeable = true;

            this.$element.on("click", ".close", this.close.bind(this));
            this.$element.fipo("taphold", "mousedown", "li", this.dragStart.bind(this));

            this.dropZone = (this.$element.parent().is("." + dropZoneClassName)) ? this.$element.parent() : this.$element;

            this.$element.on("dragend", this.dragEnd.bind(this));

            // Register drop event handlers if dropping is allowed or reordering is allowed
            if (this.options.allowDrop || this.options.allowReorder) {
                this.dropZone.on("dragenter", this.dragEnter.bind(this));
                this.dropZone.on("dragover", this.dragOver.bind(this));
                this.dropZone.on("dragleave", this.dragLeave.bind(this));
                this.dropZone.on("drop", this.drop.bind(this));
            }

            // But out dropZone into the global array ONLY if dropping is allowed!    
            if (this.options.allowDrop) {
                dropZones.push(this.dropZone);
            }

            // Prevent browser from starting his own drag&drop chain
            this.$element.on("dragstart", function (event) {
                event.preventDefault();
            });
        },

        defaults: {
            allowReorder: false,
            allowDrag: false,
            allowDrop: false,
            closeable: false
        },

        dropZone: null,

        dragStart: function (event) {
            if ($(event.target).hasClass("close")) return; // Don't drag on close button!
            if ($(event.target).hasClass("scf-js-item-action")) return; // Don't drag on close button!        
            event.preventDefault();

            var el = $(event.target).closest("li");
            el.prevAll().addClass("drag-before");
            el.nextAll().addClass("drag-after");

            if (this.options.allowDrag) {
                new CUI.CopyDragAction(event, this.$element, el, dropZones);
            } else {
                new CUI.CopyDragAction(event, this.$element, el, [this.dropZone], "vertical");
            }

            el.css({
                position: "absolute",
                display: "block"
            });

            this.sourceEl = el;

        },

        dragEnd: function (event) {
            this.$element.css({
                height: ""
            });
            this.$element.children().removeClass("drag-before drag-after");
            this.sourceEl.css({
                position: "relative"
            });

            this.sourceEl.css({
                top: 0
            });

        },
        dragEnter: function (event) {
            this.dropZone.addClass("drag-over");
            if (this.options.allowReorder) {
                this.reorderPreview(event);
            }
        },
        dragOver: function (event) {
            if (this.options.allowReorder) {
                this.reorderPreview(event);
            }
        },
        dragLeave: function (event) {
            this.dropZone.removeClass("drag-over");
            this.$element.children().removeClass("drag-before drag-after");
        },
        drop: function (event) {
            event.sourceElement.children().css({
                position: "relative",
                top: "0",
                left: "0"
            });


            this.$element.css({
                height: ""
            });
            if (this.$element.is(event.sourceElement) && this.options.allowReorder) {
                this.reorder(event, false);
            }
            if (!this.$element.is(event.sourceElement) && this.options.allowDrop) {
                var e = $(event.item);

                if (this.options.closeable && e.find(".close").length === 0) {
                    e.append("<button class=\"close\">&times;</button>");
                } else if (!this.options.closeable) {
                    e.find(".close").remove();
                }

                if (this.options.allowReorder) {
                    this.reorder(event, e);
                } else {
                    this.$element.append(e);
                }
            }
            this.$element.children().removeClass("drag-before drag-after");


            this.$element.children().css({
                position: "relative",
                display: "block"
            });

        },

        reorderPreview: function (event) {
            var p = currentPagePosition(event);
            var x = p.x;
            var y = p.y;
            var bb = boundingBox(this.$element);
            var that = this;

            if (x < bb.l || y < bb.t || x > bb.l + bb.w || y > bb.t + bb.h) {
                this.$element.children().removeClass("drag-after drag-before");
            } else {
                this.$element.children().each(function () {
                    if ($(this).is(".dragging")) return;
                    var bb = boundingBox($(this));
                    var isAfter = (y < (bb.t + bb.h / 2));
                    $(this).toggleClass("drag-after", isAfter);
                    $(this).toggleClass("drag-before", !isAfter);
                });
            }
        },

        reorder: function (event, newItem) {
            var from = (newItem) ? newItem : $(event.item);
            var before = this.$element.children(".drag-after:first");
            var after = this.$element.children(".drag-before:last");

            var oldPosition = from.index();
            if (before.length > 0) from.insertBefore(before);
            if (after.length > 0) from.insertAfter(after);
            if (before.length === 0 && after.length === 0 && newItem) {
                this.$element.append(from);
            }
            var newPosition = from.index();

            if (oldPosition != newPosition || newItem) {
                var e = jQuery.Event((newItem) ? "inserted" : "reordered");
                e.sourceElement = event.sourceElement;
                e.oldIndex = oldPosition;
                e.newIndex = newPosition;
                e.item = from.get(0);
                this.$element.trigger(e);
                return true;
            }
            return false;
        },
        close: function (event) {
            if (!this.options.closeable) return;
            event.preventDefault();
            var e = $(event.target).closest("li");
            var index = e.index();
            e.remove();
            var ev = jQuery.Event("removed");
            ev.sourceElement = this.$element.get(0);
            ev.index = index;
            ev.item = e.get(0);
            this.$element.trigger(ev);
        }
    });

    CUI.Widget.registry.register("draggable-list", CUI.DraggableList);

    if (CUI.options.dataAPI) {
        $(document).on('foundation-contentloaded', function () {
            CUI.DraggableList.init($("[data-init~=draggablelist]"));
        });
    }
}(window.jQuery));