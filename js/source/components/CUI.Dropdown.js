(function($) {
  CUI.Dropdown = new Class(/** @lends CUI.Dropdown# */{
    toString: 'Dropdown',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc A dropdown widget
      
        <p>
            <select name="foo" data-init="dropdown" multiple data-placeholder="Please Select items">
                <option>One</option>
                <option>Two</option>
                <option>Three</option>
            </select>
        </p>
        <p>
        Currently this widget does only support creation from an existing &lt;select&gt;&lt;/select&gt; field.
        </p>
    @example
    <caption>Instantiate by data API</caption>
    &lt;select name="foo" data-init="dropdown" multiple data-placeholder="Please Select items"&gt;
        &lt;option&gt;One&lt;/option&gt;
        &lt;option&gt;Two&lt;/option&gt;
        &lt;option&gt;Three&lt;/option&gt;
    &lt;/select&gt;

    @example
    <caption>Instantiate with Class</caption>
    var dropdown = new CUI.Dropdown({
      element: '#myOrdinarySelectBox'
    });

    // Changes the select box into a beautiful widget.

    @example
    <caption>Instantiate by jQuery plugin</caption>
    $("select").dropdown();

    // Changes all select boxes into beautiful widgets.
       
       
      @desc Creates a dropdown from any select element
      @constructs
      
      @param {Object}   options                               Component options
      @param {Array} [options.options=empty array]      Selectable options
      @param {boolean} [options.multiple=false]      Is this a multiselect widget?
      @param {boolean} [options.editable=false]      May the user edit the option text?
      @param {String} [options.placeholder="Select"]      Placeholder string to display in empty widget
      @param {boolean} [options.disabled=false]      Is this widget disabled?
      @param {boolean} [options.hasError=false]      Does this widget contain an error?
      @param {boolean} [options.noWidth=false]      Don't set dropdown list width?
      @param {Function} [options.autocompleteCallback=use options]      Callback for autocompletion: callback(handler, searchFor) with handler is a result callback function with handler(results, searchFor). See example page.
      @param {String}   [options.position="below"]      Where to position the dropdown list. "above" or "below"
      @param {boolean}  [options.autoPosition=true]      Should the dropdown auto position itself if there's not enough space for the default position in the window?
    */
    construct: function(options) {

        if (this.options.autocompleteCallback) {
            // Enable editing for autocompleter
            this.options.editable = true;
        }

        this._render();

        // isMobile should be replace with a CUI.Util method
        // Editable dropdown can't be natively rendered.
        if (this._isMobile() && !this.options.editable) {
            this._initForMobile();
        } else {
            this._initForDesktop();
        }
        
        var $button = this.$element.find('>div>button');
        if ($button.length > 0 && $button.attr('type') === undefined) {
            $button[0].setAttribute('type', 'button');
        }

    },
    
    defaults: {
        options: [],
        multiple: false,
        placeholder: "Select",
        disabled: false,
        editable: false,
        hasError: false,
        noWidth: false,
        position: "below",
        autoPosition: true
    },
    
    dropdownList: null,
    autocompleteList: null,
    syncSelectElement: null,
    buttonElement: null,
    positioningElement: null,
    inputElement: null,
    hasFocus: false,

    _initForMobile: function() {
        this.$element.addClass('mobile');

        this.buttonElement.on("click", function() {
            this._openSelectInput();
        }.bind(this));

        this.$element.find('select').on("change", function() {
            this._update(true);
        }.bind(this));

        // place the hidden select input at the right position for ipad and iphone
        this._placeSelect();
    },

    _initForDesktop: function() {
        this.dropdownList = new CUI.DropdownList({
            element: this.buttonElement,
            positioningElement: this.positioningElement,
            position: this.options.position,
            autoPosition: this.options.autoPosition,
            options: this.options.options,
            optionRenderer: this._optionRenderer.bind(this),
            getOptionClassAttribute: this._getOptionClassAttribute.bind(this),
            noWidth: this.options.noWidth
        });

        if (this.options.editable) {
            this.autocompleteList = new CUI.DropdownList({
                element: this.inputElement,
                positioningElement: this.positioningElement,
                position: this.options.position,
                autoPosition: this.options.autoPosition,
                options: this.options.options,
                optionRenderer: this._optionRendererAutocomplete.bind(this),
                getOptionClassAttribute: this._getOptionClassAttribute.bind(this),
                noWidth: this.options.noWidth,
                cssClass: "autocomplete-results"
            });
        }
        
        this.buttonElement.on("dropdown-list:select", this._processSelect.bind(this));
        
        this.buttonElement.on("mousedown", "", function(event) {
            event.preventDefault();
            if (this.autocompleteList !== null) {
                this._adjustAutocompleter();
            } else {
                this.dropdownList.toggle();
            }
        }.bind(this));
        this.buttonElement.on("mouseup", "", function(event) {
            event.preventDefault();
        }.bind(this));

        // Auto completion
        this.inputElement.on("click", "", function() {
           if (this.autocompleteList !== null) this._adjustAutocompleter();
        }.bind(this));
        this.inputElement.on("input", "", function() {
           if (this.autocompleteList !== null) this._adjustAutocompleter({toggleList : false});
        }.bind(this));
        this.inputElement.on("dropdown-list:select", "", function(event) {
            //this.inputElement.val(event.selectedValue);
            //this.autocompleteList.hide();
            this._processSelect(event);
        }.bind(this));
        
        // Correct focus
        this.$element.children().on("focus", "", function() {
            this.hasFocus = true;
            this._update();
        }.bind(this));
        this.$element.children().on("blur", "", function() {
            this.hasFocus = false;
            this._update();
        }.bind(this));

        this.$element.find('select').on("change", function() {
            this._update(true);
        }.bind(this));
    },

    _placeSelect: function() {
        var $select = this.$element.find('select').first();

        $select.css({
            position: 'absolute',
            left: 0,
            top: 0,
            right: 0,
            bottom: 0,
            width: 'auto',
            height: 'auto'
        });
    },

    _openSelectInput: function() {
        var selectElement = this.$element.find('select')[0];

        if (document.createEvent) {
            var e = document.createEvent("MouseEvents");
            e.initMouseEvent("mousedown", true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
            selectElement.dispatchEvent(e);
        } else if (selectElement.fireEvent) {
            selectElement.fireEvent("onmousedown");
        }
    },

    /** @ignore */
    _adjustAutocompleter: function(args) {
        var searchFor = this.inputElement.val();
        var defaults = { toggleList : true };
        var options = $.extend(defaults, args);

        var showResults = function(result, searchFor) {
            this.autocompleteList.set({
               options: result
            });
            
            if (options.toggleList) {
                this.autocompleteList.toggle();
            }
            else {
                this.autocompleteList.show();
            }
        }.bind(this);
        
        if (this.options.autocompleteCallback) {
            this.options.autocompleteCallback(showResults, searchFor);
        } else {
            var result = [];
            $.each(this.options.options, function(index, value) {
                 if (value.toLowerCase().indexOf(searchFor.toLowerCase(), 0) >= 0 ) result.push(value);
            });
            showResults(result, searchFor);
        }
       
    },

    /** @ignore */
    _optionRenderer: function(index, option) {
        var el = $("<span>" + option + "</span>");
        if (this.options.multiple) {
            var checkbox = $("<div class=\"checkbox\">");
            if (this.syncSelectElement && $(this.syncSelectElement.find("option").get(index)).attr("selected")) {
                checkbox.addClass("checked");
            }
            el.prepend(checkbox);
        }
        return el;
    },

    /** @ignore */
    _optionRendererAutocomplete: function(index, value) {
        var searchFor = this.inputElement.val();
        var i = value.toLowerCase().indexOf(searchFor.toLowerCase());
        if (i >= 0) {
            value = value.substr(0, i) + "<em>" + value.substr(i, searchFor.length) + "</em>" + value.substr(i + searchFor.length);
        }
        
        return $("<span>" + value + "</span>");
    },

    /** @ignore */
    _getOptionClassAttribute: function(index) {
        return (this.syncSelectElement) ? $(this.syncSelectElement.find("option").get(index)).data("itemclass") : "";
    },
    
    /** @ignore */
    _processSelect: function(event) {
        if (this.syncSelectElement) {

            var value = event.selectedValue;
            var current = null;

            // Synchronize select element
            if (event.source === this.autocompleteList) {
                this.syncSelectElement.find("option").each(function() {
                    if ($(this).attr("value") === value) current = $(this);
                });
                if (current === null) {
                    current = $("<option>");
                    current.attr("value", value).text(value);
                    this.syncSelectElement.append(current);
                }
                this.autocompleteList.hide();
            } else {
                current = $(this.syncSelectElement.find("option").get(event.selectedIndex));
                value = current.attr("value");
            }
            
            if (this.options.multiple) {
                var v = this.syncSelectElement.val();
                if (v === null) v = [];
                if (v.indexOf(value) >= 0) {
                    v.splice(v.indexOf(value), 1);
                } else {
                    v.push(value);
                }
                this.syncSelectElement.val(v);
                this.dropdownList.update();
            } else {
                this.syncSelectElement.val(value);
                this.dropdownList.hide();
            }
            this.syncSelectElement.change();
        }

        this._update(true);
    },
    
    /** @ignore */
    _render: function() {
        this._readDataFromMarkup();
        
        if (this.$element.get(0).tagName !== "DIV") {
            var div = $("<div></div>");
            this.$element.after(div);
            this.$element.detach();
            div.append(this.$element);
            this.$element = div;
        }

        this._createMissingElements();
        this.buttonElement = this.$element.find("button");
        this.syncSelectElement = this.$element.find("select");
        this.inputElement = this.$element.find("input");
        this.positioningElement = (this.options.editable) ? this.$element : this.buttonElement;
        
        // Build the button text placeholder. Used for managing text overflow
        if(!this.options.editable) {
            this.buttonText = $(this.buttonElement).wrapInner("<span />").find("span");
        }

        if (!this.inputElement.attr("name")) this.inputElement.attr("name", this.syncSelectElement.attr("name") + ".edit");
        if (this.syncSelectElement.attr("multiple")) this.options.multiple = true;
        
        this.$element.addClass("dropdown");
        if (this.options.editable) this.$element.addClass("dropdown-editable");
        

        if (this.$element.find("select option").length > 0 && this.options.options.length === 0) {
            this.options.options = [];
            this.$element.find("select option").each(function(i, e) {
                this.options.options.push($(e).html());
            }.bind(this));
        }
        
        // Set several options
        if (this.options.multiple) {
            this.syncSelectElement.attr("multiple", "multiple");
        } else {
            this.syncSelectElement.removeAttr("multiple", "multiple");
        }
        if (this.options.placeholder) {
            if(!this.options.editable) {
                this.buttonText.text(this.options.placeholder);
            } else {
                this.buttonElement.text(this.options.placeholder);
            }
            this.inputElement.attr("placeholder", this.options.placeholder);
        }

        this._update(true);
    },
    
    /** @ignore */
    _readDataFromMarkup: function() {
        if (this.$element.attr("disabled")) this.options.disabled = true;
        if (this.$element.attr("data-disabled")) this.options.disabled = true;
        if (this.$element.attr("multiple")) this.options.multiple = true;
        if (this.$element.attr("data-multiple")) this.options.multiple = true;
        if (this.$element.attr("placeholder")) this.options.placeholder = this.$element.attr("placeholder");
        if (this.$element.attr("data-placeholder")) this.options.placeholder = this.$element.attr("data-placeholder");
        if (this.$element.attr("data-editable")) this.options.editable = true;
        if (this.$element.attr("data-error")) this.options.hasError = true;
        if (this.$element.hasClass("error")) this.options.hasError = true;
        if (this.$element.attr("data-nowidth")) this.options.noWidth = true;
    },
    
    /** @ignore */
    _createMissingElements: function() {
        if (this.$element.find("button").length === 0) {
            var button = $("<button/>", {
                text: this.options.placeholder,
                type: 'button'
            });
            button.addClass("dropdown");
            this.$element.append(button);
        }
        if (this.options.editable && this.$element.find("input").length === 0) {
            var input = $("<input type=\"text\">");
            this.$element.prepend(input);
        }
        if (this.$element.find("select").length === 0) {
            var select = $("<select>");
            this.$element.append(select);
        }
    },
    
    /** @ignore */
    _update: function(updateContent) {
        if (updateContent) {
            if (this.syncSelectElement && !this.options.multiple) {
                var option = this.syncSelectElement.find("option:selected");
                if (option) {
                    var selectedIndex = option.index();
                    var text = option.text();
                    var html = option.html();
                    if (this.inputElement.length > 0) {
                        this.inputElement.val(text).trigger('change');
                    } else {
                        this.buttonText.html(html);
                    }
                }
            }
        }
        if (this.options.disabled) {
            this.buttonElement.attr("disabled", "disabled");
            this.inputElement.attr("disabled", "disabled");
        } else {
            this.buttonElement.removeAttr("disabled");
            this.inputElement.removeAttr("disabled");
        }
        if (this.hasFocus) {
            this.$element.addClass("focus");
        } else {
            this.$element.removeClass("focus");
        }
        if (this.options.hasError) {
            this.$element.addClass("error");
        } else {
            this.$element.removeClass("error");
        }
    },
    
    /** @ignore */
    _isMobile: function() {
        return typeof window.ontouchstart === 'object';
    }
    
  });

  CUI.util.plugClass(CUI.Dropdown);
  
  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
        $("[data-init~=dropdown]", e.target).dropdown();
    });
  }
}(window.jQuery));
