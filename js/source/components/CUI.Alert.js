(function($) {
  CUI.Alert = new Class(/** @lends CUI.Alert# */{
    toString: 'Alert',
    extend: CUI.Widget,

    /**
      @extends CUI.Widget
      @classdesc An optionally closable alert message.

      <div class="alert error">
        <button class="close" data-dismiss="alert">&times;</button>
        <strong>ERROR</strong><div>Uh oh, something went wrong with the whozit!</div>
      </div>

      @example
<caption>Instantiate with Class</caption>
var alert = new CUI.Alert({
  element: '#myAlert',
  heading: 'ERROR',
  content: 'An error has occurred.',
  closable: true
});

// Hide the alert, change the content, then show it again
alert.hide().set({ content: 'Another error has occurred.'}).show();

// jQuery style works as well
$('#myAlert').alert('hide');

      @example
<caption>Instantiate with jQuery</caption>
$('#myAlert').alert({
  heading: 'ERROR',
  content: 'An error has occurred.',
  closable: true
});

// Hide the alert, change the content, then show it again
$('#myAlert').alert('hide').alert({ heading: 'Another error has occurred.'}).alert('show');

// A reference to the element's alert instance is stored as data-alert
var alert = $('#myAlert').data('alert');
alert.hide();

      @example
<caption>Data API: Hide alert</caption>
<description>When an element within the alert has <code><span class="atn">data-dismiss</span>=<span class="atv">"alert"</span></code>, it will hide the alert.</description>
&lt;a data-dismiss=&quot;alert&quot;&gt;Dismiss&lt;/a&gt;

      @example
<caption>Markup</caption>
&lt;div class=&quot;alert error&quot;&gt;
  &lt;button class=&quot;close&quot; data-dismiss=&quot;alert&quot;&gt;&amp;times;&lt;/button&gt;
  &lt;strong&gt;ERROR&lt;/strong&gt;&lt;div&gt;Uh oh, something went wrong with the whozit!&lt;/div&gt;
&lt;/div&gt;

      @desc Creates a new alert
      @constructs

      @param {Object} options                               Component options
      @param {String} [options.heading=Type, capitalized]   Title of the alert (HTML)
      @param {String} options.content                       Content of the alert (HTML)
      @param {Boolean} options.closable                     Array of button descriptors
      @param {String} [options.size=small]                  Size of the alert. Either large or small.
      @param {String} [options.type=error]                  Type of alert to display. One of error, notice, success, help, or info
    */
    construct: function(options) {
      // Catch clicks to dismiss alert
      this.$element.delegate('[data-dismiss="alert"]', 'click.dismiss.alert', this.hide);

      // Add alert class to give styling
      this.$element.addClass('alert');

      // Listen to changes to configuration
      this.$element.on('change:heading', this._setHeading.bind(this));
      this.$element.on('change:content', this._setContent.bind(this));
      this.$element.on('change:type', this._setType.bind(this));
      this.$element.on('change:closable', this._setClosable.bind(this));
      this.$element.on('change:size', this._setSize.bind(this));

      // Read in options "set" by markup so we don't override the values they set
      $.each(this._types, function(index, type) {
        if (this.$element.hasClass(type)) {
          this.options.type = type;
          return false;
        }
      }.bind(this));

      $.each(this._sizes, function(index, size) {
        if (this.$element.hasClass(size)) {
          this.options.size = size;
          return false;
        }
      }.bind(this));

      // Render template, if necessary
      if (this.$element.children().length === 0) {
        // Set default heading
        this.options.heading = this.options.heading === undefined ? this.options.type.toUpperCase() : this.options.heading;

        this.$element.html(CUI.Templates['alert'](this.options));

        this.applyOptions();
      }
      else {
        this.applyOptions(true);
      }
    },

    defaults: {
      type: 'error',
      size: 'small',
      heading: undefined,
      visible: true,
      closable: true
    },

    _types: [
      'error',
      'notice',
      'success',
      'help',
      'info'
    ],

    _sizes: [
      'small',
      'large'
    ],

    applyOptions: function(partial) {
      if (!partial) {
        this._setHeading();
        this._setContent();
      }
      this._setClosable();
      this._setType();
      this._setSize();
    },

    /** @ignore */
    _setContent: function() {
      if (typeof this.options.content !== 'string') return;

      this.$element.find('div').html(this.options.content);
    },

    /** @ignore */
    _setHeading: function() {
      if (typeof this.options.content !== 'string') return;

      this.$element.find('strong').html(this.options.heading);
    },

    /** @ignore */
    _setType: function() {
      if (typeof this.options.type !== 'string' || this._types.indexOf(this.options.type) === -1) return;

      // Remove old type
      this.$element.removeClass(this._types.join(' '));

      // Add new type
      this.$element.addClass(this.options.type);
    },

    /** @ignore */
    _setSize: function() {
      if (typeof this.options.size !== 'string' || this._sizes.indexOf(this.options.size) === -1) return;

      if (this.options.size === 'small')
        this.$element.removeClass('large');
      else
        this.$element.addClass('large');
    },

    /** @ignore */
    _setClosable: function() {
      var el = this.$element.find('.close');
      if (!el.length) {
        // Add the close element if it's not present
        this.$element.prepend('<button class="close" data-dismiss="alert">&times;</button>');
      }
      else {
        el[this.options.closable ? 'show' : 'hide']();
      }
    }
  });

  CUI.util.plugClass(CUI.Alert);

  // Data API
  if (CUI.options.dataAPI) {
    $(function() {
      $('body').fipo('tap.alert.data-api', 'click.alert.data-api', '[data-dismiss="alert"]', function(evt) {
        $(evt.target).parent().hide();
        evt.preventDefault();
      });
    });
  }
}(window.jQuery));
