(function($) {
    CUI.PathBrowser = new Class(/** @lends CUI.PathBrowser# */{
        toString: 'PathBrowser',
        extend: CUI.Widget,

        /**
         @extends CUI.Widget
         @classdesc An autocompletable path browser widget

         <p>
            <select data-init="pathbrowser" data-placeholder="Select path">
                <option>/apps</option>
                <option>/content</option>
                <option>/etc</option>
                <option>/libs</option>
                <option>/tmp</option>
                <option>/var</option>
            </select>
         </p>

         @desc Creates a path browser field
         @constructs

         @param {Object}   options                                    Component options
         @param {Array}    [options.options=empty array]              Array of available options (will be read from &lt;select&gt; by default)
         @param {Array}    [options.optionDisplayStrings=empty array] Array of alternate strings for display (will be read from &lt;select&gt; by default)
         @param {Function} [options.optionLoader=use options]         (Optional) Callback to reload options list. Can be synch or asynch. In case of asynch handling, use second parameter as callback function: optionLoader(string currentPath, function callback) with callback(array resultArray)
         @param {String}   [options.optionLoaderRoot=use options]     (Optional) Nested key to use as root to retrieve options from the option loader result
         @param {Function} [options.optionValueReader=use options]    (Optional) Custom function to call to retrieve the value from the option loader result
         @param {Function} [options.optionTitleReader=use options]    (Optional) Custom function to call to retrieve the title from the option loader result
         @param {boolean}  [options.showTitles=true]                  Should option titles be shown?
         @param {String}   [options.rootPath='/content']              The root path where completion and browsing starts.
                                                                      Use the empty string for the repository root (defaults to '/content').
         @param {String}   [options.placeholder=null]                 Define a placeholder for the input field
         @param {int}      [options.delay=200]                        Delay before starting autocomplete when typing
         @param {int}      [options.disabled=false]                   Is this component disabled?
         @param {String}   [options.name=null]                        (Optional) name for an underlying form field.
         @param {Function} [options.autocompleteCallback=use options] Callback for autocompletion
         @param {Function} [options.optionRenderer=default renderer]  (Optional) Renderer for the autocompleter and the tag badges
         @param {String}   [options.position="below"]                 Where to position the dropdown list. "above" or "below"
         @param {boolean}  [options.autoPosition=true]                Should the dropdown auto position itself if there's not enough space for the default position in the window?
         */
        construct: function(options) {
            // Set callback to default if there is none
            if (!this.options.autocompleteCallback) {
                this.options.autocompleteCallback = this._defaultAutocompleteCallback.bind(this);
            }
            if (!this.options.optionRenderer) {
                this.options.optionRenderer = CUI.PathBrowser.defaultOptionRenderer;
            }

            // Adjust DOM to our needs
            this._render();

            // Populate alternative display strings if necessary
            while (this.options.optionDisplayStrings.length < this.options.options.length) {
                this.options.optionDisplayStrings.push(this.options.options[this.options.optionDisplayStrings.length]);
            }

            // Generate Dropdown List widget
            this.dropdownList = new CUI.DropdownList({
                element: this.inputElement,
                positioningElement: this.inputElement,
                position: this.options.position,
                autoPosition: this.options.autoPosition,
                cssClass: "autocomplete-results"
            });

            // Listen to property changes
            this.$element.on('change:disabled', this._update.bind(this));
            this.$element.on('change:placeholder', this._update.bind(this));
            this.$element.on('change:options', this._changeOptions.bind(this));

            // Listen to events
            this.$element.on("input", "input", function() {
                if (this.options.disabled) {
                    return;
                }
                if (this.typeTimeout) {
                    clearTimeout(this.typeTimeout);
                }
                this.typeTimeout = setTimeout(this._inputChanged.bind(this), this.options.delay);
            }.bind(this));

            this.$element.on("blur", "input", function() {
                if (this.options.disabled) {
                    return;
                }
                if (this.typeTimeout) {
                    clearTimeout(this.typeTimeout);
                }
                this.typeTimeout = null;
                // Set to existing selection for single term use
                if (this.selectedIndex >= 0) {
                    if (this.inputElement.attr("value") === "") {
                        this.setSelectedIndex(-1);
                    } else {
                        this._update();
                    }
                }
            }.bind(this));

            this.$element.on("keydown", "input", this._keyPressed.bind(this));
            this.$element.on("keyup", "input", this._keyUp.bind(this));

            this.dropdownList.on("dropdown-list:select", "", function(event) {
                this.dropdownList.hide(200);
                this.setSelectedIndex(event.selectedValue * 1);
            }.bind(this));

            // focus setting confuses mobile safari
            if (!CUI.util.isTouch) {
                this.$element.on("focus", "input", function(event) {
                    if (this.options.disabled) {
                        return;
                    }
                    this.$element.addClass("focus");
                }.bind(this));

                this.$element.on("blur", "input", function() {
                    if (this.options.disabled) {
                        return;
                    }
                    this.$element.removeClass("focus");
                }.bind(this));

                this.$element.on("click touchend", "input", function(event) {
                    if (this.options.disabled) {
                        return;
                    }
                    
                    this.inputElement.focus();
                    this._inputChanged();
                }.bind(this));    
            }
        },

        defaults: {
            autocompleteCallback: null,
            options: [],
            optionDisplayStrings: [],
            optionLoader: null,
            optionLoaderRoot: null,
            optionValueReader: null,
            optionTitleReader: null,
            showTitles: true,
            rootPath: "/content",
            delay: 200,
            placeholder: null,
            optionRenderer: null,
            position: "below",
            autoPosition: true
        },

        dropdownList: null, // Reference to instance of CUI.DropdownList
        syncSelectElement: null,
        inputElement: null,
        typeTimeout: null,
        selectedIndex: -1,
        triggeredBackspace: false,

        /**
         * @param {int} index Sets the currently selected option by its index.
         *                    -1 removes any selected index.
         */
        setSelectedIndex: function(index) {
            if (index < -1 || index >= this.options.options.length) {
                return;
            }
            this.selectedIndex = index;
            this._update();
        },

        /**
         * @return {int} The currently selected options by index or -1 if none is selected
         */
        getSelectedIndex: function() {
            return this.selectedIndex;
        },

        /** @ignore */
        _changeOptions: function(event) {
            if (event.widget !== this) {
                return;
            }
            this.selectedIndex = -1;
            this._update();
        },

        /** @ignore */
        _render: function() {
            this._readDataFromMarkup();

            var div;
            // if current element is select field -> turn into input field, but hold reference to select to update it on change
            if (this.$element.get(0).tagName === "SELECT") {
                div = $("<div></div>");
                this.$element.after(div);
                this.$element.detach();
                div.append(this.$element);
                this.$element = div;
            }

            // if current element is input field -> wrap it into DIV
            if (this.$element.get(0).tagName === "INPUT") {
                div = $("<div></div>");
                this.$element.after(div);
                this.$element.detach();
                div.prepend(this.$element);
                this.$element = div;
            }

            // If there was an select in markup: use it for generating options
            if (this.$element.find("select option").length > 0 && this.options.options.length === 0) {
                this.options.options = [];
                this.options.optionDisplayStrings = [];
                this.$element.find("select option").each(function(i, e) {
                    this.options.options.push($(e).val());
                    this.options.optionDisplayStrings.push($.trim($(e).text()));

                    // Save selected state
                    if ($(e).attr("selected")) {
                        this.selectedIndex = i;
                    }

                }.bind(this));
            }

            this._createMissingElements();

            this.syncSelectElement = this.$element.find("select");
            this.inputElement = this.$element.find("input");

            this.$element.addClass("pathbrowser");
            this.$element.removeClass("focus");

            if (!this.options.placeholder) {
                this.options.placeholder = this.inputElement.attr("placeholder");
            }
            if (this.options.name) {
                this.syncSelectElement.attr("name", this.options.name);
            }

            this._update();
        },

        _createMissingElements: function() {
            if (this.$element.find("select").length === 0) {
                this.$element.append($("<select></select>"));
            }
            if (this.$element.find("input").length === 0) {
                this.$element
                    .prepend($("<input/>", {
                        type: "text"
                    })
                );
            }
        },

        /** @ignore */
        _readDataFromMarkup: function() {
            if (this.$element.attr("placeholder")) {
                this.options.placeholder = this.$element.attr("placeholder");
            }
            if (this.$element.attr("data-placeholder")) {
                this.options.placeholder = this.$element.attr("data-placeholder");
            }
            if (this.$element.attr("disabled") || this.$element.attr("data-disabled")) {
                this.options.disabled = true;
            }
            if (this.$element.attr("data-option-renderer")) {
                // Allow to choose from default option renderers
                this.options.optionRenderer = CUI.PathBrowser[this.$element.attr("data-option-renderer") + "OptionRenderer"];
            }

            if (this.$element.attr("data-root-path")) {
                this.options.rootPath = this.$element.attr("data-root-path");
            }

            // Register a callback function for option loader if defined
            var optionLoader = CUI.util.buildFunction(this.$element.attr("data-option-loader"), ["path", "callback"]);
            if (optionLoader) {
                this.options.optionLoader = optionLoader.bind(this);
            }
            // Root to use from the result object
            if (this.$element.attr("data-option-loader-root")) {
                this.options.optionLoaderRoot = this.$element.attr("data-option-loader-root");
            }
            // Custom value and title readers
            var optionValueReader = CUI.util.buildFunction(this.$element.attr("data-option-value-reader"), ["object"]);
            if (optionValueReader) {
                this.options.optionValueReader = optionValueReader.bind(this);
            }
            var optionTitleReader = CUI.util.buildFunction(this.$element.attr("data-option-title-reader"), ["object"]);
            if (optionTitleReader) {
                this.options.optionTitleReader = optionTitleReader.bind(this);
            }
        },

        /** @ignore */
        _update: function() {
            if (this.options.placeholder) {
                this.inputElement.attr("placeholder", this.options.placeholder);
            }

            if (this.options.disabled) {
                this.$element.addClass("disabled");
                this.inputElement.attr("disabled", "disabled");
            } else {
                this.$element.removeClass("disabled");
                this.inputElement.removeAttr("disabled");
            }

            if (this.syncSelectElement) {
                this.syncSelectElement.find("option:selected").removeAttr("selected");
            }
            if (this.selectedIndex >= 0) {
                if (this.syncSelectElement) {
                    $(this.syncSelectElement.find("option").get(this.selectedIndex)).attr("selected", "selected");
                }
                // Value to set is what is currently in the input field until the last slash + the option value
                var option = this.options.options[this.selectedIndex];
                if (option && option.indexOf("/") !== 0) {
                    // Option contains a relative path
                    var parentPath = "";
                    var iLastSlash = this.inputElement.attr("value").lastIndexOf("/");
                    if (iLastSlash >= 0) {
                        parentPath = this.inputElement.attr("value").substring(0, iLastSlash + 1);
                    }
                    option = parentPath + option;
                }
                this._setInputValue(option, true);
            } else {
                this._setInputValue("");
            }
        },

        /** @ignore */
        _setInputValue: function(newValue, moveCursor) {
            // Using select text util to select starting from last character to last character
            // This way, the cursor is placed at the end of the input text element
            if (newValue) {
                this.inputElement.attr("value", newValue);
                if (moveCursor && this.inputElement.is(":focus")) {
                    CUI.util.selectText(this.inputElement, newValue.length);
                }
            }
        },

        /** @ignore */
        _keyUp: function(event) {
            var key = event.keyCode;
            if (key === 8) {
                this.triggeredBackspace = false; // Release the key event
            }
        },

        /** @ignore */
        _keyPressed: function(event) {
            var key = event.keyCode;
            if (!this.dropdownList.isVisible()) {
                if (key === 40) {
                    this._inputChanged(); // Show box now!
                    event.preventDefault();
                }
            }
        },

        /** @ignore */
        _inputChanged: function() {
            var self = this;

            var searchFor = this.inputElement.attr("value");
            if (searchFor.length > 0) {
                this.options.autocompleteCallback(searchFor)
                    .done(
                        function(results) {
                            self._showAutocompleter(results);
                        }
                    )
                    .fail(
                        function() {
                            // TODO: implement
                        }
                    )
                ;
            } else {
                this.dropdownList.hide();
            }
        },

        /** @ignore */
        _showAutocompleter: function(results) {
            this.dropdownList.hide();

            if (results.length === 0) {
                return;
            }

            var optionRenderer = function(iterator, value) {
                return (this.options.optionRenderer.bind(this))(iterator, value);
            };

            this.dropdownList.set("optionRenderer", optionRenderer.bind(this));
            this.dropdownList.set("options", results);

            this.dropdownList.show();
        },

        /** @ignore */
        _defaultAutocompleteCallback: function(path) {
            var self = this;
            var def = $.Deferred();

            // Check if the input value starts and ends with a slash
            // If so, the options loader will be called if it exists, in order
            // to refresh the available options list.
            // Otherwise, it will just filter the options to only show the
            // matching ones in the auto completer div.
            if (/^\//.test(path) && /\/$/.test(path) && self.options.optionLoader) {
                var isCustomRoot = false;
                if (path === "/") {
                    // Use configured root path
                    if (self.options.rootPath) {
                        path = self.options.rootPath.replace(/\/$/, "");
                        if (path !== "") {
                            isCustomRoot = true;
                        } else {
                            path = "/";
                        }
                    }
                } else {
                    // Remove final slash
                    path = path.replace(/\/$/, "");
                }

                // Replace field value if the entered path was a custom root path
                if (isCustomRoot) {
                    self._setInputValue(path + "/");
                }

                // Make the option loader a promise to guarantee that the callback is
                // executed at the right rime
                var loader = {
                    loadOptions: self.options.optionLoader
                };
                var loaderDef = $.Deferred();
                loaderDef.promise(loader);
                loader.done(
                    function(object) {
                        if ($.isFunction(object.promise)) {
                            // Original function was already returning a promise
                            // Bind the rebuild options on that object's 'done' method
                            object.done(
                                function(object) {
                                    self._rebuildOptions(def, path, object);
                                }
                            );
                        } else {
                            // Original function was not returning a promise
                            self._rebuildOptions(def, path, object);
                        }
                    }
                );
                
                // Asynch optionLoader
                var results = loader.loadOptions(path, function(data) {
                    loaderDef.resolve(data);
                });
                
                //  Synch optionLoader
                if (results) loaderDef.resolve(results);

            } else {
                def.resolve(self._filterOptions(path));
            }

            return def.promise();
        },

        _rebuildOptions: function(def, path, object) {
            var self = this;

            var root = CUI.util.getNested(object, self.options.optionLoaderRoot);
            if (root) {
                var newOptions = [];
                var newOptionDisplayStrings = [];
                $.each(root, function(i, v) {
                    // Read the title and the value either from provided custom reader
                    // or using default expected object structure
                    var value;
                    if (self.options.optionValueReader) {
                        value = self.options.optionValueReader(v);
                    } else {
                        value = typeof v === "object" ? v.path : v;
                    }
                    newOptions.push(value);

                    var title = "";
                    if (self.options.optionTitleReader) {
                        title = self.options.optionTitleReader(v);
                    } else if (typeof v === "object") {
                        title = v.title;
                    }
                    newOptionDisplayStrings.push(title);
                }.bind(self));

                self.options.options = newOptions;
                self.options.optionDisplayStrings = newOptionDisplayStrings;

                var filtered = self._filterOptions(path);
                def.resolve(filtered);
            } else {
                def.reject();
            }
        },

        _filterOptions: function(searchFor) {
            var result = [];

            $.each(this.options.options, function(key, value) {
//                if (value.toLowerCase().indexOf(searchFor.toLowerCase(), 0) >= 0) {
                    result.push(key);
//                }
            }.bind(this));

            return result;
        }

    });

    CUI.util.plugClass(CUI.PathBrowser);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            $("[data-init~='pathbrowser']", e.target).pathBrowser();
        });
    }

    CUI.PathBrowser.defaultOptionRenderer = function(iterator, index) {
        var value = this.options.options[index];
        if (value.indexOf("/") === 0) {
            // Option contains an absolute path
            var iLastSlash = value.lastIndexOf("/");
            value = value.substring(iLastSlash + 1);
        }

        // Use alternate display strings if possible
        var valueCls = "pathbrowser-autocomplete-item-value";
        var titleMarkup = "";
        if (this.options.showTitles && this.options.optionDisplayStrings[index] && this.options.optionDisplayStrings[index].length > 0) {
            valueCls += " pathbrowser-autocomplete-item-value-with-title";
            titleMarkup += "<div class=\"pathbrowser-autocomplete-item-title\">" + this.options.optionDisplayStrings[index] + "</div>";
        }

        return $("<div class=\"" + valueCls + "\">" + value + "</div>" + titleMarkup);
    };


}(window.jQuery));