(function($) {
  var ns = "cui-draggable-list";
  var dropZoneClassName = "dropzone";
  
  /* Define some private helpers */
  function boundingBox(element) {
    return {l: element.offset().left,
            t: element.offset().top,
            w: element.outerWidth(),
            h: element.outerHeight()};
  }
  function within(x, y, element) {
    var bb = boundingBox(element);
    return (x >= bb.l && y >= bb.t && x < bb.l + bb.w && y < bb.t + bb.h);
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
      return {x: x, y: y};   
  }
  
  /**
     Internal helper class to perform the drag action.
  */
  var DragAction = new Class({
    /**
      Construct a new Drag Action. This class is internal for now.
      
      After the initialization the drag ist performed immediatly.
      
      @param {Event} event        The event that triggered the drag
      @param {jQuery} source      The element that is the source of this drag
      @param {jQuery} dragElement The element that will be dragged
      @param {Array} dropZones    An Array of elements that can be destinations for this drag 
    */
    construct: function(event, source, dragElement, dropZones, restrictAxis) {
      this.sourceElement = source;
      this.dragElement = dragElement;
      this.container = this._getViewContainer(dragElement);
      this.containerHeight = this.container.get(0).scrollHeight; // Save current container height before we start dragging
      this.dropZones = dropZones;
      this.axis = restrictAxis;
      this.scrollZone = 20; // Use 20px as scrolling zone, static for now
      this.dragStart(event);
    },
    currentDragOver: null,
    
    _getViewContainer: function(element) {
      // Search for the first parent that has a hidden/scrolling overflow
      while (true) {
        var p = element.parent();
        if (p.length === 0) return p;
        if (p.is("body")) return p;
        var flow = p.css("overflow");
        if (flow == "hidden" || flow == "auto" || flow == "scroll") return p;
        element = p;
      }
    },
    dragStart: function(event) {
      event.preventDefault();
      // Starting the drag
      var p = this.dragElement.position();

      this.dragElement.css({
          "left": p.left,
          "top": p.top,
          "width": this.dragElement.width() + "px"}
      );
      this.dragElement.addClass("dragging"); 
           
      var pp = currentPagePosition(event);
      var x = pp.x;
      var y = pp.y;
              
      this.dragStart = {x: x - p.left, y: y - p.top};
     
      // Bind event listeners
      $(document).fipo("touchmove." + ns, "mousemove." + ns, this.drag.bind(this));
      $(document).fipo("touchend." + ns, "mouseup." + ns, this.dragEnd.bind(this));  
       
      this.sourceElement.trigger(this._createEvent("dragstart", event));
      
      // Perform a first drag
      this.drag(event);
      
    },
    drag: function(event) {
      event.preventDefault();
      
      // Performing the drag
      var p = currentPagePosition(event);
      var x = p.x;
      var y = p.y;

      // Need to scroll?      
      if (this.container.is("body")) {
        if ((y - this.container.scrollTop()) < this.scrollZone) this.container.scrollTop(y - this.scrollZone);
        if ((y - this.container.scrollTop()) > (this.container.height() - this.scrollZone)) this.container.scrollTop(y - (this.container.height() - this.scrollZone));
      } else {
        var oldTop = this.container.scrollTop();
        var t = this.container.offset().top + this.scrollZone;
        if (y < t) {
          this.container.scrollTop(this.container.scrollTop() - (t - y));
        }
        var h = this.container.offset().top + this.container.height() - this.scrollZone;
        if (y > h) {
          var s = this.container.scrollTop() + (y - h);
          if (s > (this.containerHeight - this.container.height())) {
            s = Math.max(this.containerHeight - this.container.height(), 0);
          }
          this.container.scrollTop(s);
        }
        var newTop = this.container.scrollTop();
        this.dragStart.y += oldTop - newTop; // Correct drag start position after element scrolling
      }
      
    
      var newCss = {};
      if (this.axis != "horizontal") newCss["top"] = y - this.dragStart.y;
      if (this.axis != "vertical") newCss["left"] = x - this.dragStart.x;

      this.dragElement.css(newCss);
      
      this.triggerDrag(event);
    },
    dragEnd: function(event) {
      event.preventDefault();
      // Finishing a drag
      this.dragElement.removeClass("dragging");
      this.dragElement.css({top: "", left: "", width: ""});
              
      // Remove event handlers
      $(document).off("." + ns);
      
      // Trigger drop
      this.triggerDrop(event);
      
      // Trigger end events
      if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragleave", event));
      this.sourceElement.trigger(this._createEvent("dragend", event));     
    },
    triggerDrag: function(event) {
      var dropElement = this._getCurrentDropZone(event);
      if (dropElement != this.currentDragOver) {
        if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragleave", event));
        this.currentDragOver = dropElement;
        if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragenter", event));
      } else {
        if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragover", event));
      }      
    },
    triggerDrop: function(event) {
      var dropElement = this._getCurrentDropZone(event);
      if (dropElement == null) return;
      var dropEvent = this._createEvent("drop", event);
      dropElement.trigger(dropEvent);
    },
    _getCurrentDropZone: function(event) {
      var p = currentPagePosition(event);
      var dropElement = null;
      
      jQuery.each(this.dropZones, function(index, value) {
        if (!within(p.x, p.y, value)) return;
        dropElement = value;
      }.bind(this));
      
      return dropElement;     
    },
    _createEvent: function(name, fromEvent) {
      var p = currentPagePosition(fromEvent);
      var event = jQuery.Event(name);
      event.pageX = p.x;
      event.pageY = p.y;
      event.sourceElement = this.sourceElement;
      event.item = this.dragElement;
      return event;
    }
    
  });
  
  /* Global array of drop zones */
  var dropZones = [];
      
  CUI.DraggableList = new Class(/** @lends CUI.DraggableList# */{
    toString: 'DraggableList',

    extend: CUI.Widget,
    
    /**
     Triggered when a the position of an item in the list has been changed due to user sorting

     @name CUI.Sortable#reordered
     @event

     @param {Object} evt                    Event object
     @param {Object} evt.sourceElement      The DOM list element this event occured on
     @param {Object} evt.item               Object representing the moved item
     @param {Object} evt.oldIndex           The old position of the item in the list
     @param {Object} evt.newIndex           The new position of the item in the list
    */
    /**
     Triggered when an item has ben inserted from another list

     @name CUI.Sortable#inserted
     @event

     @param {Object} evt                    Event object
     @param {Object} evt.sourceElement      The DOM list element where the item came from
     @param {Object} evt.item               Object representing the inserted item
     @param {Object} evt.oldIndex           The old position of the item in the former list
     @param {Object} evt.newIndex           The new position of the item in the current list
    */
    /**
     Triggered when an items has been removed from the list by the user

     @name CUI.Sortable#removed
     @event

     @param {Object} evt                    Event object
     @param {Object} evt.item               Object representing the removed item
     @param {Object} evt.index              The former position of the item
    */              
    /**
      @extends CUI.Widget
      @classdesc Makes a list draggable. This also implies re-ordering of the list items on behalf of the user. This widget also
      defines some high-level events for inserting, reordering and removing items. The underlying drag/drop events should not be used outside this widget,
      as they are subject to change in the future.

      <h2 class="line">Examples</h2>

      @example
      <caption>A fully configured list</caption>
      &lt;div class="dropzone"&gt;
        &lt;ul class="draggable" data-init="draggable-list" data-allow="drag drop reorder" data-closeable="true"&gt;
        &lt;li&gt;Item 1&lt;/li&gt;
        &lt;li&gt;Item 2&lt;/li&gt;
        &lt;/ul&gt;
        &lt;p&gt;Drop here!&lt;/p&gt;
      &lt;/div&gt;
          
      
      @desc Creates a draggable/sortable list
      @constructs

      @param {Object} options                          Component options
      @param {Mixed} options.element                   jQuery selector or DOM element to use for sortable list. Has to be an <ul>
      @param {boolean} [options.allowReorder=false]    May the user reorder the list by drag&drop?     
      @param {boolean} [options.allowDrag=false]       May the user drag elements of the list to other lists?
      @param {boolean} [options.allowDrop=false]       May the user drop elements into this list?
      @param {boolean} [options.closable=false]        Can the user remove items from this list?                             
    */
    construct: function(options) {
      this.$element.addClass("draggable");
      
      if (this.$element.data("allow")) {
        var allow = this.$element.data("allow").split(" ");
        if (jQuery.inArray("reorder", allow) >= 0) this.options.allowReorder = true;
        if (jQuery.inArray("drag", allow) >= 0) this.options.allowDrag = true;
        if (jQuery.inArray("drop", allow) >= 0) this.options.allowDrop = true;
      }
      if (this.$element.data("closable")) this.options.closable = true;
                  
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
      this.$element.on("dragstart", function(event) {
        event.preventDefault();
      });
    },

    defaults: {
      allowReorder: false,
      allowDrag: false,
      allowDrop: false,
      closable: false
    },
    
    dropZone: null,
    
    dragStart: function(event) {
      if ($(event.target).hasClass("close")) return; // Don't drag on close button!
      event.preventDefault();

      //this.dragging = $(event.target).closest("li");
      
      var el = $(event.target).closest("li");
      el.prevAll().addClass("drag-before");
      el.nextAll().addClass("drag-after");

      // Fix height of list element to avoid flickering of page
      this.$element.css({height: this.$element.height() + $(event.item).height() + "px"});
      if (this.options.allowDrag) {
        new DragAction(event, this.$element, el, dropZones);
      } else {
        new DragAction(event, this.$element, el, [this.dropZone], "vertical");
      }

    },
    dragEnd: function(event) {
      this.$element.css({height: ""});
    },
    dragEnter: function(event) {
      this.dropZone.addClass("drag-over");
      if (this.options.allowReorder) {
        this.reorderPreview(event);
      }  
    },
    dragOver: function(event) {
      if (this.options.allowReorder) {
        this.reorderPreview(event);
      }
    },
    dragLeave: function(event) {
      this.dropZone.removeClass("drag-over");
      this.$element.children().removeClass("drag-before drag-after");      
    },
    drop: function(event) {
      this.$element.css({height: ""});
      if (this.$element.is(event.sourceElement) && this.options.allowReorder) {
        this.reorder(event, false); 
      }
      if (!this.$element.is(event.sourceElement) && this.options.allowDrop) {
        var e = $(event.item);
        
        if (this.options.closable && e.find(".close").length === 0) {
          e.append("<button class=\"close\">&times;</button>");
        } else if (!this.options.closable) {
          e.find(".close").remove();
        }
        
        if (this.options.allowReorder) {
          this.reorder(event, e);
        } else {
          this.$element.append(e);
        }
      }
      this.$element.children().removeClass("drag-before drag-after");
    },
    
    reorderPreview: function(event) {
      var p = currentPagePosition(event);
      var x = p.x;
      var y = p.y;
      var bb = boundingBox(this.$element);
      var that = this;
            
      if (x < bb.l || y < bb.t || x > bb.l + bb.w || y > bb.t + bb.h) {
        this.$element.children().removeClass("drag-after drag-before");
      } else {
        this.$element.children().each(function() {
          if ($(this).is(".dragging")) return;
          var bb = boundingBox($(this));
          var isAfter = (y < (bb.t + bb.h / 2));
          $(this).toggleClass("drag-after", isAfter);
          $(this).toggleClass("drag-before", !isAfter);        
        });
      }
                  
    },
    
    reorder: function(event, newItem) {
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
    close: function(event) {
      if (!this.options.closable) return;
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

  // jQuery plugin
  CUI.util.plugClass(CUI.DraggableList);

  if (CUI.options.dataAPI) {
      $(document).on('cui-contentloaded.data-api', function() {
        $("[data-init~=draggable-list]").draggableList();
      });
  }
}(window.jQuery));
