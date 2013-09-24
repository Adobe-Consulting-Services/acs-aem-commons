(function($) {
    CUI.Tooltip = new Class(/** @lends CUI.Tooltip# */{
        toString: 'Tooltip',

        extend: CUI.Widget,
    /**
      @extends CUI.Widget
      @classdesc <p>A tooltip that can be attached to any other element and may be displayed immediately, on mouse over or only on API call.</p>
      <p>Please always have in mind that there are two elements to deal with: The tooltip HTML element itself, and a "target" element.
      The tooltip is bound to this "target" element. Only on mouseover (or touch) of the "target" element the tooltip is shown (in interactive mode).</p>
      <h3>Quicktip</h3>
      <p>There is also a special "quicktip" mode that gives you a quick way to define tooltips on any element: This element will turn into the target element
      of a newly and dynamically created tooltip (see example below). This "quicktip" even works for dynamically inserted elements.</p>
      <p>
      <button data-init="quicktip" data-quicktip-content="Maybe an error?">Quicktip</button>
      </p>
      @example
      <caption>Instantiate by data API</caption>
&lt;button id="my-button"&gt;My Button&lt;/button&gt;
&lt;span class="tooltip arrow-left" data-interactive="true" data-init="tooltip" data-target="#my-button"&gt;Tooltip content&lt;/span&gt;

Currently there are the following data options:
  data-init="tooltip"         Inits the tooltip widget after page load
  data-interactive            Set to "true" for interactive show/hide on mouseover/touch.
  data-target                 Give an CSS-Selector for defining the target element this tooltips targets at.
  arrow-* CSS classes         One of arrow-left, arrow-right, arrow-top, arrow-bottom to define the position of the tooltip
  type CSS classes            One of success, error, info, notice to define the type of the tooltip

      @example
      <caption>Instantiate by special "quicktip" data option</caption>
      <description>The "quicktip" options are a quick and convenient way of defining tooltips for your elements, even if they are dynamically injected into your page.</description>
&lt;button id="dynamic-button" data-init="quicktip" data-quicktip-content="This is a quicktip" data-quicktip-type="success" data-quicktip-arrow="bottom"&gt;
    Dynamic quicktip creation
&lt;/button&gt;

The quicktip data options are applied to the target element of the tooltip (see above for the behaviour of "target" elements). These are the options:
  data-init="quicktip"          Creates a new tooltip on mouseover/touch (="quicktip").
  data-quicktip-content         Defines a content for the dynamic tooltip. If this is not given, the html() of the element itself is used.
  data-quicktip-arrow           Defines the direction of the arrow of the new tooltip (and therefore the position)
  data-quicktip-type            One of "info", "success", "error", "notice"

      @example
      <caption>Instantiate by Class</caption>
      &lt;button id="dynamic-button"&gt;Dynamic tooltip creation&lt;/button&gt;
      &lt;script type="text/javascript"&gt;
          new CUI.Tooltip({target: "#dynamic-button",
                           content: "Dynamic tooltip",
                           interactive: true});
          // Note: No need for a "element" option here, just define the targeted element
      &lt;/script&gt;
     
      
      @desc Creates a new tooltip     
      @constructs

      @param {Object} options                       Component options
      @param {Mixed} [options.element]              jQuery selector or DOM element to use for tooltip.
      @param {Mixed} options.target                 jQuery selector or DOM element the tooltip is attached to
      @param {String} [options.content]             Content of the tooltip (HTML)
      @param {String} [options.type=info]           Type of dialog to display. One of info, error, notice or success
      @param {String} [options.arrow=left]          Where to place the arrow? One of left, right, top or bottom.
      @param {Integer} [options.delay=500]          Delay before an interactive tooltip is shown.
      @param {Integer} [options.distance=5]         Additional distance of tooltip from element.
      @param {Boolean} [options.visible=true]       True to display immediately, False to defer display until show() called
      @param {Boolean} [options.interactive=false]  True to display tooltip on mouse over, False to only show/hide it when show()/hide() is called manually
      @param {Boolean} [options.autoDestroy=false]  Automatically destroy tooltip on hide?
     */
        construct: function(options) {
            // Ensure we have an object, not only a selector
            if (this.options.target) this.options.target = $(this.options.target);

            if (this.$element.length === 0 && this.options.target) {
                // Special case: We do not have a element yet, but a target
                // -> let us create our own element
                this.$element = $("<div>");
                this.$element.insertAfter(this.options.target);
            }

            // Add tooltip class to give styling
            this.$element.addClass('tooltip');

            if (this.$element.data("interactive")) {
                this.options.interactive = true;
                if (!this.options.target) this.options.target = this.$element.parent();
            }
     
            if (this.$element.data("target")) {
                this.options.target = $(this.$element.data("target"));
            }

            if (!this.options.arrow) {
                this.options.arrow = "left"; // set some default
                if (this.$element.hasClass("arrow-left")) this.options.arrow = "left";
                if (this.$element.hasClass("arrow-right")) this.options.arrow = "right";
                if (this.$element.hasClass("arrow-top")) this.options.arrow = "top";
                if (this.$element.hasClass("arrow-bottom")) this.options.arrow = "bottom";
            }

            // Interactive Tooltips are never visible by default!
            if (this.options.interactive) this.options.visible = false;

            this.$element.toggleClass("hidden", !this.options.visible);

            // Listen to changes to configuration
            this.$element.on('change:content', this._setContent.bind(this));
            this.$element.on('change:type', this._setType.bind(this));
            this.$element.on('change:arrow', this._setArrow.bind(this));

            this.applyOptions();
            this.reposition();

            // Save this object also in the target element
            if (this.options.target) this.options.target.data("tooltip", this);

            if (this.options.interactive && this.options.target) {
                var hto = null;
                // Special behaviour on mobile: show tooltip on every touchstart
                $(this.options.target).finger("touchstart.cui-tooltip",function(event) {
                    if (hto) clearTimeout(hto);
                    this.show();
                    hto = setTimeout(function() {
                        this.hide();
                    }.bind(this), 3000); // Hide after 3 seconds
                }.bind(this));

                var showTimeout = false;
                $(this.options.target).pointer("mouseover.cui-tooltip", function(event) {
                    if (showTimeout) clearTimeout(showTimeout);
                    showTimeout = setTimeout(function() {
                        this.show();
                    }.bind(this), this.options.delay);
                }.bind(this));        

                $(this.options.target).pointer("mouseout.cui-tooltip", function(event) {
                    if (showTimeout) clearTimeout(showTimeout);
                    this.hide();
                }.bind(this));        
            }
          
        },

        defaults: {
          target: null,
          visible: true,
          type: 'default',
          interactive: false,
          arrow: null,
          delay: 500,
          distance: 5
        },

        _types: [
          'info',
          'error',
          'notice',
          'success'
        ],
        
        _arrows: [
          'arrow-left',
          'arrow-right',
          'arrow-top',
          'arrow-bottom'
        ],
        
        applyOptions: function() {
          this._setContent();
          this._setType();
          this._setArrow();
        },

        /** @ignore */
        _setType: function() {
            if (typeof this.options.type !== 'string' || this._types.indexOf(this.options.type) === -1) return;

            // Remove old type
            this.$element.removeClass(this._types.join(' '));

            // Add new type
            this.$element.addClass(this.options.type);

            // Re-positioning
            this.reposition();
        },
        
        /** @ignore */
        _setArrow: function() {
            if (typeof this.options.arrow !== 'string' || this._arrows.indexOf("arrow-" + this.options.arrow) === -1) return;

            // Remove old type
            this.$element.removeClass(this._arrows.join(' '));

            // Add new type
            this.$element.addClass("arrow-" + this.options.arrow);

            // Re-positioning
            this.reposition();
        },    

        /** @ignore */
        _setContent: function() {
            if (typeof this.options.content !== 'string') return;

            this.$element.html(this.options.content);

            // Re-positioning
            this.reposition();
        },

        
        /** @ignore */
        _show: function() {
            if (this.$element.hasClass("hidden")) {
                this.$element.removeClass('hidden');
                this.$element.css("display", "none");
            }
            this.$element.fadeIn();
            //this.reposition();
        },

        /** @ignore */
        _hide: function() {
            this.$element.fadeOut(400, function() {
              if (this.options.autoDestroy) {
                this.$element.remove();
                $(this.options.target).off(".cui-tooltip");
                $(this.options.target).data("tooltip", null);
              }         
            }.bind(this));
            return this;
        },

        /**
          Place tooltip on page

          @returns {CUI.Tooltip} this, chainable
         */
        reposition: function(withoutWorkaround) {
            if (!this.options.target) return;
            
            // Reposition a second time due to rendering errors with Chrome and IE
            if (!withoutWorkaround) setTimeout(function() {this.reposition(true);}.bind(this), 50);
            
            this.$element.detach().insertAfter(this.options.target);

            this.$element.css("position", "absolute");

            var el = $(this.options.target);
            var eWidth = el.outerWidth(true);
            var eHeight = el.outerHeight(true);
            var eLeft = el.position().left;
            var eTop = el.position().top;


            var width = this.$element.outerWidth(true);
            var height = this.$element.outerHeight(true);

            var left = 0;
            var top = 0;

            if (this.options.arrow === "left") {
                left =  eLeft + eWidth + this.options.distance;
                top = eTop + (eHeight - height) / 2;
            }
            if (this.options.arrow === "right") {
                left =  eLeft - width - this.options.distance;
                top = eTop + (eHeight - height) / 2;
            }
            if (this.options.arrow === "bottom") {
                left =  eLeft + (eWidth - width) / 2;
                top = eTop - height - this.options.distance;
            }
            if (this.options.arrow === "top") {
                left =  eLeft + (eWidth - width) / 2;
                top = eTop + eHeight + this.options.distance;
            }                  

            this.$element.css('left', left);
            this.$element.css('top', top);

            return this;
        }
    });

    CUI.util.plugClass(CUI.Tooltip);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            // Only initialize non-interactive tooltips this way!
            $("[data-init~=tooltip]", e.target).tooltip();
        });
        
        $(document).fipo("touchstart", "mouseover", "[data-init~=quicktip]", function(event) {
          var el = $(event.target);
          var tooltip = el.data("tooltip");
          if (!tooltip) {
            new CUI.Tooltip({
              target: el,
              content: el.data("quicktip-content") || el.html(),
              type: el.data("quicktip-type"),
              arrow: el.data("quicktip-arrow"),
              interactive: true,
              autoDestroy: true
            });
            el.trigger(event);
          }
        }.bind(this));  

    }
}(window.jQuery));
