(function($) {
  CUI.Sticky = new Class(/** @lends CUI.Sticky# */{
    toString: 'Sticky',
    extend: CUI.Widget,

    /**
      @extends CUI.Widget
      @classdesc A sticky widget - can make every element sticky to screen
      @deprecated
        
      @desc Makes element sticky, i.e. the element does not scroll out of screen.
    */
    construct: function(options) {
        this.$element.addClass("sticky");
        this.wrapper = $("<div>").addClass("sticky-wrapper");
        this.$element.wrapAll(this.wrapper);
       
        this.wrapper = this.$element.parent();
        this.wrapper.height(this.$element.outerHeight(true));
        
        
        this.scrollingElement = this.$element.parents(".sticky-container");
        if (this.scrollingElement.length === 0) {
            this.scrollingElement = $(document);
            this.pageScroll = true;
        }
        
        this.scrollingElement.on("scroll", this._fixElementPosition.bind(this));
        $(window).on("resize", this._fixElementPosition.bind(this));
    },

    _fixElementPosition: function() {
         
         var pos = this.wrapper.offset().top;
         var scroll = (this.pageScroll) ? this.scrollingElement.scrollTop() : this.scrollingElement.offset().top;
         var startAt = this._getStickPosition();

         var left = this.wrapper.position().left;
                   
         var w = this.wrapper.width();
         if ((pos - startAt) < scroll) {
            if (!this.pageScroll) {
                var containerPosition = this.scrollingElement.position();
                startAt += containerPosition.top;
                left += containerPosition.left;
                this.$element.detach();
                this.scrollingElement.after(this.$element);
            }
            this.$element.css({
                "position": (this.pageScroll) ? "fixed" : "absolute",
                "top": startAt+"px",
                "left": left,
                "width": w+"px"
            });
         } else {
            if (!this.pageScroll) {
                this.$element.detach();
                this.wrapper.append(this.$element);
            }
            this.$element.css({
                "position": "",
                "top": "",
                "left": "",
                "width": w+"px"
            });
         }

    },

    _getStickPosition: function() {
        var etop = this.wrapper.offset().top;
        var startAt = 0;
        this.scrollingElement.find(".sticky-wrapper").each(function(index, element) {
            if ($(element).offset().top < etop) startAt += $(element).outerHeight(true);
        }.bind(this));
        return startAt;
    },
    
    scrollingElement: null,
    pageScroll: false,
    wrapper: null
  });

  CUI.util.plugClass(CUI.Sticky);

  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
      $(".sticky,[data-init=sticky]", e.target).sticky();
    });
  }
}(window.jQuery));
