(function($) {
  CUI.DropdownList = new Class(/** @lends CUI.DropdownList# */{
    toString: 'DropdownList',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc A dropdown list widget
      
      <p>
        Dropdown lists are meant to be used by other widgets like Filters and Dropdowns.
        Dropdown lists are invisible by default and can only be made visible by explicitly calling the methods
        "show" (resp. "hide") on the widget.
      </p>
      
      @desc Creates a dropdown list appended to any DOM element
      @constructs
      
      @param {Object}          options                               Component options
      @param {Array}           [options.options]                Array of options in the list
      @param {Function}        [options.optionRenderer=null]                Callback function to render one option
      @param {jQuery|Object}   [options.positioningElement=null]     If this optional element is given, the dropdown list
                                                                     will be placed beyond it instead of the standard element
      @param {String}   [options.cssClass=null]     An optional CSS class string that will be added to the list.

      @param {int}      [options.scrollBuffer]      Distance from bottom of list (px) before scrolled bottom event is fired. Use with infinite loading.
      @param {String}   [options.loadingIndicator]  HTML markup to show when loading in new content.
      @param {String}   [options.noMoreOptions]     Text to show when there are no more options to load.
      @param {String}   [options.position="below"]      Where to position the dropdown list. "above" or "below"
      @param {boolean}  [options.autoPosition=true]      Should the dropdown auto position itself if there's not enough space for the default position in the window?
    */
    construct: function(options) {

        var container = $("<div class=\"dropdown-container\">");
         
        var el = (this.options.positioningElement) ? this.options.positioningElement : this.$element;
        el.after(container);
        el.detach();
        container.append(el);
        this.containerElement = container;

        this.$element.on('change:options change:optionRenderer', function (event) {
            if (event.widget !== this) return; // Only listen to own events
            this.update();
        }.bind(this));
     

        // Listen to events 
        this.$element.on("keydown", "", this._keyPressed.bind(this));

        this.$element.on("blur", "", function() {
           // Did anyone want us to prevent hiding?
           if (this.preventHiding) {
             this.preventHiding = false;
             return;
           }
           this.hide();
        }.bind(this));
       
    },
    
    defaults: {
        positioningElement: null,
        position: "below",
        autoPosition: true,
        optionRenderer: null,
        options: ["Apples", "Pears", "Bananas", "Strawberries"],
        cssClass: null,
        visible: false,
        scrollBuffer: 10,
        loadingIndicator: "<div class='spinner'></div>",
        noWidth: false
    },

    listElement: null,
    containerElement: null,
    currentIndex: -1,
    preventHiding: false,
    
    /**
     * Show this list
     */
    show: function() {
        // Hide old list (if any!)
        this._unrender();
        this.currentIndex = -1;
        this.$element.focus();
        this._render();
    },
    
    /**
     * Hide this list with an optional delay in millis
     * @param int delay Delay before hiding element in milliseconds
     * @return void or timeout object if a delay was given
     */
    hide: function(delay) {
        if (delay > 0) {
            return setTimeout(this._unrender.bind(this), delay);
        } else {
            this._unrender();
        }
        return null;
    },
    
    /**
     * @return boolean true, if this list is currently visible
     */
    isVisible: function() {
        return this.options.visible;
    },
    
    /**
     * Toggle list visibility based on current status
     */
    toggle: function() {
        return this.isVisible() ? this.hide() : this.show();
    },

    /**
     * Updates the rendering of this widget.
     */
    update: function() {
        if (this.listElement) {
            this._unrender();
            this._render();
        }
    },

    /**
    * Append items to the end of the list.
    */
    addItems: function(items) {
        var offset = this.listElement.find('li').not('.loading-indicator').length;
        if(this.listElement) {
            var list = this.listElement.find('ul');
            $.each(items, function(index, value) {
                var el = (this.options.optionRenderer) ? this.options.optionRenderer(index, value) : $("<span>" + value.toString() + "</span>");
                var li = $("<li data-id=\"" + (offset+index) + "\">");
                if (index === this.currentIndex) li.addClass("selected");
                
                // Set class on list items from data-item-class set on options
                var optionClassAttribute = this.options.getOptionClassAttribute(index);
                if (optionClassAttribute) {
                    li.addClass(optionClassAttribute);
                }

                li.append(el);
                list.append(li);
                this.options.options.push(value);
            }.bind(this));
        }
    },
    
    getNumberOfItems: function() {
        var offset = this.listElement.find('li').not('.loading-indicator').length;
        return offset;
    },

    /**
    * Appends a loading indicator to the end of the list. Useful for loading in extra content
    */
    addLoadingIndicator: function() {
        if(this.listElement) {
            this.listElement.find("ul").append($("<li>" + this.options.loadingIndicator + "</li>").addClass("loading-indicator"));
        }
    },

    /**
    * Removes loading indicator from the list.
    */
    removeLoadingIndicator: function() {
      if (this.listElement) {
        this.listElement.find(".loading-indicator").remove();
      }
    },
    
    /** @ignore */    
    _keyPressed: function(event) {        
        var key = event.keyCode;
        
        // Only listen to keys if there is an autocomplete box right now
        if (!this.listElement) {
            return;
        }

        var currentIndex = this.currentIndex;
        
        if (key === 38) { // up
            event.preventDefault();
            if (currentIndex > 0) currentIndex--;
        }
        
        if (key === 40) { // down
            event.preventDefault();
            if (currentIndex < (this.listElement.find("li").length - 1)) currentIndex++;
        }
        
        if (key === 27) { // escape
            event.preventDefault();
            this.hide();
            return;
        }
        
        if (key === 13 || key === 20) { // return or space
           event.preventDefault();
           if (currentIndex >= 0) {
                this._triggerSelect(currentIndex, event);
                return;
           }
        }
        
        this.currentIndex = currentIndex;

        // Set new css classes
        var listItems = this.listElement.find("li");
        listItems.removeClass("selected");
        if (currentIndex >= 0) {
            var el = $(listItems[currentIndex]);
            el.addClass("selected");

            // Scroll to position if necessary
            var t = el.position().top;
            this.listElement.animate({scrollTop: t}, 50);
        }
    },
    /** @ignore */    
    _unrender: function() {       
        if (this.listElement) {
            this.listElement.remove();
            this.listElement = null;  
        }

        this.containerElement.removeClass("dropdown-visible");

        this.options.visible = false;
    },

    /** @ignore */    
    _render: function() {
        var options = this.options.options,
            pos;

        if (options.length === 0) return;
               
        var list = $("<ul></ul>");
        if (this.options.cssClass) list.addClass(this.options.cssClass);

        $.each(options, function(index, value) {
            var el = (this.options.optionRenderer) ? this.options.optionRenderer(index, value) : $("<span>" + value.toString() + "</span>");
            var li = $("<li data-id=\"" + index + "\">");
            if (index === this.currentIndex) li.addClass("selected");

            // Set class on list items from data-item-class set on options
            var optionClassAttribute = this.options.getOptionClassAttribute? this.options.getOptionClassAttribute(index):undefined;
            if (optionClassAttribute) {
                li.addClass(optionClassAttribute);
            }

            li.append(el);
            list.append(li);
        }.bind(this));
        
        list.on("click", "li:not(.loading-indicator)", function(event) {
            event.preventDefault();
            this._triggerSelect($(event.target).closest("li").attr("data-id"), event);
        }.bind(this));

        var el = (this.options.positioningElement) ? this.options.positioningElement : this.$element;

        var container = $("<div class=\"dropdown-list\">");
        container.append(list);

        this.listElement = list = container;

        // Append the list to body and hide it, allows us to calculate its natural height
        list.css({top: 0, left: 0, visibility: "hidden", display: "block"}).appendTo(document.body);

        if (!this.options.noWidth) {
            list.css('width', el.outerWidth(false) + "px");
        }

        pos = this._calcPosition();

        list.css({top: pos.top + "px", left: pos.left + "px", visibility: "visible"});

        this.containerElement.addClass("dropdown-visible");

        list.on("scroll", "", function(event) {
            // Trigger scroll event
            this._listScrolled();
        }.bind(this));

        list.on("mousedown", "", function(event) {
            event.preventDefault();
            event.stopPropagation();
        }.bind(this));

        this.options.visible = true;
    },

      /** @ignore **/
      _calcPosition: function() {
          var el = (this.options.positioningElement) ? this.options.positioningElement : this.$element,
              topb = el.offset().top + el.outerHeight(),
              topa = el.offset().top - this.listElement.outerHeight(),
              pos = {
                  left: el.offset().left,
                  top: topb
              },
              spaceWinBelow = topb + this.listElement.outerHeight() - $(window).scrollTop() < $(window).height(),
              spaceWinAbove = topa > $(window).scrollTop(),
              spaceDocBelow = topb + this.listElement.outerHeight() < $(document).height(),
              spaceDocAbove = topa > 0;

          pos['top'] = (this.options.position === "above") ? topa : topb;

          if(this.options.autoPosition) {
              if (this.options.position === "above" && !spaceWinAbove) {
                  pos['top'] = (spaceWinBelow || (!spaceDocAbove && spaceDocBelow)) ? topb : topa;
              } else if (this.options.position === "below" && !spaceWinBelow) {
                  pos['top'] = (spaceWinAbove || (!spaceDocBelow && spaceDocAbove)) ? topa : topb;
              }
          }

          return pos;
      },

    /** @ignore **/
    _listScrolled: function() {
      if(this._reachedListBottom()) {
          this._triggerScrolledBottom();
      }
    },

    /** @ignore **/
    _reachedListBottom: function() {
      var listWrapper = this.listElement;
      var list = this.listElement.find('ul');
      return (list.height() - listWrapper.height() <= listWrapper.scrollTop() + this.options.scrollBuffer);
    },
    
    /** @ignore */    
    _triggerSelect: function(index, event) {
        // Trigger a change event
        this.$element.focus();
        var e = $.Event('dropdown-list:select', {
          selectedIndex: index,
          selectedValue: this.options.options[index],
          source: this,
          originalEvent: event //passing originalEvent, for finding the exact target if required.
        });
        this.$element.trigger(e);    
    },

    /** @ignore */
    _triggerScrolledBottom: function(index) {
        // Trigger a scrolled bottom event
        var e = $.Event('dropdown-list:scrolled-bottom');
        this.$element.trigger(e);
    }
    
  });

  CUI.util.plugClass(CUI.DropdownList);
}(window.jQuery));
