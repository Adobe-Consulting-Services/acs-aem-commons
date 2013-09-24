(function ($, window, undefined) {
    CUI.TagList = new Class(/** @lends CUI.TagList# */{
        toString: 'TagList',

        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc A tag list for input widgets. This widget is intended to be used by other widgets.
         *
         * <h2 class="line">Examples</h2>
         *  
         * <ol class="taglist" data-init="taglist" data-fieldname="myrequestparam" style="margin: 2rem">
         *     <li>
         *         <button class="icon-close"></button>
         *         Carrot
         *         <input type="hidden" value="Carrot"/>
         *     </li>
         *     <li>
         *         <button class="icon-close"></button>
         *         Banana
         *         <input type="hidden" value="Banana"/>
         *     </li>
         *     <li>
         *         <button class="icon-close"></button>
         *         Apple
         *         <input type="hidden" value="Apple"/>
         *     </li>
         * </ol>
         *
         * @example
         * <caption>Data API: Instantiate, set options, and show</caption>
         * 
         * &lt;ol class=&quot;taglist&quot; data-init=&quot;taglist&quot; data-fieldname=&quot;myrequestparam&quot;&gt;
         *     &lt;li&gt;
         *         &lt;button class=&quot;icon-close&quot;&gt;&lt;/button&gt;
         *         Carrot
         *         &lt;input type=&quot;hidden&quot; value=&quot;Carrot&quot;/&gt;
         *     &lt;/li&gt;
         *     &lt;li&gt;
         *         &lt;button class=&quot;icon-close&quot;&gt;&lt;/button&gt;
         *         Banana
         *         &lt;input type=&quot;hidden&quot; value=&quot;Banana&quot;/&gt;
         *     &lt;/li&gt;
         *     &lt;li&gt;
         *         &lt;button class=&quot;icon-close&quot;&gt;&lt;/button&gt;
         *         Apple
         *         &lt;input type=&quot;hidden&quot; value=&quot;Apple&quot;/&gt;
         *     &lt;/li&gt;
         * &lt;/ol&gt;
         *
         * @description Creates a new tag list
         * @constructs
         * 
         * @param  {Object} options Component options
         * @param  {Mixed} options.element jQuery selector or DOM element to use for panel
         * @param  {String} options.fieldname fieldname for the input fields
         * @param  {Array} options.values to set the taglist
         *
         * @fires TagList#itemadded
         * @fires TagList#itemremoved
         * 
         */
        construct: function (options) {
            var self = this;

            this.applyOptions();

            this.$element
                .on('change:values', this._setValues.bind(this));

            this.$element.fipo('tap', 'click', 'button', function (event) {
                var elem = $(event.currentTarget).next('input');

                self.removeItem(elem.val());
            });

            // accessibility
            this._makeAccessible();
        },

        defaults: {
            fieldname: "",
            values: null,
            tag: 'li'
        },

        /**
         * existing values in the tag list
         * @private
         * @type {Array}
         */
        _existingValues: null,

        applyOptions: function () {
            var self = this;

            this._existingValues = [];

            this.options.values = this.options.values || [];

            // set values if given
            if (this.options.values.length > 0) {
                this._setValues();
            } else { // read from markup
                this.$element.find('input').each(function (i, e) {
                    var elem = $(e);
 
                    // add to options.values
                    self._existingValues.push(elem.attr('value'));
                });
            }
        },

        /**
         * @private
         */
        _setValues: function () {
            var items = this.options.values;

            // remove list elements
            this.$element.empty();

            // clear options to readd
            this.options.values = [];
            // add elements again
            this.addItem(items);
        },

        /**
         * adds some accessibility attributes and features
         * http://www.w3.org/WAI/PF/aria/roles#list
         * @private
         */
        _makeAccessible: function () {
            this.$element.attr({
                'role': 'list'
            });

            this.$element.children(this.options.tag).attr({
                'role': 'listitem'
            });
        },

        /**
         * @private
         */
        _show: function () {
            this.$element
                .show()
                .attr('aria-hidden', false);
        },

        /**
         * @private
         */
        _hide: function () {
            this.$element
                .hide()
                .attr('aria-hidden', true);
        },

        /**
         * remove an item from the DOM
         * @private
         * @param  {String} item
         */
        _removeItem: function (item) {
            var elem = this.$element.find('input[value="' + item + '"]');

            if (elem.length > 0) {
                elem.parent().remove();

                this.$element.trigger($.Event('itemremoved'), {
                    value: item
                });
            }
        },

        /**
         * adds a new item to the DOM
         * @private
         * @param  {String|Object} item entry to be displayed
         */
        _appendItem: function (item) {
            var display, val, elem;

            // see if string or object
            if ($.type(item) === "string") {
                display = val = item;
            } else {
                display = item.display;
                val = item.value;
            }

            // always be a string
            val += "";

            if (($.inArray(val, this._existingValues) > - 1) || val.length === 0) {
                return;
            }

            // add to internal storage
            this._existingValues.push(val); // store as string

            // add DOM element
            elem = $('<'+ this.options.tag +'/>', {
                'role': 'listitem',
                'text': display
            });

            $('<button/>', {
                'class': 'icon-close'
            }).prependTo(elem);

            $('<input/>', {
                'type': 'hidden',
                'value': val,
                'name': this.options.fieldname
            }).appendTo(elem);

            this.$element.append(elem);

            this.$element.trigger($.Event('itemadded'), {
                value: val,
                display: display
            });
        },

        /**
         * @param {String} item value to be deleted
         */
        removeItem: function (item) {
            var idx = this._existingValues.indexOf("" + item);

            if (idx > -1) {
                this._removeItem(item);
                this._existingValues.splice(idx, 1);
            }
        },

        /**
         * @param  {String|Object|Array} item
         * @param  {String} item.display
         * @param  {String} item.value
         */
        addItem: function (item) {
            var self = this,
                items = $.isArray(item) ? item : [item];

            $.each(items, function (i, item) {
                self._appendItem(item);
            });
        }
    });

    CUI.util.plugClass(CUI.TagList);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (event) {
            $('[data-init~=taglist]', event.target).tagList();
        });
    }

    /**
     * Triggered when an item was added
     *
     * @name CUI.TagList#itemadded
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.value value which was added
     * @param {String} event.display displayed text of the element
     */
    
    /**
     * Triggered when an item was removed
     *
     * @name CUI.TagList#itemremoved
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.value value which was removed
     */

}(jQuery, this));
