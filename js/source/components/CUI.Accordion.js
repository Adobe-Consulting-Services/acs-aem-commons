(function($) {
  CUI.Accordion = new Class(/** @lends CUI.Accordion# */{
    toString: 'Accordion',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc A widget for both accordions and collapsibles
      
            <ul data-init="accordion">
              <li>
                <h3>
                  <em>Tab 1</em> of the accordion
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
              </li>
              <li class="active">
                <h3>
                  <em>Tab 2</em> of the accordion
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
              </li>
              <li>
                <h3>
                  <em>Tab 3</em> of the accordion
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
              </li>                             
            </ul>
            
            <div data-init="collapsible">
                <h3>
                  <em>Headline</em> of the collapsible
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
            </div>
                  
      @example
      <caption>Instantiate Accordion with data API</caption>
      <description>This is the most convenient way to instantiate an accordion, as an accordion in any case needs some markup to be initialized. Give the "active" class to the element that should be opened by default.</description>
&lt;ul data-init="accordion"&gt;
  &lt;li&gt;
    &lt;h3&gt;
      &lt;em&gt;Tab 1&lt;/em&gt; of the accordion
    &lt;/h3&gt;
    ...
  &lt;/li&gt;
  &lt;li class="active"&gt;
    &lt;h3&gt;
      &lt;em&gt;Tab 2&lt;/em&gt; of the accordion
    &lt;/h3&gt;
    ...
  &lt;/li&gt;
&lt;/ul&gt;

      @example
      <caption>Instantiate Collapsible with data API</caption>
      <description>Add the "active" class to open the collapsible by default.</description>
&lt;div data-init="collapsible"&gt;
  &lt;h3&gt;
    &lt;em&gt;Headline&lt;/em&gt; of the collapsible
  &lt;/h3&gt;
  ...
&lt;/div&gt;

      @example
      <caption>Instantiate Accordion with jQuery plugin</caption>
      <description>The widget will try to guess if it is a collapsible instead of accordion by testing if the CSS class "collapsible" is present.</description>
  $("#accordion").accordion({active: 2}); // Set active tab
  $("#collapsible").accordion({active: true}); // Set collapsible to "opened" (=active)
          
      @desc Creates a new accordion or collapsible
      @constructs

      @param {Object} options                               Widget options.
      @param {Mixed}  [options.active=false]                Index of the initial active tab of the accordion or one of true/false for collapsibles
            
    */
    construct: function(options) {
      this.isAccordion = (!this.$element.hasClass("collapsible")) && (this.$element.data("init") !== "collapsible");
    
      if (this.isAccordion) this.$element.addClass("accordion");
    
      if (this.isAccordion) {
        var activeIndex = this.$element.children(".active").index();
        if (this.options.active !== false) activeIndex = this.options.active;
        this.$element.children().each(function(index, element) {
          this._initElement(element, index != activeIndex);
        }.bind(this));
      } else {
        this._initElement(this.$element, !(this.options.active || this.$element.hasClass("active")));
      }
      
      this.$element.on("click", "h3", this._toggle.bind(this));
      
      this.$element.on("change:active", this._changeActive.bind(this));
      
      // Prevent text selection on header
      this.$element.on("selectstart", "h3", function(event) {
        event.preventDefault();
      });
      
      this._makeAccessible();
    },
    
    defaults: {
      active: false
    },
    
    isAccordion: false,
    
    _toggle: function(event) {
      var el = $(event.target).closest(".collapsible"),
          isCurrentlyActive = el.hasClass("active"),
          active = (isCurrentlyActive) ? false : ((this.isAccordion) ? el.index() : true); 
      this.setActive(active);
    },
    _changeActive: function() {
      if (this.isAccordion) {
        this._collapse(this.$element.children(".active"));
        if (this.options.active !== false) {
          var activeElement = this.$element.children().eq(this.options.active);
          this._expand(activeElement);
        }
      } else {
        if (this.options.active) {
          this._expand(this.$element);
        } else {
          this._collapse(this.$element);
        }
      }      
    },
    setActive: function(active) {
      this.options.active = active;
      this._changeActive();    
    },
    _initElement: function(element, collapse) {
        // Add correct header
        if ($(element).find("h3").length === 0) $(element).prepend("<h3>&nbsp;</h3>");
        if ($(element).find("h3 i").length === 0) $(element).find("h3").prepend("<i></i>&nbsp;");
        
        $(element).addClass("collapsible");
        
        var head = $(element).find("h3"),
            fold = $("<div></div>").toggle(!collapse),
            icon = head.find("i");
        
        // wrap the collapsible content in a div so that we can identify 
        // the relationship between the heading and the content it controls
        $(element).wrapInner(fold);
        
        // move the heading before the collapsible content
        head.prependTo(element);
        
        // Set correct initial state
        if (collapse) {
          $(element).removeClass("active");
          $(element).height(head.height());
          icon.removeClass("icon-accordiondown").addClass("icon-accordionup");
        } else {
          $(element).addClass("active");
          $(element).css("height", "auto");
          icon.removeClass("icon-accordionup").addClass("icon-accordiondown");
        }    
    },
    _collapse: function(el) {
         el.find("h3 i").removeClass("icon-accordiondown").addClass("icon-accordionup");
         el.animate({height: el.find("h3").height()}, "fast", function() {
            el.removeClass("active"); // remove the active class after animation so that background color doesn't change during animation
            el.find("div[aria-expanded]").hide(); // After animation we want to hide the collapsed content so that it cannot be focused
         });
         
         // update WAI-ARIA accessibility properties
         var head = el.find("h3"),
             fold = head.next("div[aria-expanded]");
         if (this.isAccordion) {
             head.attr({
                 "tabindex": head.is(document.activeElement) ? 0 : -1,
                 "aria-selected": false 
             });
         } else {
            head.attr({
                 "aria-expanded": false 
             });
         }
         fold.attr({
             "aria-hidden": true,
             "aria-expanded": false 
         });
    },
    _expand: function(el) {
         el.addClass("active");
         el.find("h3 i").removeClass("icon-accordionup").addClass("icon-accordiondown");
         var h = this._calcHeight(el);
            
         el.animate({height: h}, "fast", function() {
           el.css("height", "auto"); // After animation we want the element to adjust its height automatically
         });
         
         // update WAI-ARIA accessibility properties
         var head = el.find("h3"),
             fold = head.next("div[aria-expanded]");
         if (this.isAccordion) {
             head.attr({
                 "tabindex": 0,
                 "aria-selected": true 
             });
         } else {
            head.attr({
                 "aria-expanded": true 
             });
         }
         fold.attr({
             "aria-hidden": false,
             "aria-expanded": true 
         }).show();
    },
    /** @ignore */
    _calcHeight: function(el) {
      // Dimension calculation of invisible elements is not trivial.
      // "Best practice": Clone it, put it somwhere on the page, but not in the viewport,
      // and make it visible. 
      var el2 = $(el).clone(),
          fold2 = el2.find('div[aria-expanded]');
      fold2.show();
      el2.css({display: "block",
               position: "absolute",
               top: "-10000px",
               width: el.width(), // Ensure we calculate with the same width as before
               height: "auto"});
      $("body").append(el2);
      var h = el2.height();
      el2.remove();
      return h;
    },
    /**
     * adds accessibility attributes and features
     * per the WAI-ARIA Accordion widget design pattern: 
     * http://www.w3.org/WAI/PF/aria-practices/#accordion 
     * @private
     */
    _makeAccessible: function() {
        var idPrefix = 'accordion-' + new Date().getTime() + '-',
            section, head, fold, isActive, panelId;
        if (this.isAccordion) {
            
            this.$element.attr({
                "role": "tablist" // accordion container has the role="tablist"
            });
            
            this.$element.children(".collapsible").each(function (i, e) {
                var section =  $(e),
                    head = section.find("h3:first"),
                    isActive = section.hasClass("active"),
                    panelId = idPrefix + 'panel-' + i,
                    fold = head.next("div");
                
                section.attr({
                    "role": "presentation" // collapsible containers have the role="presentation" 
                });
                
                head.attr({
                    "role": "tab", // accordion headers should have the role="tab"
                    "id": head.attr("id") || idPrefix + "tab-" + i, // each tab needs an id
                    "aria-controls": panelId, // the id for the content wrapper this header controls
                    "aria-selected": isActive, // an indication of the current state
                    "tabindex": (isActive ? 0 : -1)
                });

                fold.attr({
                    "role": "tabpanel", // the content wrapper should have the role="tabpanel"
                    "id": panelId, // each content wrapper needs a unique id
                    "aria-labelledby": head.attr("id"), // the content wrapper is labelled by its header
                    "aria-expanded": isActive, // an indication of the current state
                    "aria-hidden": !isActive // hide/show content to assistive technology
                });
            });
            
        } else {
            idPrefix = 'collapsible-' + new Date().getTime() + '-';
            section =  this.$element;
            head = section.find("h3:first");
            isActive = section.hasClass("active");
            panelId = idPrefix + 'panel';
            fold = head.next("div");
                
            head.attr({
                "role": "button", // the header should have the role="button"
                "id": head.attr("id") || idPrefix + "heading", // each header needs an id
                "aria-controls": panelId, // the id for the content wrapper this header controls
                "aria-expanded": isActive, // an indication of the current state
                "tabindex": 0
            });

            fold.attr({
                "id": panelId, // each content wrapper needs a unique id
                "aria-labelledby": head.attr("id"), // the content wrapper is labelled by its header
                "aria-expanded": isActive, // an indication of the current state
                "aria-hidden": !isActive // hide/show content to assistive technology
            });
        }
        
        // handle keydown events from focusable descendants
        this.$element.on('keydown', ':focusable', this._onKeyDown.bind(this));
        
        // handle focusin/focusout events from focusable descendants
        this.$element.on('focusin.accordion', ':focusable', this._onFocusIn.bind(this));
        this.$element.on('focusout.accordion', 'h3:focusable', this._onFocusOut.bind(this));
        
        this.$element.on('touchstart.accordion, mousedown.accordion', 'h3:focusable', this._onMouseDown.bind(this));
    },
    /**
     * keydown event handler, which defines the keyboard behavior of the accordion control
     * per the WAI-ARIA Accordion widget design pattern: 
     * http://www.w3.org/WAI/PF/aria-practices/#accordion
     * @private
     */   
    _onKeyDown: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head),
            keymatch = true;
                    
        switch (event.which) {
            case 13: //enter
            case 32: //space
                if (isHead) {
                    head.trigger('click');
                } else {
                    keymatch = false;
                }
                break;
            case 33: //page up
            case 37: //left arrow
            case 38: //up arrow
                if ((isHead && this.isAccordion) || (event.which === 33 && (event.metaKey || event.ctrlKey))) {
                    // If the event.target is an accordion heading, or the key command is CTRL + PAGE_UP,
                    // focus the previous accordion heading, or if none exists, focus the last accordion heading.
                    if (el.prev().find("h3:first").focus().length === 0) {
                        this.$element.find(".collapsible:last h3:first").focus();
                    } 
                } else if (!isHead && (event.metaKey || event.ctrlKey)) {
                    // If the event.target is not a collapsible heading, 
                    // and the key command is CTRL + UP or CTRL + LEFT, focus the collapsible heading.
                    head.focus();
                } else {
                   keymatch = false; 
                }
                break;
            case 34: //page down
            case 39: //right arrow 
            case 40: //down arrow
                if (isHead && this.isAccordion) {
                    // If the event.target is an accordion heading,
                    // focus the next accordion heading, or if none exists, focus the first accordion heading.
                    if (el.next().find("h3:first").focus().length === 0) {
                        this.$element.find(".collapsible:first h3:first").focus();
                    } 
                } else if (!isHead && event.which === 34 && (event.metaKey || event.ctrlKey)) {
                    // If the event.target is not a collapsible heading, 
                    // and the key command is CTRL + PAGE_DOWN, focus the collapsible heading.
                    head.focus();
                } else {
                   keymatch = false; 
                }
                break;
            case 36: //home
                if (isHead && this.isAccordion) {
                    this.$element.find(".collapsible:first h3:first").focus();
                } else {
                   keymatch = false; 
                }
                break;
            case 35: //end
                if (isHead && this.isAccordion) {
                    this.$element.find(".collapsible:last h3:first").focus();
                } else {
                   keymatch = false; 
                }
                break;
            default:
                keymatch = false;
                break;
        }
        
        if (keymatch === true) {
            event.preventDefault();
        }
    },
    /**
     * focusin event handler, used to update tabindex properties on accordion headers
     * and to display focus style on headers.
     * @private
     */
    _onFocusIn: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head);            
        if (isHead) {
            if (this.isAccordion) {
                this.$element.find(".collapsible h3[role=tab]").attr('tabindex', -1);
            }
            if (!head.data('collapsible-mousedown')) {
                el.addClass('focus');
            } else {
                head.removeData('collapsible-mousedown');
            }
        }
        head.attr('tabindex', 0);
    },
    /**
     * focusout event handler, used to clear the focus style on headers.
     * @private
     */
    _onFocusOut: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head);            
        if (isHead) {
            el.removeClass('focus').removeData('collapsible-mousedown');
        }
    },
    /**
     * mousedown event handler, used flag 
     * @private
     */
    _onMouseDown: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head);            
        if (isHead) {
            head.data('collapsible-mousedown', true);
        }
    }
  });

  CUI.util.plugClass(CUI.Accordion);
  
  // Data API
  $(document).on("cui-contentloaded.data-api", function(e) {
    $("[data-init~=accordion],[data-init~=collapsible]").accordion();
  });
}(window.jQuery));


