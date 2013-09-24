(function($) {
  CUI.NumberInput = new Class(/** @lends CUI.NumberInput# */{
    toString: 'NumberInput',
    extend: CUI.Widget,

    /**
      @extends CUI.Widget
      @classdesc A number input widget with increment and decrement buttons.

      @example
      <caption>Instantiate with Class</caption>
      &lt;div id=&quot;jsNumberInput&quot;&gt;
        &lt;button type=&quot;button&quot;&gt;decrement&lt;/button&gt;
        &lt;input type=&#039;text&#039;&gt;
        &lt;button type=&quot;button&quot;&gt;increment&lt;/button&gt;
      &lt;/div&gt;

      var numberInput = new CUI.NumberInput({
        element: '#jsNumberInput'
      });
      
      @example
      <caption>Instantiate with jQuery</caption>
      &lt;div id=&quot;jsNumberInput&quot;&gt;
        &lt;button type=&quot;button&quot;&gt;decrement&lt;/button&gt;
        &lt;input type=&#039;text&#039;&gt;
        &lt;button type=&quot;button&quot;&gt;increment&lt;/button&gt;
      &lt;/div&gt;

      $('#jsNumberInput').numberInput();
      
      @example
      <caption>Markup</caption>
      &lt;!-- Standard Number Input --&gt;                            
      &lt;div class=&quot;numberinput&quot; data-init=&quot;numberinput&quot;&gt;
          &lt;button type=&quot;button&quot; class=&#039;decrement&#039;&gt;decrement&lt;/button&gt;
          &lt;input type=&#039;text&#039;&gt;
          &lt;button type=&quot;button&quot; class=&#039;increment&#039;&gt;increment&lt;/button&gt;
      &lt;/div&gt;

      @desc Creates a Number Input object
      @constructs
      @param {Object} options Component options
      @param {numberic} [options.min=NaN] (Optional) Minimum value allowed for input.
      @param {numberic} [options.max=NaN] (Optional) Maximum value allowed for input.
      @param {numberic} [options.step=1] Amount increment/decrement for input.
      @param {boolean} [options.hasError=false] Set the error state of the widget.
      @param {boolean} [options.disabled=false] Set the disabled state of the widget.
      
    */

    construct: function(options) {

      this._fixMarkup();
      this._setListeners();
      this._setAttributes();
      
    },
    
    defaults: {
      max: null,
      min: null,
      step: 1,
      hasError: false,
      disabled: false
    },

    /**
      Increments value by step amount
    */
    increment: function () {
      if (this._isNumber()) {
        var value = this.getValue();
        value += this.getStep();
        value = value > this.getMax() ? this.getMax() : value;
        this.setValue(value);
      }
    },

    /**
      Decrements value by step amount
    */
    decrement: function () {
      var value =  this.getValue();
      value -= this.getStep();
      value = value < this.getMin() ? this.getMin() : value;
      this.setValue(value);
    },

    /**
      Sets the value, which triggers the change event.  Note that value will be 
      limited to the range defined by the min and max properties. 
      @param value {numberic} The input value to set.
    */
    setValue: function(value) {
      this.$input.val(value);
      this.$input.trigger('change');
    },

    /**
      Sets the minimum value allowed. 
      @param value {numberic} The min value to set.
    */
    setMin: function(value) {
      this.set('min', value);
    },

    /**
      Sets the maximum value allowed. 
      @param value {numberic} The max value to set.
    */
    setMax: function(value) {
      this.set('max', value);
    },


    /**
      Sets the step value for increment and decrement. 
      @param value {numberic} The step value to set.
    */
    setStep: function(value) {
      this.set('step', value);
    },

    /**
      @return The current input value.
    */
    getValue: function() {
      return parseFloat(this.$input.val());
    },

    /**
      @return The minimum input value allowed.
    */
    getMin: function() {
      return parseFloat(this.options.min);
    },

    /**
      @return The maximum input value allowed.
    */
    getMax: function() {
      return parseFloat(this.options.max);
    },

    /**
      @return The current increment/decrement step amount .
    */
    getStep: function() {
      return parseFloat(this.options.step);
    }, 

    /** @ignore */
    _fixMarkup: function() {
      // get the input, fix it if it's number
      this.$input = this.$element.find('input').not("[type=hidden]");
      if (this.$input.attr('type') != 'text') {
        this._switchInputTypeToText(this.$input);
      }

      var buttons = this.$element.find('button');

      if (buttons && buttons.length == 2) {
        this.$decrementElement = $(buttons[0]);
        this.$incrementElement = $(buttons[1]);
      } else {
        // create the right amount of buttons 
        // and remove any weird buttons
        $.each(buttons, function(index) {
          $(buttons[index]).detach();
        });
        this.$decrementElement = $("<button type=\"button\">");
        this.$incrementElement = $("<button type=\"button\">");
        this.$input.before(this.$decrementElement);
        this.$input.after(this.$incrementElement);
      }
      // fix buttons just how we like them
      this.$decrementElement.addClass("decrement").html("decrement");
      this.$incrementElement.addClass("increment").html('increment');
    }, 

    /** @ignore */
    _setListeners: function() {

      this.$input.on('change', function() {
        this._checkMinMaxViolation();
        this._adjustValueLimitedToRange();
      }.bind(this));

      this.on('beforeChange:step', function(event) {
        this._optionBeforeChangeHandler(event);
      }.bind(this));

      this.on('beforeChange:min', function(event) {
        this._optionBeforeChangeHandler(event);
      }.bind(this));

      this.on('beforeChange:max', function(event) {
        this._optionBeforeChangeHandler(event);
      }.bind(this));

      this.on('change:disabled', function(event) {
        this._toggleDisabled();
      }.bind(this));

      this.on('change:hasError', function(event) {
        this._toggleError();
      }.bind(this));

      this.$incrementElement.on('click', function () {
        this.increment();
      }.bind(this));

      this.$decrementElement.on('click', function (event) {
        this.decrement();
      }.bind(this));

      // adding fipo seems to break tests right now
      // this.$incrementElement.fipo('tap', 'click', function (event) {
      //   event.preventDefault();
      //   this.increment();
      // }.bind(this));

      // this.$decrementElement.fipo('tap', 'click', function (event) {
      //   event.preventDefault();
      //   this.decrement();
      // }.bind(this));

    },

    /** @ignore */
    _setAttributes: function() {

      this.$element.addClass('numberinput');
      
      if (this.$input.attr('max')) {
        this.setMax(this.$input.attr('max'));
      }

      if (this.$input.attr('min')) {
        this.setMin(this.$input.attr('min'));
      }

      if (this.$input.attr('step')) {
        this.setStep(this.$input.attr('step'));
      }

      if (this.$element.attr("error")) {
        this.options.hasError = true;
      }

      this.setStep(this.options.step || CUI.Datepicker.step);

      this.setValue(this.$input.val() || 0);
      
      if (this.$element.attr('disabled') || this.$element.attr('data-disabled') ) {
        this._toggleDisabled();
      }

      if (this.$element.hasClass('error') || this.$element.attr('data-error') ) {
        this.set('hasError', true);
      }
    },

    /** @ignore */
    _adjustValueLimitedToRange: function() {
      var value = this.getValue();
      if (isNaN(value)) {
        // console.error("CUI.NumberInput value set to NaN");
      } else {
        if (value > this.getMax()) {
          value = this.getMax();
        } else if (value < this.getMin()) {
          value = this.getMin();
        }
      }
      this.$input.val(value);
    },

    /** @ignore */
    _checkMinMaxViolation: function() {
      
      if (this._isNumber()) {
        this.$incrementElement.removeAttr('disabled');
        this.$decrementElement.removeAttr('disabled');

        if (this.options.max && this.getValue() >= this.getMax()) {
          this.$incrementElement.attr('disabled', 'disabled');
        } else if (this.options.min && this.getValue() <= this.getMin()) {
          this.$decrementElement.attr('disabled', 'disabled');
        }
      }
    },

   /** @ignore */
    _switchInputTypeToText: function($input) {
        var convertedInput = $input.detach().attr('type', 'text');
        this.$element.prepend(convertedInput);
    },

    /** @ignore */
    _isNumber: function () {
      return !isNaN(this.$input.val());
    },

    /** @ignore */
    _optionBeforeChangeHandler: function(event) {
      if (isNaN(parseFloat(event.value))) {
        // console.error('CUI.NumberInput cannot set option \'' + event.option + '\' to NaN value');
        event.preventDefault();
      }
    },

    /** @ignore */
    _toggleDisabled: function() {
      if (this.options.disabled) {
        this.$incrementElement.attr('disabled', 'disabled');
        this.$decrementElement.attr('disabled', 'disabled');
        this.$input.attr('disabled', 'disabled');
      } else {
        this.$incrementElement.removeAttr('disabled');
        this.$decrementElement.removeAttr('disabled');
        this.$input.removeAttr('disabled');
      }
    },

    /** @ignore */
    _toggleError: function() {
      if (this.options.hasError) {
        this.$element.addClass('error');
      } else {
        this.$element.removeClass('error');
      }
    }

  });

  CUI.util.plugClass(CUI.NumberInput);
  
  // Data API
  $(document).on("cui-contentloaded.data-api", function(e) {
    $("[data-init~=numberinput]", e.target).numberInput();
  });

}(window.jQuery));