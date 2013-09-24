(function ($, window, undefined) {
    CUI.Select = new Class(/** @lends CUI.Select# */{
        toString: 'Select',

        extend: CUI.Widget,
        
        /**
         * @extends CUI.Widget
         * @classdesc a widget which is similar to the native &lt;select&gt;
         *
         * <h2 class="line">Examples</h2>
         *
         * <span class="select" data-init="select">
         *     <button type="button">Select</button>
         *     <select>
         *         <option value="1">One</option>
         *         <option value="2">Two</option>
         *         <option value="3">Three</option>
         *     </select>
         * </span>
         *
         * <span class="select" data-init="select">
         *     <button type="button">Select</button>
         *     <select multiple="true">
         *         <option value="1">One</option>
         *         <option value="2">Two</option>
         *         <option value="3">Three</option>
         *     </select>
         * </span>
         *
         * @example
         * <caption>Instantiate with Class</caption>
         * var selectlist = new CUI.Select({
         *     element: '#mySelect'
         * });
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#mySelect').select({
         *
         * });
         *
         * @example
         * <caption>Data API: Instantiate, set options</caption>
         *
         * &lt;span class=&quot;select&quot; data-init=&quot;select&quot;&gt;
         *     &lt;button type=&quot;button&quot;&gt;Select&lt;/button&gt;
         *     &lt;select&gt;
         *         &lt;option value=&quot;1&quot;&gt;One&lt;/option&gt;
         *         &lt;option value=&quot;2&quot;&gt;Two&lt;/option&gt;
         *         &lt;option value=&quot;3&quot;&gt;Three&lt;/option&gt;
         *     &lt;/select&gt;
         * &lt;/span&gt;
         *
         * @description Creates a new select
         * @constructs
         *
         * @param {Object} options Component options
         * @param {Mixed} options.element jQuery selector or DOM element to use for panel
         * @param {String} [options.type=static] static or dynamic list
         * @param {Boolean} [nativewidget=false] shows a native &lt;select&gt; instead of a SelectList widget
         * @param {Boolean} [nativewidgetonmobile=true] forces a native &lt;select&gt; on a mobile device if possible
         * @param {Boolean} [multiple=false] multiple selection, will automatically be detected form a given &lt;select&gt; source
         */
        construct: function () {
            var self = this;

            // find elements
            this._button = this.$element.children('button');
            this._select = this.$element.children('select');
            this._selectList = this.$element.children('.selectlist');
            this._tagList = this.$element.children('.taglist');
            this._valueInput = this.$element.children('input[type=hidden]');

            // apply
            this.applyOptions();
        },

        defaults: {
            type: 'static',
            nativewidget: false,
            nativewidgetonmobile: true,
            multiple: false,
            tagConfig: null,
            selectlistConfig: null
        },

        applyOptions: function () {
            var forcedNativeWidget = this.options.nativewidgetonmobile && CUI.util.isTouch && this.options.type === 'static';

            // there is a select given so read the "native" config options
            if (this._select.length > 0) {
                // if multiple set multiple
                if (this._select.prop('multiple')) {
                    this.options.multiple = true;
                }
            }
            

            if (this.options.nativewidget || forcedNativeWidget) {
                this._setNativeWidget(forcedNativeWidget);
            } else {
                this._setSelectList();
            }

            this._setTagList();

            // if we have a static <select> based list
            // load the values from markup
            if (this.options.type === 'static') {
                this._handleNativeSelect();
            }
        },

        /**
         * this option is mainly supposed to be used on mobile
         * and will just work with static lists
         * @private
         * @param {Boolean} [force]
         */
        _setNativeWidget: function (force) {
            var self = this;

            if (this.options.nativewidget || force) {
                this._select.css({
                    display: 'block',
                    width: this._button.outerWidth(),
                    height: this._button.outerHeight(),
                    opacity: 0.01
                });

                this._select.position({
                    my: 'left top',
                    at: 'left top',
                    of: this._button
                });

                if (this.options.multiple) {
                    this._setTagList();
                }

                // if it is in single selection mode, 
                // then the btn receives the label of the selected item
                this._select.on('change.select', this._handleNativeSelect.bind(this));

            } else {
                this._select.off('change.select');
            }
        },

        /**
         * handles a native change event on the select
         * @private
         */
        _handleNativeSelect: function (event) {
            var self = this;

            if (self.options.multiple) {
                // loop over all options
                $.each(self._select[0].options, function (i, opt) {
                    if (opt.selected) {
                        self._tagListWidget.addItem({
                            value: opt.value,
                            display: opt.text
                        });
                    } else {
                        self._tagListWidget.removeItem(opt.value);
                    }
                });
            } else {
                self._button.text(self._select[0][self._select[0].selectedIndex].text);
            }
        },

        /**
         * this function parses the values from the native select
         * and prints the right markup for the SelectList widget
         * This function may only be called in SelectList widget mode.
         * @private
         */
        _parseMarkup: function () {
            var self = this,
                optgroup = this._select.children('optgroup');

            function parseGroup(parent, dest) {
                parent.children('option').each(function (i, e) {
                    var opt = $(e);

                    $('<li/>', {
                        'data-value': opt.val(),
                        'text': opt.text()
                    }).appendTo(dest);
                });
            }

            // optgroups are part of the select -> different markup
            if (optgroup.length > 0) {
                optgroup.each(function (i, e) {
                    var group = $(e),
                        entry = $('<li/>', {
                                'class': 'optgroup'
                            }).append($('<span/>', {
                                'text': group.attr('label')
                            }));

                    parseGroup(group, $('<ul/>').appendTo(entry));

                    self._selectList.append(entry);
                });
            } else { // flat select list
                parseGroup(this._select, this._selectList);
            }
        },

        /**
         * set SelectList widget
         * @private
         */
        _setSelectList: function () {
            var self = this,
                type = 'static';

            // if the element is not there, create it
            if (this._selectList.length === 0) {
                this._selectList = $('<ul/>', {
                    'class': 'selectlist'
                }).appendTo(this.$element);
            }

            // read values from markup
            if (this._select.length > 0) {
                this._parseMarkup();
            } else { // if no <select> wa found then a dynamic list is expected
                type = 'dynamic';
            }

            this._selectList.selectList($.extend({
                relatedElement: this._button,
                type: type
            }, this.options.selectlistConfig || {}));

            this._selectListWidget = this._selectList.data('selectList');

            // handler to open usggestion box
            this._button.fipo('tap', 'click', function (event) {
                event.preventDefault();
                self._toggleList();
            }).finger('click', false);

            this._selectList
                // receive the value from the list
                .on('selected.select', this._handleSelected.bind(this))
                // handle open/hide for the button
                .on('show.dropdown hide.select', function (event) {
                    self._button.toggleClass('active', event.type === 'show');
                });
        },

        /**
         * sets a tag list for the multiple selection
         * @private
         */
        _setTagList: function () {
            if (this.options.multiple) {
                // if the element is not there, create it
                if (this._tagList.length === 0) {
                    this._tagList = $('<ol/>', {
                        'class': 'taglist'
                    }).appendTo(this.$element);
                }

                this._tagList.tagList(this.options.tagConfig || {});

                this._tagListWidget = this._tagList.data('tagList');
            }
        },

        /**
         * handles a select of a SelectList widget
         * @private
         */
        _handleSelected: function (event) {
            this._selectListWidget.hide();

            // set select value
            this._select.val(event.selectedValue);

            if (this.options.multiple) {
                this._tagListWidget.addItem({
                    value: event.selectedValue,
                    display: event.displayedValue
                });
            } else {
                // set the button label
                this._button.text(event.displayedValue);
                // in case it is dynamic a value input should be existing
                this._valueInput.val(event.selectedValue);
            }

            this._button.trigger('focus');
        },

        /**
         * toggles the visibility of a SelectList widget
         * @private
         */
        _toggleList: function () {
            this._selectListWidget.toggleVisibility();
        }
    });

    CUI.util.plugClass(CUI.Select);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (e) {
            $('[data-init~=select]', e.target).select();
        });
    }

}(jQuery, this));
