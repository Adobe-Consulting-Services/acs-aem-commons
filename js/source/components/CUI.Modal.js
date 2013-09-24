(function ($, window, undefined) {

    CUI.Modal = new Class(/** @lends CUI.Modal# */{
        toString: 'Modal',

        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc A dialog that prevents interaction with page elements while displayed. Modal will use existing markup if it is present, or create markup if <code>options.element</code> has no children.
         *
         * <h2 class="line">Example</h2>
         *     <a href="#myModal" class="button" data-toggle="modal">Show Modal</a>
         *     <div id="myModal" class="modal">
         *         <div class="modal-header">
         *         <h2>A Sample Modal</h2>
         *         <button type="button" class="close" data-dismiss="modal">&times;</button>
         *     </div>
         *     <div class="modal-body">
         *         <p>Some sample content.</p>
         *     </div>
         *     <div class="modal-footer">
         *         <button data-dismiss="modal">Ok</button>
         *     </div>
         * </div>
         *
         * @example
         * <caption>Instantiate with Class</caption>
         * var modal = new CUI.Modal({
         *     element: '#myModal',
         *     header: 'My Modal',
         *     content: '&lt;p&gt;Content here.&lt;/p&gt;',
         *     buttons: [{
         *         label: 'Save',
         *         className: 'primary',
         *         click: function(evt) {
         *             console.log('Modal: This would usually trigger a save...');
         *             this.hide(); // could also use evt.dialog.hide();
         *         }
         *     }]
         * });
         *
         * // Hide the modal, change the heading, then show it again
         * modal.hide().set({ header: 'My Modal Again'}).show();
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#myModal').modal({
         *     header: 'My Modal',
         *     content: '&lt;p&gt;Content here.&lt;/p&gt;',
         *     buttons: [{
         *         label: 'Close',
         *         click: 'hide', // Specifying 'hide' causes the dialog to close when clicked
         *     }]
         * });
         *
         * // jQuery style works as well
         * $('#myModal').modal('hide');
         *
         * // A reference to the element's modal instance is stored as data-modal
         * var modal = $('#myModal').data('modal');
         * modal.hide();
         *
         * @example 
         * <caption>Data API: Instantiate and show modal</caption>
         * <description>When using a <code class="prettify">&lt;button&gt;</code>, specify the jQuery selector for the element using <code>data-target</code>. Markup should exist already if no options are specified.</description>
         * &lt;button data-target=&quot;#myModal&quot; data-toggle=&quot;modal&quot;&gt;Show Modal&lt;/button&gt;
         *
         * @example
         * <caption>Data API: Instantiate, set options, and show</caption>
         * <description>When using an <code class="prettify">&lt;a&gt;</code>, specify the jQuery selector for the element using <code>href</code>. Markup is optional since options are specified as data attributes.</description>
         * &lt;a 
         *     href=&quot;#modal&quot;
         *     data-toggle=&quot;modal&quot;
         *     data-heading=&quot;Test Modal&quot;
         *     data-content=&quot;&amp;lt;p&amp;gt;Test content&amp;lt;/p&amp;gt;&quot;
         *     data-buttons=&#x27;[{ &quot;label&quot;: &quot;Close&quot;, &quot;click&quot;: &quot;close&quot; }]&#x27;
         * &gt;Show Modal&lt;/a&gt;
         *
         * @example
         * <caption>Data API: Instantiate, load content asynchronously, and show</caption>
         * <description>When loading content asynchronously, regardless of what tag is used, specify the jQuery selector for the element using <code>data-target</code> and the URL of the content to load with <code>href</code>.</description>
         * &lt;button
         *     data-target="#myModal"
         *     data-toggle=&quot;modal&quot;
         *     href=&quot;content.html&quot;
         * &gt;Show Modal&lt;/button&gt;
         *
         * @example
         * <caption>Markup</caption>
         * &lt;h2 class=&quot;line&quot;&gt;Example&lt;/h2&gt;
         *     &lt;a href=&quot;#myModal&quot; class=&quot;button&quot; data-toggle=&quot;modal&quot;&gt;Show Modal&lt;/a&gt;
         *     &lt;div id=&quot;myModal&quot; class=&quot;modal&quot;&gt;
         *         &lt;div class=&quot;modal-header&quot;&gt;
         *         &lt;h2&gt;A Sample Modal&lt;/h2&gt;
         *         &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;modal&quot;&gt;&amp;times;&lt;/button&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-body&quot;&gt;
         *         &lt;p&gt;Some sample content.&lt;/p&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-footer&quot;&gt;
         *         &lt;button data-dismiss=&quot;modal&quot;&gt;Ok&lt;/button&gt;
         *     &lt;/div&gt;
         * &lt;/div&gt;
         *
         * @example
         * <caption>Markup with &lt;form&gt; tag</caption>
         * <description>Modals can be created from the <code class="prettify">&lt;form&gt;</code> tag as well. Make sure to set <code class="prettify">type="button"</code> on buttons that should not perform a submit.</description>
         * &lt;form id=&quot;myModal&quot; class=&quot;modal&quot; action="/users" method="post"&gt;
         *     &lt;div class=&quot;modal-header&quot;&gt;
         *         &lt;h2&gt;Create User&lt;/h2&gt;
         *             &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;modal&quot;&gt;&amp;times;&lt;/button&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-body&quot;&gt;
         *         &lt;label for=&quot;name&quot;&gt;Name&lt;/label&gt;&lt;input id=&quot;name&quot; name=&quot;name&quot; type=&quot;text&quot;&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-footer&quot;&gt;
         *         &lt;button type="button" data-dismiss=&quot;modal&quot;&gt;Cancel&lt;/button&gt;
         *         &lt;button type="submit"&gt;Submit&lt;/button&gt;
         *     &lt;/div&gt;
         * &lt;/form&gt;
         * 
         * 
         * @desc Creates a new modal dialog
         * @constructs
         * @param {Object} options Component options
         * @param {Mixed} options.element jQuery selector or DOM element to use for dialog
         * @param {String} options.header Title of the modal dialog (HTML)
         * @param {String} options.content Title of the modal dialog (HTML)
         * @param {String} [options.type=default] Type of dialog to display. One of default, error, notice, success, help, or info
         * @param {Array} [options.buttons] Array of button descriptors
         * @param {String} [options.buttons.label] Button label (HTML)
         * @param {String} [options.buttons.className] CSS class name to apply to the button
         * @param {Mixed} [options.buttons.click] Click handler function or string 'hide' to hide the dialog
         * @param {String} [options.remote] URL to asynchronously load content from the first time the modal is shown
         * @param {Mixed} [options.backdrop=static] False to not display transparent underlay, True to display and close when clicked, 'static' to display and not close when clicked
         * @param {Mixed} [options.visible=true] True to display immediately, False to defer display until show() called
         * 
         */
        construct: function (options) {
            // @deprecated, rather the template engine should be agonistic
            // Render template, if necessary
            // disabled for now
            if (this.$element.children().length === 0) {
                this.$element.html(CUI.Templates['modal']($.extend({}, this.options, { buttons: '' })));
            }

            // @deprecated adding a styling class blocks the reusability
            // of the modal's javascript. This will be removed in future
            // add styling
            this.$element.addClass('modal');

            // modal parts
            this.header = this.$element.find('.modal-header');
            this.body = this.$element.find('.modal-body');
            this.footer = this.$element.find('.modal-footer');

            // previous focus element
            this._previousFocus = $();

            // creates a backdrop object
            // but it does not attach it to the document
            this.backdrop = $('<div/>', {
                'class': 'modal-backdrop',
                'style': 'display: none;'
            }).fipo('tap', 'click', function (event) {
                if (this.options.backdrop !== 'static') {
                    this.hide();
                }
            }.bind(this));

            // Fetch content asynchronously, if remote is defined
            this.body.loadWithSpinner(this.options.remote);

            this.applyOptions();

            this.$element.on('change:heading', this._setHeading.bind(this)) // @deprecated
                .on('change:header', this._setHeader.bind(this))
                .on('change:content', this._setContent.bind(this))
                .on('change:buttons', this._setFooter.bind(this))
                .on('change:type', this._setType.bind(this))
                .on('change:fullscreen', this._setFullscreen.bind(this))
                // close when a click was fired on a close trigger (e.g. button)
                .fipo('tap', 'click', '[data-dismiss="modal"]', this.hide.bind(this));

            this._makeAccessible();
        },

        defaults: {
            backdrop: 'static',
            visible: true,
            type: 'default',
            fullscreen: false,
            attachToBody: true
        },

        _types: [
            'default',
            'error',
            'notice',
            'success',
            'help',
            'info'
        ],

        applyOptions: function () {
            this._setHeader();
            this._setHeading();  // @deprecated
            this._setContent();
            this._setFooter();
            this._setType();
            this._setFullscreen();

            if (this.options.visible) {
                // Show immediately
                this.options.visible = false;
                this.show();
            }
        },

        /**
         * adds some accessibility attributes and features
         * http://www.w3.org/WAI/PF/aria-practices/#dialog_modal
         * @private
         */
        _makeAccessible: function () {
            var self = this,
                idPrefix = 'modal-header' + new Date().getTime() + '-';

            // the element has the role dialog
            this.$element.attr({
                'role': 'dialog',
                'aria-hidden': !this.options.visible,
                'aria-labelledby': idPrefix + 'label',
                'aria-describedby': idPrefix + 'message',
                'tabindex': -1
            });

            this.header.find('h2').attr({
                'id': idPrefix + 'label',
                'tabindex': 0
            });

            // Message areas have role document and tabindex="0"
            this.body.attr({
                'id': idPrefix + 'message',
                'role': 'document',
                'tabindex': 0
            });

            // keyboard handling
            this.$element.on('keydown', ':focusable', function (event) {
                // enables keyboard support

                var elem = $(event.currentTarget),
                    tabbables = self.$element.find(':tabbable'),
                    focusElem;

                switch (event.which) {
                    case 9: //tab
                        if (event.shiftKey && event.currentTarget === tabbables[0]) {
                            // in case it is the first one, we switch to the last one
                            focusElem = tabbables.last();
                        } else if (!event.shiftKey && event.currentTarget === tabbables[tabbables.length-1]) {
                            // in case it is the last one, we switch to the first one
                            focusElem = tabbables.first();
                        }
                        break;
                }

                if (focusElem) { // if a key matched then we set the currently focused element
                    event.preventDefault();
                    focusElem.trigger('focus');
                }
            });
        },

        /**
         * sets the type of the modal
         * @private
         */
        _setType: function () {
            if (!this.options.type) {
                return;
            }

            // Remove old type
            this.$element.removeClass(this._types.join(' '));

            // Add new type
            if (this.options.type !== 'default') {
                this.$element.addClass(this.options.type);
            }
        },

        /**
         * sets the header of the modal
         * @private
         */
        _setHeader: function () {
            if (!this.options.header) {
                return;
            }

            this.header.find('h2').html(this.options.header);
        },

        /**
         * @deprecated rather use #_setHeader
         * @private
         */
        _setHeading: function () {
            if (!this.options.heading) {
                return;
            }

            this.options.header = this.options.heading;
            this._setHeader.apply(this, arguments);
        },

        /**
         * sets the content of the modal body
         * @private
         */
        _setContent: function () {
            if (!this.options.content) {
                return;
            }

            this.body.html(this.options.content);
        },

        /**
         * sets the buttons into the footer from the config
         * @private
         */
        _setFooter: function () {
            if (!$.isArray(this.options.buttons)) {
                return;
            }

            var self = this;

            // remove existing buttons
            this.footer.empty();

            $.each(this.options.buttons, function(idx, button) {
                // Create an anchor if href is provided
                var btn = button.href ? $('<a/>', {
                        'class': 'button'
                    }) : $('<button/>', {
                        'type': 'button'
                    });

                // Add label
                btn.html(button.label);

                // attach event handler
                if (button.click === 'hide') {
                    btn.attr('data-dismiss', 'modal');
                } else if ($.isFunction(button.click)) {
                    btn.fipo('tap', 'click', button.click.bind(self, {
                        dialog: self
                    }));
                }

                if (button.href) {
                    btn.attr('href', button.href);
                }

                if (button.className) {
                    btn.addClass(button.className);
                }

                self.footer.append(btn);
            });
        },

        /**
         * sets the fullscreen css class
         * @private
         */
        _setFullscreen: function () {
            if (this.options.fullscreen) {
                this.$element.addClass('fullscreen');
            } else {
                this.$element.removeClass('fullscreen');
            }
        },

        /**
         * @private
         * @event beforeshow
         */
        _show: function () {
            var body = $('body'),
                tabcapture,
                self = this;

            // ARIA: http://www.w3.org/WAI/PF/aria-practices/#dialog_modal
            // When the dialog is closed or cancelled focus should 
            // return to the element in the application which had focus 
            // before the dialog is invoked
            this._previousFocus = $(':focus'); //save previously focused element
            this._previousFocus.trigger('blur');

            body.addClass('modal-open');

            // center before showing
            this.center();

            // fire event before showing the modal
            this.$element.trigger('beforeshow');

            this._toggleBackdrop(true);

            // Move to the bottom of body so we're outside of any relative/absolute context
            // This allows us to know we'll always float above the backdrop
            if (this.options.attachToBody) {
                if (this.$element.parent('body').length === 0) {
                    this.$element.appendTo(body);
                }
                // ARIA
                // Hide sibling elements from assistive technologies, 
                // but first store the state of any siblings already have the aria-hidden attribute 
                this.$element.siblings('[aria-hidden]').each(function(index, element) {
                    $(element).data('aria-hidden', $(element).attr('aria-hidden'));
                });            
                this.$element.siblings().not('script, link, style').attr('aria-hidden', this.options.visible);
            }

            this.$element.attr('aria-hidden', !this.options.visible);

            // fadeIn
            this.$element.fadeIn();

            // When a modal dialog opens focus goes to the first focusable item in the dialog
            this.$element.find(':tabbable:not(.modal-header .close):first').focus();
            
            // add tab-focusable divs to capture and forward focus to the modal dialog when page regains focus
            tabcapture = $('<div class="cui-modal-tabcapture" tabindex="0"/>');
            tabcapture.on('focus.modal-tabcapture', function(event) {
                var tabbables = self.$element.find(':tabbable'),
                    tabcaptures = $('body > .cui-modal-tabcapture'),
                    lasttabcapture  = tabcaptures.last(),
                    focusElem;
                
                if (event.currentTarget === lasttabcapture[0]) {
                    focusElem = tabbables.filter(':not(.modal-header .close):last');
                } else {
                    focusElem = tabbables.filter(':not(.modal-header .close):first');
                }

                if (focusElem.length === 0) {
                    focusElem = self.$element;
                }

                focusElem.trigger('focus');
            })
            .prependTo(body)
            .clone(true)
            .appendTo(body);

            // add escape handler
            $(document).on('keydown.modal-escape', this._escapeKeyHandler.bind(this));

            return this;
        },

        /**
         * @private
         * @event beforehide
         */
        _hide: function () {
            $('body').removeClass('modal-open')
                .find('.cui-modal-tabcapture').off('focus.modal-tabcapture').remove();

            // remove escape handler
            $(document).off('keydown.modal-escape');

            // fire event before showing the modal
            this.$element.trigger('beforehide');

            this._toggleBackdrop(false);

            this.$element.attr('aria-hidden', !this.options.visible);
            
            this.$element.siblings()
                .removeAttr('aria-hidden')
                .filter(':data("aria-hidden")')
                .each(function(index, element) {
                    $(element).attr('aria-hidden', $(element).data('aria-hidden'))
                        .removeData('aria-hidden');
                });

            // fadeOut
            this.$element.fadeOut().trigger('blur');

            // ARIA: http://www.w3.org/WAI/PF/aria-practices/#dialog_modal
            // When the dialog is closed or cancelled focus should 
            // return to the element in the application which had focus 
            // before the dialog is invoked
            this._previousFocus.trigger('focus');

            return this;
        },

        /**
         * centers the modal in the middle of the screen
         * @returns {CUI.Modal} this, chainable
         */
        center: function () {
            var width = this.$element.outerWidth(),
                height = this.$element.outerHeight();

            this.$element.css({
                'margin-left': - (width / 2),
                'margin-top': - (height / 2)
            });
        },

        /**
         * toggles back drop
         * @private
         * @param  {Boolean} [show] true/false to force state
         */
        _toggleBackdrop: function (show) {
            if (!this.options.backdrop) {
                return;
            }

            var body = $('body');

            if ((show || this.backdrop.is(':hidden')) && show !== false) {
                this.backdrop.appendTo(body).fadeIn();
            }
            else {
                this.backdrop.fadeOut(function () {
                    $(this).detach();
                });
            }
        },

        /**
         * handler to close the dialog on escape key
         * @private
         */
        _escapeKeyHandler: function (event) {
            if (event.which === 27) {
                this.hide();
            }

        }
    });

    CUI.util.plugClass(CUI.Modal);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (event) {
            // initialize the modal dialogs
            $('[data-init~=modal]', event.target).modal();
        });


        // @deprecated
        // this differs from other components
        // rather in future we use data-init~="modal-trigger" to intialize a trigger
        // and require data-init~="modal" on the modal to indicate it is a modal
        $(document).fipo('tap.modal.data-api', 'click.modal.data-api', '[data-toggle="modal"]', function (e) {
            var $trigger = $(this);

            // Get the target from data attributes
            var $target = CUI.util.getDataTarget($trigger);

            // Pass configuration based on data attributes in the triggering link
            var href = $trigger.attr('href');
            var options = $.extend({ remote: !/#/.test(href) && href }, $target.data(), $trigger.data());

            // Parse buttons
            if (typeof options.buttons === 'string') {
                options.buttons = JSON.parse(options.buttons);
            }

            // If a modal already exists, show it
            var instance = $target.data('modal');
            var show = true;
            if (instance && instance.get('visible'))
            show = false;

            // Apply the options from the data attributes of the trigger
            // When the dialog is closed, focus on the button that triggered its display
            $target.modal(options);

            // Perform visibility toggle if we're not creating a new instance
            if (instance)
            $target.data('modal').set({ visible: show });

            // Stop links from navigating
            e.preventDefault();
        }).finger('click.modal.data-api', '[data-toggle="modal"]', false);
    }

}(jQuery, this));
