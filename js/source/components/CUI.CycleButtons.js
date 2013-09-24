;(function($) {
  "use strict";

  function getNext(from) {
    var next = from.next();

    if (next.length > 0) {
      return next;
    }

    // returns the first child. i.e. rotating
    return from.prevAll().last();
  }

  CUI.CycleButtons = new Class(/** @lends CUI.CycleButtons# */{
    toString: "CycleButtons",
    extend: CUI.Widget,

    /**
      @extends CUI.Widget
      @classdesc
      A component that show the current active item. Only one item can be active at the same time.
      When clicked, the next item of the active is shown and the click is triggered at that next item instead.
      If the last item is clicked, then the first item is shown and triggered accordingly.

      @example
&lt;span class="cyclebuttons" data-init="cyclebuttons">
  &lt;button class="cyclebuttons-active icon-viewcard" type="button">Card&lt;/button>
  &lt;button class="icon-viewlist" type="button">List&lt;/button>
&lt;/span>

      @desc Creates a new instance
      @constructs

      @param {Object} options Widget options
     */
    construct: function() {
      // Currently doesn't support form submission
      // When you need it please raise the issue in the mailing first, as the feature should not be necessarily implemented in this component

      this.$element.on("click tap", function(e) {
        if (e._cycleButtons) {
          return;
        }

        e.stopPropagation();
        e.preventDefault();

        var toggle = $(this);

        if (toggle.children().length === 1) {
          return;
        }

        var from = toggle.children(".cyclebuttons-active");
        var to = getNext(from);

        from.removeClass("cyclebuttons-active");
        to.addClass("cyclebuttons-active");

        var click = $.Event("click", {
          _cycleButtons: true
        });
        to.trigger(click);
      });
    }
  });

  CUI.util.plugClass(CUI.CycleButtons);

  // Data API
  $(document).on("cui-contentloaded.data-api", function(e) {
    $("[data-init~='cyclebuttons']", e.target).cycleButtons();
  });
}(window.jQuery));
