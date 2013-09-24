(function($) {
  CUI.LabeledSlider = new Class(/** @lends CUI.LabeledSlider# */{
    toString: 'LabeledSlider',
    extend: CUI.Slider,
    
    alternating: false,
    /**
      @extends CUI.Slider
      @classdesc <p><span id="slider-label">A slider widget with labeled ticks</span></p>

        <div class="slider ticked filled label-alternating" data-init="labeled-slider">
            <fieldset>
                <legend>Slider with alternating labels<br></legend>
                <label>Minimum <input type="range" value="14" min="10" max="22" step="2"></label>
                <label>Maximum <input type="range" value="16" min="10" max="22" step="2"></label>
            </fieldset>
            <ul class="tick-labels">
              <li>First label</li>
              <li>Second label</li>
              <li>Third label</li>
              <li>Fourth label</li>
              <li>Fifth label</li>                                                            
            </ul>
        </div>

        <p>
        The labeled slider uses the same options/markup as the slider label, but with one addition: You can provide a list of labels for the
        slider's ticks. (And of course use data-init="labeled-slider"!)
        </p>
        <p><em>Please note</em> that you have to list the labels for the ticks exactly in the order and count that you configured
        your slider's ticks. If your slider has 5 ticks, provide 5 labels for it. The number of ticks depends on the step / min / max values and
        can be calculated by ceil((max - min) / step) - 1.</p>

    @example
    <caption>Slider with labeled ticks</caption>
        &lt;div class="slider ticked filled" data-init="labeled-slider"&gt;
            &lt;fieldset&gt;
                &lt;legend&gt;Slider with alternating labels&lt;br&gt;&lt;/legend&gt;
                &lt;label&gt;Minimum &lt;input type="range" value="14" min="10" max="20" step="2"&gt;&lt;/label&gt;
                &lt;label&gt;Maximum &lt;input type="range" value="16" min="10" max="20" step="2"&gt;&lt;/label&gt;
            &lt;/fieldset&gt;
            &lt;ul class="tick-labels"&gt;
              &lt;li&gt;First label&lt;/li&gt;
              &lt;li&gt;Second label&lt;/li&gt;
              &lt;li&gt;Third label&lt;/li&gt;
              &lt;li&gt;Fourth label&lt;/li&gt;
              &lt;li&gt;Fifth label&lt;/li&gt;                                                            
            &lt;/ul&gt;
        &lt;/div&gt;

    @example
    <caption>Slider with labeled ticks that alternate in two lines (note the label-alternating class)</caption>
        &lt;div class="slider ticked filled label-alternating" data-init="labeled-slider"&gt;
            &lt;fieldset&gt;
                &lt;legend&gt;Slider with alternating labels&lt;br&gt;&lt;/legend&gt;
                &lt;label&gt;Minimum &lt;input type="range" value="14" min="10" max="20" step="2"&gt;&lt;/label&gt;
                &lt;label&gt;Maximum &lt;input type="range" value="16" min="10" max="20" step="2"&gt;&lt;/label&gt;
            &lt;/fieldset&gt;
            &lt;ul class="tick-labels"&gt;
              &lt;li&gt;First label&lt;/li&gt;
              &lt;li&gt;Second label&lt;/li&gt;
              &lt;li&gt;Third label&lt;/li&gt;
              &lt;li&gt;Fourth label&lt;/li&gt;
              &lt;li&gt;Fifth label&lt;/li&gt;                                                            
            &lt;/ul&gt;
        &lt;/div&gt;

      @desc Creates a labeled slider from a div
      @constructs
      
      @param {Object}   options                               Component options
      @param {number} [options.step=1]  The steps to snap in
      @param {number} [options.min=1]   Minimum value
      @param {number} [options.max=100] Maximum value
      @param {number} [options.value=1] Starting value
      @param {number} [options.tooltips=false] Show tooltips?
      @param {String} [options.orientation=horizontal]  Either 'horizontal' or 'vertical'
      @param {boolean} [options.slide=false]    True for smooth sliding animations. Can make the slider unresponsive on some systems. 
      @param {boolean} [options.disabled=false] True for a disabled element
      @param {boolean} [options.bound=false] For multi-input sliders, indicates that the min value is bounded by the max value and the max value is bounded by the min
    **/    
    construct: function() {
      this.$element.addClass("labeled-slider");
    },
    
    _getTickLabel: function(index) {
      var el = this.$element.find("ul.tick-labels li").eq(index);
      return el.html();
    },
           
    _buildTicks: function() {
        var that = this;
        
        if (this.$element.hasClass("label-alternating")) this.alternating = true;
      
        // The ticks holder
        var $ticks = $("<div></div>").addClass('ticks');
        this.$element.prepend($ticks);

        var numberOfTicks = Math.ceil((that.options.max - that.options.min) / that.options.step) - 1;
        var trackDimensions = that.isVertical ? that.$element.height() : that.$element.width();
        var maxSize = trackDimensions / (numberOfTicks + 1);
        
        if (this.alternating) maxSize *= 2;
        for (var i = 0; i < numberOfTicks; i++) {
            var position = trackDimensions * (i + 1) / (numberOfTicks + 1);
            var tick = $("<div></div>").addClass('tick').css((that.isVertical ? 'bottom' : 'left'), position + "px");
            $ticks.append(tick);
            var className = "tick-label-" + i;
            var ticklabel = $("<div></div").addClass('tick-label ' + className);
            if (!that.isVertical) position -= maxSize / 2;
            ticklabel.css((that.isVertical ? 'bottom' : 'left'), position + "px");
            if (!that.isVertical) ticklabel.css('width', maxSize + "px");
            if (that.alternating && !that.isVertical && i % 2 === 1) {
              ticklabel.addClass('alternate');
              tick.addClass('alternate');
            }
            ticklabel.append(that._getTickLabel(i));
            $ticks.append(ticklabel);
        }
        that.$ticks = $ticks.find('.tick');
        if(that.options.filled) {
            that._coverTicks();
        }
    }
    
  });



  CUI.util.plugClass(CUI.LabeledSlider);

  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
        $(".slider[data-init~='labeled-slider']", e.target).labeledSlider();
    });
  }
}(window.jQuery));



