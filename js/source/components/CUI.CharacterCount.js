(function($) {
  CUI.CharacterCount = new Class(/** @lends CUI.CharacterCount# */{
    toString: 'CharacterCount',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc Give visual feedback of the maximum length for textfields/textarea to the user.
      <p>This widget will not restrict the
      user to the max length given: The user can enter as much text
      as he/she wants and will only get visual feedback.</p>
      <p>For textareas some browsers count newline characters differently: While they count as 2 chars in the browsers builtin maxlength support,
      they only count as 1 char in this widget.</p>
      <p>
      <textarea maxlength="50" rows="10" cols="20" data-init="character-count"></textarea>
      </p>
      
      @example
<caption>Instantiate with Class</caption>
var alert = new CUI.CharacterCount({
  element: '#myTextField',
  maxlength: 50
});

      @example
<caption>Instantiate with jQuery</caption>
$('#myTextField').characterCount({maxlength: 50});

      @example
<caption>Markup</caption>
&lt;input type="text" maxlength="50" data-init="character-count"&gt;

      @desc Create a character count for a textfield or textarea.
      @constructs

      @param {Object} options                               Component options
      @param {String} [options.maxlength]                   Maximum length for the textfield/textarea (will be read from markup if given)
    */
    construct: function(options) {
      if (this.$element.attr("maxlength")) this.options.maxlength = this.$element.attr("maxlength");
      this.$element.removeAttr("maxlength"); // Remove so that we can do our own error handling

      this.countElement = $("<span>").addClass("character-count");
      
      if (!this.$element.is("input")) {
        this.container = $("<div>").addClass("character-count-container");
        this.$element.wrapAll(this.container);
      }
            
      this.$element.after(this.countElement);
      
      this.$element.on("input", this._render.bind(this));
      this.$element.on("change:maxlength", this._render.bind(this));
      
      this._render();
      
    },
    
    defaults: {
      maxlength: null
    },
    
    /**
     @return {boolean} True, if the current textfield/textarea content is too long (greater than maxlength).
    */
    isTooLong: function() {
      var isFormField = this.$element.is("input,textarea");
      // In case of form field we use the value to count characters, for normal HTML elements we use the inner text
      var length = (isFormField) ? this.$element.val().length : this.$element.text().length;
      var tooLong = (this.options.maxlength) ? (length > this.options.maxlength) : false;
      return tooLong;      
    },
    
    _render: function() {
      var isFormField = this.$element.is("input,textarea");
      // In case of form field we use the value to count characters, for normal HTML elements we use the inner text
      var length = (isFormField) ? this.$element.val().length : this.$element.text().length;
      var tooLong = this.isTooLong();
      this.$element.toggleClass("error", tooLong);
      this.countElement.toggleClass("negative", tooLong);
      this.countElement.text((this.options.maxlength) ? (this.options.maxlength - length) : length);
    }
  });

  CUI.util.plugClass(CUI.CharacterCount);
  
  // Data API
  $(document).on("cui-contentloaded.data-api", function(e) {
    $("[data-init~=character-count]", e.target).characterCount();
  });
}(window.jQuery));


