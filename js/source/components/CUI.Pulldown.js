(function($) {
  CUI.Pulldown = new Class(/** @lends CUI.Pulldown# */{
    toString: 'Pulldown',
    extend: CUI.Widget,

    defaults: {
        pulldownSize: 0
    },

    timeout: null,
    popoverShown: false,

    /**
      @extends CUI.Widget
      @classdesc A pulldown widget

      @param {Object}   options                               Component options
      @param {integer} [options.pulldownSize=0]               Defines the maximum number of rows to be displayed before adding a scrollbar

    */
    construct: function(options) {
        this._readDataFromMarkup();

        var $link = this.$element.find('a').first();
        var $popover = this.$element.find('.popover').first();

        $link.on("click", function(event) {
            event.preventDefault();
            this.togglePopover();
            this._keepFocus();
        }.bind(this));

        $popover.on("click", function() {
            this._keepFocus();
        }.bind(this));

        $link.on("blur", function() {
            this.timeout = setTimeout(function() {
                this.timeout = null;
                this.hidePopover();
            }.bind(this), 200);
        }.bind(this));

        $popover.fipo("tap", "click", "ul > li > a", function() {
            var self = this;
            setTimeout(function() {
                self.hidePopover();
            }, 500);
        }.bind(this));
    },

    _readDataFromMarkup: function () {
        if (this.$element.data("pulldownSize")) {
            // Force number
            this.options.pulldownSize = this.$element.data('pulldownSize') * 1;
        }
    },

    _keepFocus: function() {
        var $link = this.$element.find('a').first();

        if (!$link.is(".disabled, [disabled]")) {
            clearTimeout(this.timeout);
            this.timeout = null;
            $link.focus();
        }
    },

    togglePopover: function() {
        if (this.popoverShown) {
            this.hidePopover();
        } else if (!this.$element.find('a').first().is(".disabled, [disabled]")) {
            this.showPopover();
        }
    },

    showPopover: function() {
        this._placePopover();
        this.$element.find('.popover').show();
        this.popoverShown = true;
    },

    hidePopover: function() {
        this.$element.find('.popover').hide();
        this.popoverShown = false;
    },

    setDisabled: function(disabled) {
        if (disabled === true) {
            this.$element.find('a').first().addClass('disabled');
            // In case the popover was displayed prior to blocking the pulldown
            this.hidePopover();
        } else
            this.$element.find('a').first().removeClass('disabled');
    },

    _placePopover: function() {
        var $link = this.$element.find('a').first(),
            $popover = this.$element.find('.popover'),
            position = $link.position(),
            top,
            left,
            marginLeft;

        //default width either to:
        //first link width + 22 (22 is alignment on icon) if link is larger that popover
        //or popover width if larger

        var w = Math.max($link.width() + 22, $popover.width());

        var size = {
            width: w,
            height: $link.height()
        };

        // alignment
        if ($popover.hasClass('alignleft')) {
            top = position.top + size.height;
            left = - 30;
            marginLeft = 30 - w;
        } else { // align right
            top = position.top + size.height + 15;
            left = position.left + $link.width() - size.width + 5;
            marginLeft = size.width - 30;
        }

        $popover.css({
            top: top,
            left: left,
            width: size.width
        });

        /*$('.popover.arrow-top:before').css({
            marginLeft: marginLeft
        });*/

        var $list = $popover.find("ul").first();
        if (this.options.pulldownSize > 0) {
            var sum = 0;
            $list.find('li:lt(' + this.options.pulldownSize + ')').each(function() {
                // Need to add list item's outer height and its contained link outer height
                // as list item's outer height will just contain border height because it's hidden
                sum += $(this).outerHeight() + $(this).find("a").first().outerHeight();
            });
            $list.css("max-height", sum);
        }
    }

  });

  CUI.util.plugClass(CUI.Pulldown);

  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
        $("[data-init~=pulldown]", e.target).pulldown();
    });
  }

}(window.jQuery));
