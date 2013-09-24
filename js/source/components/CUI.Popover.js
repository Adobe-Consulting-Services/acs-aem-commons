(function($) {
  var uuid = 0;

  CUI.Popover = new Class(/** @lends CUI.Popover# */{
    toString: 'Popover',
    extend: CUI.Widget,

    /**
      @extends CUI.Widget
      @classdesc A box which points at an element

      @desc Creates a new popover
      @constructs

      @param {Object} options                               Component options
      @param {String} options.content                       Content of the popover (HTML)
    */
    construct: function(options) {
      // Add CSS class to give styling
      this.$element.addClass('popover');

      // Listen to changes to configuration
      this.$element.on('change:content', this._setContent.bind(this));
      this.$element.on('change:pointAt', this._setPointAt.bind(this));
      this.$element.on('change:pointFrom', this._setPointFrom.bind(this));

      // Render template, if necessary
      if (this.$element.html() === '') {
        this.applyOptions();
      }
      else {
        this.applyOptions(true);
      }

      this.hide();
      this.uuid = (uuid += 1);
      this.cachedPointFrom = this.options.pointFrom;
    },

    defaults: {
      pointFrom: 'bottom',
      alignFrom: 'left',
      pointAt: $('body'),
      arrowPos: '',
      visible: true
    },

    _directions: [
      'top',
      'bottom',
      'right',
      'left'
    ],

    applyOptions: function(partial) {
      if (!partial) {
        this._setContent();
      }
      this._setPointAt();
      this._setPointFrom();
    },

    setPosition: function(position) {
      // Reset point from
      this._doSetPointFrom(this.options.pointFrom);

      // move element to under body for absolute positioning
      if (this.$element.parent().get(0) !== $('body').get(0)) {
        this.$element.detach().appendTo($('body'));
      }
      
      var screenWidth = $(window).width();
      var screenHeight = $(window).height();

      var pointFrom = this.options.pointFrom;
      var top = position[1];
      var left = position[0];

      var width = this.$element.outerWidth();
      var height = this.$element.outerHeight();

      var arrowHeight = Math.round((this.$element.outerWidth() - this.$element.width())/1.5);
      
      // Switch direction if we fall off screen
      if (pointFrom === 'top' && top - height - arrowHeight < 0) {
        pointFrom = 'bottom';
        this._doSetPointFrom('bottom');
      }
      if (pointFrom === 'bottom' && top + height + arrowHeight > screenHeight) {
          pointFrom = 'top';
          this._doSetPointFrom('top');
      }

      // Base on pointFrom
      if (pointFrom === 'bottom' || pointFrom === 'top') {
        left -= width/2;
      }

      if (pointFrom === 'bottom') {
        top += arrowHeight; // TBD find out the size of 1rem
      } else if (pointFrom === 'top') {
        top -= height + arrowHeight; // TBD find out the size of 1rem
      }
      
      // Offset if we collide with the right side of the window
      var offset = 0;
      var leftOffset = screenWidth - (left + width);
      if (leftOffset < 0)
        offset = leftOffset;
      
      // Offset if we collide with the left side of the window
      if (left < 0)
        offset = -left;

      // Apple offset
      left += offset;
      
      // Position arrow
      if (offset < 0) {
        this.$element.addClass('arrow-pos-right');
      }
      else if (offset > 0) {
        this.$element.addClass('arrow-pos-left');
      }
      else {
        this.$element.removeClass('arrow-pos-left arrow-pos-right');
      }
      
      // Position body
      this.$element.css({
        top: top,
        left: left
      });
    },

    /** @ignore */
    _show: function() {
      this.$element.show();

      if (!this.options.preventAutoHide) {
        $('body').fipo('tap.popover-hide-'+this.uuid, 'click.popover-hide-'+this.uuid, function(e) {
          var el = this.$element.get(0);

          if (e.target !== el && !$.contains(el, e.target)) {
            this.hide();
            $('body').off('.popover-hide-'+this.uuid);
          }
        }.bind(this));
      }
    },

    /** @ignore */
    _hide: function() {
      this.$element.hide();
      $('body').off('.popover-hide-'+this.uuid);
    },

    /** @ignore */
    _setContent: function() {
      if (typeof this.options.content !== 'string') return;

      this.$element.html(this.options.content);
    },


    /** @ignore */
    _setPointAt: function() {
      /*
      * Find first absolute/relative positioned parent
      * REMARK: moved from CUI.Util, has to be implemented properly
      */
      function positionedParent (el) {
        var parent;

        el.parents().each(function() {
          var $this = $(this), position = $this.css('position');

          if (position === 'absolute' || position === 'relative') {
            parent = $this;
            return false;
          }
        });

        return parent || $('body');
      }
      var $el = $(this.options.pointAt);
      
      if ($el.length !== 1) return;

      // ensure we have the same parent so relative positioning works like a charm.
      // a sad, sad charm.
      if (this.$element.parent().get(0) !== $el.parent().get(0)) {
        this.$element.detach().insertAfter($el);
      }

      var screenPadding = 6; // Use some padding to the borders of the screen

      // we could probably use more variables here
      // - said no one ever
      var relativePosition = $el.position(),
          absolutePosition = $el.offset(),
          pointAtHeight = $el.outerHeight(),
          pointAtWidth = $el.outerWidth(),
          screenWidth = $("body").prop("scrollWidth"),
          screenHeight = $("body").prop("scrollHeight"),
          pointFrom = this.options.pointFrom,
          left = relativePosition.left,
          top = relativePosition.top,
          absTopDiff = absolutePosition.top - parseInt($el.css("margin-top"), 10) - top, // Fix jQuery as it does different margin calculations on offset() and position()
          absLeftDiff = absolutePosition.left - parseInt($el.css("margin-left"), 10) - left, // Fix jQuery as it does different margin calculations on offset() and position()
          width = this.$element.outerWidth(true),
          height = this.$element.outerHeight(),
          parentWidth = positionedParent(this.$element).width(),
          parentPadding = parseFloat(this.$element.parent().css('padding-right')),
          arrowHeight = Math.round((this.$element.outerWidth() - this.$element.innerWidth()) / 1.45),
          // The arrow height calculation just approximates the size, as we can not get it from the element
          right, offset = 0, offsetTop = 0;

      // Switch directions if we fall off screen
      if (pointFrom === 'top' && absolutePosition.top - height- arrowHeight < 0) {
        pointFrom = 'bottom';
      }

      if (pointFrom === 'bottom' && absolutePosition.top + height + arrowHeight + pointAtHeight > screenHeight) {
        pointFrom = 'top';
      }
      if (pointFrom === 'left' && absolutePosition.left - width - arrowHeight < 0) {
          pointFrom = 'right';
        }

      if (pointFrom === 'right' && absolutePosition.left + width + arrowHeight + pointAtWidth > screenWidth) {
          pointFrom = 'left';
      }

      // set our point direction
      this._doSetPointFrom(pointFrom);

      if (pointFrom === 'bottom' || pointFrom === 'top') {
        left -= (width/2 - pointAtWidth/2); // account for the width of the popover, as well as the width of the pointed-at element

        if (pointFrom === 'bottom') {
          top += (pointAtHeight + arrowHeight);
        } else if (pointFrom === 'top') {
          top -= (height + arrowHeight);
        }
      }

      if (pointFrom === 'left' || pointFrom === 'right') {
        top -= (height/2 - pointAtHeight/2);

        if (pointFrom === 'left') {
          left -= (width + arrowHeight);
        } else if (pointFrom === 'right') {
          left += (pointAtWidth + arrowHeight);
        }
      }
      
      // for right-aligned popovers, we need to take into account the positioned parent width, as well as the padding
      right = parentWidth - left - width + parentPadding*2;

      if (absLeftDiff + left - screenPadding < 0) {
        offset = -(absLeftDiff + left) + screenPadding;
      } else if (absLeftDiff + left + width + screenPadding > screenWidth) {
        offset = screenWidth - (absLeftDiff + left + width) - screenPadding;
      }

      // adjust if we would be offscreen
      left += offset;
      right -= offset;

      if (this.options.alignFrom === 'right') {
        this.$element.css({
          top: top,
          left: 'auto',
          right: right
        });
      } else {
        this.$element.css({
          top: top,
          left: left,
          right: 'auto'
        });
      }

      var set_arrows = false;
      // Position arrow
      if (pointFrom === 'top' || pointFrom === 'bottom') {
        if (offset < 0 || this.options.arrowPos === 'right') {
          this.$element.addClass('arrow-pos-right');
          set_arrows = true;
        } else if (offset > 0 || this.options.arrowPos === 'left') {
          this.$element.addClass('arrow-pos-left');
          set_arrows = true;
        }
      }
      
      if (!set_arrows) {
        this.$element.removeClass('arrow-pos-left arrow-pos-right');
      }
    },

    _doSetPointFrom: function(pointFrom) {
      // Remove old direction
      this.$element.removeClass('arrow-top arrow-bottom arrow-right arrow-left');

      if (pointFrom === 'bottom')
        this.$element.addClass('arrow-top');
      else if (pointFrom === 'top')
        this.$element.addClass('arrow-bottom');
      else if (pointFrom === 'left')
        this.$element.addClass('arrow-right');
      else if (pointFrom === 'right')
        this.$element.addClass('arrow-left');
    },

    /** @ignore */
    _setPointFrom: function() {
      var pointFrom = this.options.pointFrom;
      if (this._directions.indexOf(pointFrom) === -1)
        return;

      if (this.cachedPointFrom !== pointFrom) {
        this._doSetPointFrom(pointFrom);
        this.cachedPointFrom = pointFrom;
      }
      
    }
  });

  CUI.util.plugClass(CUI.Popover);

  $(function() {
    $('body').fipo('tap.popover.data-api', 'click.popover.data-api', '[data-toggle="popover"]', function (e) {
      var $trigger = $(this),
          $target = CUI.util.getDataTarget($trigger);

      // if data target is not defined try to find the popover as a sibling
      $target = $target && $target.length > 0 ? $target : $trigger.next('.popover');

      var popover = $target.popover($.extend({pointAt: $trigger}, $target.data(), $trigger.data())).data('popover');

      popover.toggleVisibility();
    }).on('click.popover.data-api', '[data-toggle="popover"]', false);
  });
}(window.jQuery));
