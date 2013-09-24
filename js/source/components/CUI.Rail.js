(function ($, window, undefined) {
    CUI.Rail = new Class(/** @lends CUI.Rail# */{
        toString: 'Rail',
        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc this widget enables rail features
         * <div id="myRail" class="rail" data-init="rail" data-theme="dark">
         *     <div class="wrap">
         *     Place your content here.
         *     </div>
         * </div>
         * @example
         * <caption>Instantiate with Class</caption>
         * var rail = new CUI.Rail({
         *     element: '#myRail'
         * });
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#myRail').rail();
         *
         * @example
         * <caption>Markup</caption>
         * &lt;div id=&quot;myRail&quot; class=&quot;rail&quot; data-init=&quot;rail&quot; data-theme=&quot;dark&quot;&gt;
         *     &lt;div class=&quot;wrap&quot;&gt;
         *     Place your content here.
         *     &lt;/div&gt;
         * &lt;/div&gt;
         * 
         * @constructs
         * @param {Object} options
         */
        construct: function (options) {

            // add default theme to rail
            // this is a temporary solution because it expects to markup to already have the attribute
            if (!this.$element.attr('data-theme')) {
                this.$element.attr('data-theme', 'dark');
            }

            this.applyOptions();
        },

        /**
         * sets all options
         */
        applyOptions: function () {
            // foldable sections
            this._initFoldable();

            // rail switcher
            if (this.$element.find('.rail-switch').length > 0) {
                this._initRailSwitcher();
            }

            // accessibility
            this._makeAccessible();
        },

        /**
         * @private
         */
        _show: function () {
            this.$element.removeClass('closed');
        },

        /**
         * @private
         */
        _hide: function () {
            this.$element.addClass('closed');
        },

        /**
         * initialize the functionality for foldable sections
         * @private
         */
        _initFoldable: function () {
            var openClass = 'open',
                idPrefix = 'rail-section-foldable-'; // + section anchor

            // add accessibility
            this.$element.find('section.foldable').each(function (i, e) {
                var section = $(e),
                    head = section.find('.heading'),
                    fold = section.find('.fold'),
                    curId = idPrefix + (head.attr('href') || (' ' + new Date().getTime() + '-' + i)).substring(1),
                    isOpen = section.hasClass(openClass);

                section.attr('role', 'presentation');
                head.attr({
                    'role': 'button',
                    'id': head.attr('id') || curId,
                    'aria-controls': fold.attr('id') || curId + '-fold',
                    'tabindex': 0,
                    'aria-expanded': isOpen
                });
                fold.attr({
                    'id': fold.attr('id') || curId + '-fold',
                    'aria-labelledby': head.attr('id'),
                    'aria-hidden': !isOpen,
                    'aria-expanded': isOpen
                });
            });

            // click handler for open/close
            this.$element.fipo('tap', 'click', 'section.foldable .heading', function (event) {
                var head = $(event.currentTarget),
                    section = head.parents('.foldable:first'),
                    fold = section.find('.fold'),
                    isOpen = section.toggleClass(openClass).hasClass(openClass);

                head.attr({
                    'aria-expanded': isOpen
                });

                fold.attr({
                    'aria-hidden': !isOpen,
                    'aria-expanded': isOpen
                });

                // workaround to make sure that VoiceOver announces the expanded items.
                if (isOpen && event.type === 'tap') {
                    fold.attr('tabindex', '-1').trigger('focus');
                    head.trigger('focus');
                    fold.removeAttr('tabindex');
                }
            }).on('keydown', 'section.foldable .heading:not(button):not(a[href])', function (event) {
                var head = $(event.currentTarget);
                switch (event.which) {
                    case 13: //enter
                    case 32: //space
                        head.trigger('click');
                        break;
                }
            });

        }, //_initFoldable

        /**
         * initializes the rail switcher
         * @private
         */
        _initRailSwitcher: function () {
            var activeClass = 'active',
                focusClass = 'focus',
                idPrefix = 'rail-switcher-view-', // + myviewname 
                tabs = this.$element.find('.rail-switch a'),
                views = this.$element.find('.rail-view');

            // legacy support (Coral 1.x)
            // instead of data-view we are using href anchors (#)
            // though in case the anchors are not set but the data attr we port the data-attr to ids
            tabs.filter('[href="#"]').each(function (i, e) {
                var tab = $(this),
                    viewName = tab.data('view'),
                    view = views.filter('[data-view="'+ viewName +'"]');

                tab.attr('href', '#' + idPrefix + viewName);
                view.attr('id', idPrefix + viewName);
            });

            // set accessibility props
            this.$element.find('.rail-switch nav').attr({ // role of the tabs
                'role':'tablist'
            });

            tabs.attr({
                'role': 'tab',
                'aria-selected': function () {
                    return $(this).hasClass(activeClass);
                },
                'tabindex': function () {
                    return $(this).hasClass(activeClass) ? 0 : -1;
                },
                'aria-controls': function () {
                    return $(this).attr('href').substring(1);
                }
            });

            views.attr({'role': 'tabpanel',
                'aria-hidden': function () {
                    return !$(this).hasClass(activeClass);
                },
                'aria-expanded': function () {
                    return $(this).hasClass(activeClass);
                },
                'aria-labelledby': function () {
                    return $(this).attr('id');
                }
            });

            // switch handler
            this.$element.fipo('tap', 'click', '.rail-switch a', function (event) {
                var targetTab = $(event.currentTarget),
                    // view name from target
                    viewName = targetTab.attr('href').substring(1),
                    // the views are not cached to allow dynamic adding/removal
                    targetView = views.filter('#'+ viewName);

                // to avoid jumping to a hash
                event.preventDefault();

                // remove active class from all tabs/views
                views.removeClass(activeClass).attr({
                    'aria-hidden': true,
                    'aria-expanded': false
                });
                tabs.removeClass(activeClass).attr({
                    'aria-selected': false,
                    'tabindex': -1
                });

                // add active class to target tab/view
                targetTab.addClass(activeClass).attr({
                    'aria-selected': true,
                    'tabindex': 0
                });
                targetView.addClass(activeClass).attr({
                    'aria-hidden':false,
                    'aria-expanded': true
                });
            });

            this.$element.pointer('blur', '.rail-switch a', function (event) {
                $(event.currentTarget).removeClass(focusClass);
            });
        }, // _initRailSwitcher

        /**
         * adds some accessibility attributes and features
         * @private
         */
        _makeAccessible: function () {
            var focusClass = 'focus';
            // The rail is complementary content
            // See: http://www.w3.org/TR/wai-aria/roles#complementary
            this.$element.attr('role', 'complementary');

            // TODO move that into a generic file
            // init the key handling for tabs
            var tablists = this.$element.find('[role="tablist"]'),
                tabSelector = '[role="tab"]';

            tablists.on('keydown', tabSelector, function (event) {
                // enables keyboard support

                var elem = $(event.currentTarget),
                    tabs = $(event.delegateTarget).find(tabSelector),
                    focusElem = elem,
                    keymatch = true,
                    idx = tabs.index(elem);

                switch (event.which) {
                    case 33: //page up
                    case 37: //left arrow
                    case 38: //up arrow
                        focusElem = idx-1 > -1 ? tabs[idx-1] : tabs[0];
                        break;
                    case 34: //page down
                    case 39: //right arrow 
                    case 40: //down arrow
                        focusElem = idx+1 < tabs.length ? tabs[idx+1] : tabs[tabs.length-1];
                        break;
                    case 36: //home
                        focusElem = tabs[0];
                        break;
                    case 35: //end
                        focusElem = tabs[tabs.length-1];
                        break;
                    default:
                        keymatch = false;
                        break;
                }

                if (keymatch) { // if a key matched then we set the currently focused element
                    event.preventDefault();
                     // set focus class here to avoid having the focus glow with mouse click
                    $(focusElem).addClass(focusClass)
                        .trigger('focus')
                        .trigger('click');
                }
            });
        } // _makeAccessible
    });

    CUI.util.plugClass(CUI.Rail);

    // Data API
    if (CUI.options.dataAPI) {
        $(function () {
            $("[data-init~=rail]").rail();
        });
    }

}(jQuery, this));
