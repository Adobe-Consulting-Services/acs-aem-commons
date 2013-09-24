(function ($, window, undefined) {
    CUI.Autocomplete = new Class(/** @lends CUI.Autocomplete# */{
        toString: 'Autocomplete',

        extend: CUI.Widget,

        defaults: {
            mode: 'starts', // filter mode ['starts', 'contains']
            delay: 500,
            showtypeahead: false,
            showsuggestions: false,
            showclearbutton: false,
            showtags: false,

            selectlistConfig: null,
            tagConfig: null
        },

        construct: function () {
            var self = this;

            // find elements
            this._input = this.$element.children('input');
            this._selectlist = this.$element.find('.selectlist');
            this._tags = this.$element.find('.taglist');

            this._suggestionsBtn = this.$element.find('.autocomplete-suggestion-toggle');


            // apply
            this.applyOptions();
        },

        applyOptions: function () {
            this._setClearButton();

            this._setTags();
            this._setSelectlist();
            this._setTypeahead();
            this._setSuggestions();

            this._setType();
        },

        /**
         * initializes the type of the autocomplete
         */
        _setType: function () {
            if (this._selectListWidget.options.type === 'static') {
                this.$element.on('query', this.handleStaticFilter.bind(this));
            } else if (this._selectListWidget.options.type === 'dynamic') {
                this.$element.on('query', this.handleDynamicFilter.bind(this));
            }
        },

        /**
         * initialize the clear button
         * @private
         */
        _setClearButton: function () {
            var self = this;

            if (this.options.showclearbutton) {

                // create button if not there
                if (!this._clearBtn) {
                    this._clearBtn = $('<button/>', {
                        'class': 'autocomplete-clear icon-close'
                    }).fipo('tap', 'click', function (event) {
                        event.preventDefault();

                        self.clear();
                        self._input.focus();
                    }).finger('click', false);
                }

                this._clearBtn.appendTo(this.$element);
                this._input.on('keyup.autocomplete-clearbtn', this._refreshClear.bind(this));
                this._refreshClear();
            } else {
                if (this._clearBtn) {
                    this._clearBtn.detach();
                }
                this._input.off('keyup.autocomplete-clearbtn');
            }
        },

        /*_setSuggestions: function () {
            var self = this;

            if (this.options.showsuggestions) {

                // if the element is not there, create it
                if (this._suggestions.length === 0) {
                    this._suggestions = $('<ul/>', {
                        'class': 'selectlist autocomplete-suggestions'
                    }).appendTo(this.$element);
                }

                this._suggestions.selectList($.extend({
                    relatedElement: this._input
                }, this.options.suggestionConfig || {}));

                this._selectListSuggestion = this._suggestions.data('selectList');

                // if the button to trigger the suggestion box is not there, 
                // then we add it
                if (this._suggestionsBtn.length === 0) {

                    this._suggestionsBtn = $('<button/>', {
                        'class': 'autocomplete-suggestion-toggle'
                    });

                    this._suggestionsBtn.appendTo(this.$element);
                }

                // handler to open usggestion box
                this._suggestionsBtn.fipo('tap', 'click', function (event) {
                    event.preventDefault();
                    self._toggleSuggestions();
                }).finger('click', false);


                this._suggestions
                    // receive the value from the list
                    .on('selected.autcomplete-suggestion', this._handleSuggestionSelected.bind(this))
                    // handle open/hide for the button
                    .on('show.autcomplete-suggestion hide.autcomplete-suggestion', function (event) {
                        self._suggestionsBtn.toggleClass('active', event.type === 'show');
                    });
                // add class to input to to increase padding right for the button
                this._input.addClass('autocomplete-has-suggestion-btn');
            } else {
                this._suggestionsBtn.remove();
                this._suggestions.off('selected.autcomplete-suggestion show.autcomplete-suggestion hide.autcomplete-suggestion');
                this._input.removeClass('autocomplete-has-suggestion-btn');
            }
        },*/

        /**
         * initializes the select list widget
         * @private
         */
        _setSelectlist: function () {
            var self = this;

            // if the element is not there, create it
            if (this._selectlist.length === 0) {
                this._selectlist = $('<ul/>', {
                    'class': 'selectlist'
                }).appendTo(this.$element);
            }

            this._selectlist.selectList($.extend({
                relatedElement: this._input,
                autofocus: false,
                autohide: false
            }, this.options.selectlistConfig || {}));

            this._selectListWidget = this._selectlist.data('selectList');

            this._selectlist
                // receive the value from the list
                .on('selected.autcomplete', this._handleSelected.bind(this));
        },

        /**
         * initializes the tags for multiple options
         * @private
         */
        _setTags: function () {
            if (this.options.showtags) {

                // if the element is not there, create it
                if (this._tags.length === 0) {
                    this._tags = $('<ul/>', {
                        'class': 'taglist'
                    }).appendTo(this.$element);
                }

                this._tags.tagList(this.options.tagConfig || {});
                this._tagList = this._tags.data('tagList');

                this._input.on('keyup.autocomplete-addtag', this._addTag.bind(this));

            } else {
                this._input.off('keyup.autocomplete-addtag');
            }
        },

        /**
         * initializes the typeahead functionality
         * @fires Autocomplete#query
         * @private
         */
        _setTypeahead: function () {
            var self = this,
                timeout;

            function timeoutLoadFunc() {
                self.$element.trigger($.Event('query', {
                    value: self._input.val()
                }));
            }

            if (this.options.showtypeahead) {

                // bind keyboard input listening
                this._input.on('keyup.autocomplete', function (event) {
                    // debounce
                    if (timeout) {
                        clearTimeout(timeout);
                    }

                    timeout = setTimeout(timeoutLoadFunc, self.options.delay);
                });

            } else {
                this._input.off('keyup.autocomplete');
            }
        },

        _setSuggestions: function () {
            var self = this;

            if (this.options.showsuggestions) {

                // if the button to trigger the suggestion box is not there, 
                // then we add it
                if (this._suggestionsBtn.length === 0) {

                    this._suggestionsBtn = $('<button/>', {
                        'class': 'autocomplete-suggestion-toggle'
                    });

                    this._suggestionsBtn.appendTo(this.$element);
                }

                // handler to open usggestion box
                this._suggestionsBtn.fipo('tap', 'click', function (event) {
                    event.preventDefault();
                    self._toggleSuggestions();
                }).finger('click', false);

                // add class to input to to increase padding right for the button
                this._input.addClass('autocomplete-has-suggestion-btn');
            } else {
                this._suggestionsBtn.remove();
                this._input.removeClass('autocomplete-has-suggestion-btn');
            }
        },

        /*
        _setTypeahead: function () {
            var self = this,
                timeout;

            function timeoutLoadFunc() {
                self._selectListTypeahead.set('dataadditional', {
                    value: self._input.val()
                });
                self._selectListTypeahead.show();
                self._selectListTypeahead.triggerLoadData(true);
            }

            if (this.options.showtypeahead) {

                // if the element is not there, create it
                if (this._typeahead.length === 0) {
                    this._typeahead = $('<ul/>', {
                        'class': 'selectlist autocomplete-typeahead'
                    }).appendTo(this.$element);
                }

                this._typeahead.selectList($.extend({
                    relatedElement: this._input,
                    autofocus: false,
                    autohide: false
                }, this.options.typeaheadConfig || {}));

                this._selectListTypeahead = this._typeahead.data('selectList');

                // bind keyboard input listening
                this._input.on('keyup.autocomplete-typeahead', function (event) {
                    // debounce
                    if (timeout) {
                        clearTimeout(timeout);
                    }

                    timeout = setTimeout(timeoutLoadFunc, 500);
                });

            } else {
                this._input.off('keyup.autocomplete-typeahead');
            }
        },*/

        /**
         * adds a new tag when pressed button was Enter
         * @private
         * @param {jQuery.Event} event
         */
        _addTag: function (event) {
            if (event.which !== 13) {
                return;
            }

            this._tagList.addItem(this._input.val());
            this.clear();
        },

        _handleSelected: function (event) {
            this._selectListWidget.hide();
            
            if (this.options.showtags) {
                this._tagList.addItem(event.displayedValue);
            } else {
                this._input.val(event.displayedValue);
            }

            this._input.trigger('focus');
        },

        _toggleSuggestions: function () {
            this._selectListWidget.toggleVisibility();
        },

        _refreshClear: function () {
            this._clearBtn.toggleClass('hide', this._input.val().length === 0);
        },

        /**
         * handles a static list filter (type == static) based on the defined mode
         * @param  {jQuery.Event} event
         */
        handleStaticFilter: function (event) {
            this._selectList.find('[role="option"]').each(function (i, e) {

            });
        },

        /**
         * handles a static list filter (type == static) based on the defined mode
         * @param  {jQuery.Event} event
         */
        handleDynamicFilter: function (event) {
            this._selectListWidget.set('dataadditional', {
                value: event.value
            });
            this._selectListWidget.show();
            this._selectListWidget.triggerLoadData(true);
        },

        /**
         * clears the autocomplete input field
         */
        clear: function () {
            this._input.val('');
            this._refreshClear();
        },

        /**
         * disables the autocomplete
         */
        disable: function () {
            this.$element.addClass('disabled');
            this._input.prop('disabled', true);
            this._suggestionsBtn.prop('disabled', true);
        },

        /**
         * enables the autocomplete
         */
        enable: function () {
            this.$element.removeClass('disabled');
            this._input.prop('disabled', false);
            this._suggestionsBtn.prop('disabled', false);
        }
    });

    CUI.util.plugClass(CUI.Autocomplete);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (e) {
            $('[data-init~=autocomplete]', e.target).autocomplete();
        });
    }

}(jQuery, this));
