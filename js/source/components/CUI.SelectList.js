(function ($, window, undefined) {
    CUI.SelectList = new Class(/** @lends CUI.SelectList# */{
        toString: 'SelectList',

        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc A select list for drop down widgets. This widget is intended to be used by other widgets.
         *
         * <h2 class="line">Examples</h2>
         * 
         * <ul class="selectlist" data-init="selectlist">
         *     <li data-value="expr1">Expression 1</li>
         *     <li data-value="expr2">Expression 2</li>
         *     <li data-value="expr3">Expression 3</li>
         * </ul>
         *
         * <ul class="selectlist" data-init="selectlist" data-multiple="true">
         *     <li class="optgroup">
         *         <span>Group 1</span>
         *         <ul>
         *             <li data-value="expr1">Expression 1</li>
         *             <li data-value="expr2">Expression 2</li>
         *             <li data-value="expr3">Expression 3</li>
         *         </ul>
         *     </li>
         *     <li class="optgroup">
         *         <span>Group 2</span>
         *         <ul>
         *             <li data-value="expr4">Expression 4</li>
         *             <li data-value="expr5">Expression 5</li>
         *         </ul>
         *     </li>
         * </ul>
         *
         * @example
         * <caption>Instantiate with Class</caption>
         * var selectlist = new CUI.SelectList({
         *     element: '#mySelectList'
         * });
         *
         * // show the select list
         * selectlist.show();
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#mySelectList').selectList({
         *
         * });
         *
         * // jQuery style works as well for show/hide
         * $('#mySelectList').selectList('show');
         *
         * @example
         * <caption>Data API: Instantiate, set options, and show</caption>
         *
         * &lt;ul class=&quot;selectlist&quot; data-init=&quot;selectlist&quot;&gt;
         *     &lt;li data-value=&quot;expr1&quot;&gt;Expression 1&lt;/li&gt;
         *     &lt;li data-value=&quot;expr2&quot;&gt;Expression 2&lt;/li&gt;
         *     &lt;li data-value=&quot;expr3&quot;&gt;Expression 3&lt;/li&gt;
         * &lt;/ul&gt;
         *
         * &lt;ul class=&quot;selectlist&quot; data-init=&quot;selectlist&quot; data-multiple=&quot;true&quot;&gt;
         *     &lt;li class=&quot;optgroup&quot;&gt;
         *         &lt;span&gt;Group 1&lt;/span&gt;
         *         &lt;ul&gt;
         *             &lt;li data-value=&quot;expr1&quot;&gt;Expression 1&lt;/li&gt;
         *             &lt;li data-value=&quot;expr2&quot;&gt;Expression 2&lt;/li&gt;
         *             &lt;li data-value=&quot;expr3&quot;&gt;Expression 3&lt;/li&gt;
         *         &lt;/ul&gt;
         *     &lt;/li&gt;
         *     &lt;li class=&quot;optgroup&quot;&gt;
         *         &lt;span&gt;Group 2&lt;/span&gt;
         *         &lt;ul&gt;
         *             &lt;li data-value=&quot;expr4&quot;&gt;Expression 4&lt;/li&gt;
         *             &lt;li data-value=&quot;expr5&quot;&gt;Expression 5&lt;/li&gt;
         *         &lt;/ul&gt;
         *     &lt;/li&gt;
         * &lt;/ul&gt;
         * 
         *
         * @example
         * <caption>Initialize with custom paramters to load remotely</caption>
         * 
         * &lt;ul class=&quot;selectlist&quot; data-init=&quot;selectlist&quot; data-type=&quot;dynamic&quot; data-dataurl=&quot;remotehtml.html&quot;&gt;
         *     
         * &lt;/ul&gt;
         *
         * @description Creates a new select list
         * @constructs
         * 
         * @param  {Object} options Component options
         * @param  {Mixed} options.element jQuery selector or DOM element to use for panel
         * @param  {String} [options.type=static] static or dynamic list
         * @param  {Boolean} [options.multiple=false] multiple selection or not
         * @param  {Object} options.relatedElement DOM element to position at
         * @param  {Boolean} [options.autofocus=true] automatically sets the focus on the list
         * @param  {Boolean} [options.autohide=true] automatically closes the list when it loses its focus
         * @param  {String} [options.dataurl] URL to receive values dynamically
         * @param  {String} [options.dataurlformat=html] format of the dynamic data load
         * @param  {Object} [options.dataadditional] additonal data to be sent
         * @param  {Function} [options.loadData] function to be called if more data is needed. This must not be used with a set dataurl.
         *
         * 
         */
        construct: function (options) {
            this.applyOptions();

            this.$element
                .on('change:type', this._setType.bind(this))
                .on('change:autohide', this._setAutohide.bind(this))
                .on('click', '[role="option"]', this._triggerSelected.bind(this));

            // accessibility
            this._makeAccessible();
        },

        defaults: {
            type: 'static', // static or dynamic
            multiple: false,
            relatedElement: null,
            autofocus: true, // autofocus on show
            autohide: true, // automatically hides the box if it loses focus
            dataurl: null,
            dataurlformat: 'html',
            datapaging: true,
            datapagesize: 10,
            dataadditional: null,
            loadData: $.noop, // function to receive more data
            position: 'center bottom-1'  // -1 to override the border
        },

        applyOptions: function () {
            this._setType();
        },

        /**
         * @private
         */
        _setAutohide: function () {
            var self = this,
                receivedFocus = false;

            if (this.options.autohide) {
                this.$element
                    .on('focusout.selectlist-autohide', function (event) {
                        clearTimeout(self._autohideTimer);
                        self._autohideTimer = setTimeout(function () {
                            if (!receivedFocus) {
                                self.hide();
                            }
                            receivedFocus = false;
                        }, 500);
                    })
                    .on('focusin.selectlist-autohide', function (event) {
                        receivedFocus = true;
                    });
            } else {
                this.$element.off('focusout.selectlist-autohide focusin.selectlist-autohide');
            }
        },

        /**
         * @private
         */
        _setType: function () {
            var self = this,
                timeout;

            function timeoutLoadFunc() {
                var elem = self.$element.get(0),
                    scrollHeight = elem.scrollHeight,
                    scrollTop = elem.scrollTop;

                if ((scrollHeight - self.$element.height()) <= (scrollTop + 30)) {
                    self._handleLoadData();
                }
            }

            // we have a dynamic list of values
            if (this.options.type === 'dynamic') {

                this.$element.on('scroll.selectlist-dynamic-load', function (event) {
                    // debounce
                    if (timeout) {
                        clearTimeout(timeout);
                    }

                    if (self._loadingComplete || this._loadingIsActive) {
                        return;
                    }

                    timeout = setTimeout(timeoutLoadFunc, 500);
                });
            } else { // static
                this.$element.off('scroll.selectlist-dynamic-load');
            }
        },

        /**
         * adds some accessibility attributes and features
         * http://www.w3.org/WAI/PF/aria/roles#listbox
         * @private
         */
        _makeAccessible: function () {
            this.$element.attr({
                'role': 'listbox',
                'tabindex': -1, // the list itself is not focusable
                'aria-hidden': true,
                'aria-multiselectable': this.options.multiple
            });

            this._makeAccessibleListOption(this.$element.children());

            // setting tabindex
            this.$element.on('focusin focusout', 'li[role="option"]', function (event) {
                $(event.currentTarget).attr('tabindex', event.type === 'focusin' ? -1 : 0);
            });

            // keyboard handling
            this.$element.on('keydown', 'li[role="option"]', function (event) {
                // enables keyboard support

                var elem = $(event.currentTarget),
                    entries = $(event.delegateTarget)
                        .find('[role="option"]')
                        .not('[aria-disabled="true"]'), // ignore disabled
                    focusElem = elem,
                    keymatch = true,
                    idx = entries.index(elem);

                switch (event.which) {
                    case 13: // enter
                    case 32: // space
                        // choose element
                        elem.trigger('click');
                        event.preventDefault();
                        keymatch = false;
                        break;
                    case 27: //esc
                        elem.trigger('blur');
                        keymatch = false;
                        break;
                    case 33: //page up
                    case 37: //left arrow
                    case 38: //up arrow
                        focusElem = idx-1 > -1 ? entries[idx-1] : entries[entries.length-1];
                        break;
                    case 34: //page down
                    case 39: //right arrow 
                    case 40: //down arrow
                        focusElem = idx+1 < entries.length ? entries[idx+1] : entries[0];
                        break;
                    case 36: //home
                        focusElem = entries[0];
                        break;
                    case 35: //end
                        focusElem = entries[entries.length-1];
                        break;
                    default:
                        keymatch = false;
                        break;
                }

                if (keymatch) { // if a key matched then we set the currently focused element
                    event.preventDefault();
                    $(focusElem).trigger('focus');
                }
            });
        },

        /**
         * makes the list options accessible
         * @private
         * @param  {jQuery} elem
         */
        _makeAccessibleListOption: function (elem) {
            elem.each(function (i, e) {
                var entry = $(e);

                // group header
                if (entry.hasClass('optgroup')) {
                    entry.attr({
                        'role': 'presentation',
                        'tabindex': -1
                    }).children('ul').attr({
                        'role': 'group'
                    }).children('li').attr({
                        'role': 'option',
                        'tabindex': 0
                    });

                } else {
                    entry.attr({
                        'role': 'option',
                        'tabindex': 0
                    });
                }
            });
        },

        /**
         * @private
         */
        _show: function () {
            var self = this;

            this.$element
                .addClass('visible')
                .attr('aria-hidden', false);

            this.$element.position({
                my: 'top',
                at: this.options.position,
                of: this.options.relatedElement
            });

            if (this.options.autofocus) {
                this.$element.find('li[role="option"]:first').trigger('focus');
            }

            // if dynamic start loading
            if (this.options.type === 'dynamic') {
                this._handleLoadData().done(function () {
                    self.$element.find('li[role="option"]:first').trigger('focus');
                    this._setAutohide();
                });
            } else { // otherwise set autohide immediately
                this._setAutohide();
            }
        },

        /**
         * @private
         */
        _hide: function () {
            if (this._autohideTimer) {
                clearTimeout(this._autohideTimer);
            }
            this.$element
                .removeClass('visible')
                .attr('aria-hidden', true);

            
            this.reset();
        },

        /**
         * triggers an event for the currently selected element
         * @fires SelectList#selected
         * @private
         */
        _triggerSelected: function (event) {
            var cur = $(event.currentTarget),
                val = cur.data('value'),
                display = cur.text();

            cur.trigger($.Event('selected', {
                selectedValue: val,
                displayedValue: display
            }));
        },

        /**
         * deletes the item from the dom
         */
        clearItems: function () {
            this.$element.empty();
        },

        /**
         * current position for the pagination
         * @private
         * @type {Number}
         */
        _pagestart: 0,

        /**
         * indicates if all data was fetched
         * @private
         * @type {Boolean}
         */
        _loadingComplete: false,

        /**
         * indicates if currently data is fetched
         * @private
         * @type {Boolean}
         */
        _loadingIsActive: false,

        /**
         * handle asynchronous loading of data (type == dynamic)
         * @private
         */
        _handleLoadData: function () {
            var promise,
                self = this,
                end = this._pagestart + this.options.datapagesize,
                wait = $('<div/>',{
                    'class': 'selectlist-wait'
                }).append($('<span/>', {
                    'class': 'wait'
                }));

            if (this._loadingIsActive) {
                return;
            }

            // activate fetching
            this._loadingIsActive = true;

            // add wait
            this.$element.append(wait);

            // load from given URL
            if (this.options.dataurl) {
                promise = $.ajax({
                    url: this.options.dataurl,
                    context: this,
                    dataType: this.options.dataurlformat,
                    data: $.extend({
                        start: this._pagestart,
                        end: end
                    }, this.options.dataadditional || {})
                }).done(function (data) {
                    var cnt = 0;

                    if (self.options.dataurlformat === 'html') {
                        var elem = $(data);

                        cnt = elem.filter('li').length;

                        self._makeAccessibleListOption(elem);
                        self.$element.append(elem);
                    }

                    // if not enough elements came back then the loading is complete
                    if (cnt < self.options.datapagesize) {
                        this._loadingComplete = true;
                    }

                });

            } else { // expect custom function to handle
                promise = this.options.loadData.call(this, this._pagestart, end);
            }

            // increase to next page
            this._pagestart = end;

            promise.always(function () {
                wait.remove();
                this._loadingIsActive = false;
            });

            return promise;
        },

        /**
         * resets the dynamic loaded data
         */
        reset: function () {
            if (this.options.type === 'dynamic') {
                this.clearItems();
                this._pagestart = 0;
                this._loadingComplete = false;
            }
        },

        /**
         * triggers a loading operation 
         * this requires to have the selectlist in a dynamic configuration
         * @param  {Boolean} reset resets pagination
         */
        triggerLoadData: function (reset) {
            if (reset) {
                this.reset();
            }

            this._handleLoadData();
        }
    });

    CUI.util.plugClass(CUI.SelectList);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (event) {
            $('[data-init~=selectlist]', event.target).selectList();
        });
    }

    /**
     * Triggered when option was selected
     *
     * @name CUI.SelectList#selected
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.selectedValue value which was selected
     * @param {String} event.displayedValue displayed text of the selected element
     */
    
    /**
     * Triggered when option was unselected
     *
     * @name CUI.SelectList#unselected
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.selectedValue value which was unselected
     * @param {String} event.displayedValue displayed text of the unselected element
     */

}(jQuery, this));
