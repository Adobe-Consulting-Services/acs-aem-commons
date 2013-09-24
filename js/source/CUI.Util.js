(function ($, window, undefined) {
  /**
   * Utility functions used by CoralUI widgets
   * @namespace
   */
  CUI.util = {

    /**
     * Flag if a touch device was detected
     * @type {Boolean}
     */
    isTouch: 'ontouchstart' in window,

    /**
     * Get the target element of a data API action using the data attributes of an element.
     * 
     * @param {jQuery} $element The jQuery object representing the element to get the target from
     * @return {jQuery} The jQuery object representing the target element
     */
    getDataTarget: function ($element) {
      var href = $element.attr('href');
      var $target = $($element.attr('data-target') || (href && href.replace(/.*(?=#[^\s]+$)/, ''))); // Strip for ie7
      return $target;
    },

    /**
     * Decapitalize a string by converting the first letter to lowercase.
     * 
     * @param {String} str The string to de-capitalize
     * @return {String} The de-capitalized string
     */
    decapitalize: function (str) {
      return str.slice(0,1).toLowerCase()+str.slice(1);
    },

    /**
     * Capitalize a string by converting the first letter to uppercase.
     * 
     * @param {String} str The string to capitalize
     * @return {String} The capitalized string
     */
    capitalize: function (str) {
      return str.slice(0,1).toUpperCase()+str.slice(1);
    },

    /**
     * Create a jQuery plugin from a class
     * @param {Class} PluginClass The class to create to create the plugin for
     * @param {String} [pluginName=PluginClass.toString()] The name of the plugin to create. The de-capitalized return value of PluginClass.toString() is used if left undefined
     * @param {Function} [callback]                              A function to execute in the scope of the jQuery object when the plugin is activated. Used for tacking on additional initialization procedures or behaviors for other plugin functionality.
     */
    plugClass: function (PluginClass, pluginName, callback) {
      pluginName = pluginName || CUI.util.decapitalize(PluginClass.toString());

      $.fn[pluginName] = function(optionsIn) {
        var pluginArgs = arguments;
        return this.each(function() {
          var $element = $(this);

          // Combine defaults, data, options, and element config
          var options = $.extend({}, $element.data(), typeof optionsIn === 'object' && optionsIn, { element: this });

          // Get instance, if present already
          var instance = $element.data(pluginName) || new PluginClass(options);

          if (typeof optionsIn === 'string') // Call method, pass args
            instance[optionsIn].apply(instance, Array.prototype.slice.call(pluginArgs, 1));
          else if ($.isPlainObject(optionsIn)) // Apply options
            instance.set(optionsIn);

          if (typeof callback === 'function')
            callback.call(this, instance);
        });
      };

      $.fn[pluginName].Constructor = PluginClass;
    },

    /**
     * Register a callback from a string
     * 
     * @param {String} callbackAsString The string containing the callback function to register
     * @param {Object} [params] Parameters to provide when executing callback
     * @return {Function} The callback function generated from the provided string
     */
    buildFunction: function (callbackAsString, params) {
      params = params || [];

      if (typeof params === "string") {
        params = [params];
      }

      if (callbackAsString) {
        try {
          var Fn = Function;
          return new Fn(params, "return " + callbackAsString + "(" + params.join(", ") + ");");
        } catch (e) {
          return null;
        }
      }
    },

    /**
     * Selects text in the provided field
     * @param {Number} start (optional) The index where the selection should start (defaults to 0)
     * @param {Number} end (optional) The index where the selection should end (defaults to the text length)
     */
    selectText: function (field, start, end) {
      var value = field.val();

      if (value.length > 0) {
        start = start || 0;
        end = end || value.length;
        var domEl = $(field)[0];
        if (domEl.setSelectionRange) {
        // Mostly all browsers
          domEl.blur();
          domEl.setSelectionRange(start, end);
          domEl.focus();
        } else if (domEl.createTextRange) {
          // IE
          var range = domEl.createTextRange();
          range.collapse(true);
          range.moveEnd("character", end - value.length);
          range.moveStart("character", start);
          range.select();
        }
      }
    },

    /**
     * Utility function to get the value of a nested key within an object
     * 
     * @param {Object} object The object to retrieve the value from
     * @param {String} nestedKey The nested key. For instance "foo.bar.baz"
     * @return {Object} The object value for the nested key
     */
    getNested: function(object, nestedKey) {
      if (!nestedKey) {
        return object;
      }

      // Split key into a table
      var keys = typeof nestedKey === "string" ? nestedKey.split(".") : nestedKey;

      // Browse object
      var result = object;
      while (result && keys.length > 0) {
        result = result[keys.shift()];
      }

      return result;
    },
    
    /**
     * Utility function to transform a string representation of a boolean value into that boolean value
     * 
     * @param {String} string representation
     * @return {Boolean} The boolean value of the string
     */
    isTrue: function(str) {
        return str === 'true';
    }

  };

  // add touch class to <html>
  $('html').toggleClass('touch', CUI.util.isTouch);

}(jQuery, this));