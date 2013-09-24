/**
  Crockford's new_constructor pattern, modified to allow walking the prototype chain, automatic constructor/destructor chaining, easy toString methods, and syntactic sugar for calling superclass methods

  @see Base

  @function

  @param {Object} descriptor                        Descriptor object
  @param {String|Function} descriptor.toString   A string or method to use for the toString of this class and instances of this class
  @param {Object} descriptor.extend                 The class to extend
  @param {Function} descriptor.construct            The constructor (setup) method for the new class
  @param {Function} descriptor.destruct             The destructor (teardown) method for the new class
  @param {Mixed} descriptor.*                       Other methods and properties for the new class

  @returns {Base} The created class.
*/
var Class;
var Exception;

(function() {
  /**
    @name Base

    @classdesc The abstract class which contains methods that all classes will inherit.
    Base cannot be extended or instantiated and does not exist in the global namespace.
    If you create a class using <code class="prettyprint">new Class()</code> or <code class="prettyprint">MyClass.extend()</code>, it will come with Base' methods.

    @desc Base is an abstract class and cannot be instantiated directly. Constructors are chained automatically, so you never need to call the constructor of an inherited class directly
    @constructs

    @param {Object} options  Instance options. Guaranteed to be defined as at least an empty Object
   */

  /**
    Binds a method of this instance to the execution scope of this instance.

    @name bind
    @memberOf Base.prototype
    @function

    @param {Function} func The this.method you want to bind
   */
  var bindFunc = function(func) {
    // Bind the function to always execute in scope
    var boundFunc = func.bind(this);

    // Store the method name
    boundFunc._methodName = func._methodName;

    // Store the bound function back to the class
    this[boundFunc._methodName] = boundFunc;

    // Return the bound function
    return boundFunc;
  };

  /**
    Extends this class using the passed descriptor. 
    Called on the Class itself (not an instance), this is an alternative to using <code class="prettyprint">new Class()</code>.
    Any class created using Class will have this static method on the class itself.

    @name extend
    @memberOf Base
    @function
    @static

    @param {Object} descriptor                        Descriptor object
    @param {String|Function} descriptor.toString   A string or method to use for the toString of this class and instances of this class
    @param {Object} descriptor.extend                 The class to extend
    @param {Function} descriptor.construct            The constructor (setup) method for the new class
    @param {Function} descriptor.destruct             The destructor (teardown) method for the new class
    @param {Anything} descriptor.*                    Other methods and properties for the new class
   */
  var extendClass = function(descriptor) {
    descriptor.extend = this;
    return new Class(descriptor);
  };

  Class = function(descriptor) {
    descriptor = descriptor || {};

    if (descriptor.hasOwnProperty('extend') && !descriptor.extend) {
      throw new Class.NonTruthyExtendError(descriptor.toString === 'function' ? descriptor.toString() : descriptor.toString);
    }

    // Extend Object by default
    var extend = descriptor.extend || Object;

    // Construct and destruct are not required
    var construct = descriptor.construct;
    var destruct = descriptor.destruct;

    // Remove special methods and keywords from descriptor
    delete descriptor.bind;
    delete descriptor.extend;
    delete descriptor.destruct;
    delete descriptor.construct;

    // Add toString method, if necessary
    if (descriptor.hasOwnProperty('toString') && typeof descriptor.toString !== 'function') {
      // Return the string provided
      var classString = descriptor.toString;
      descriptor.toString = function() {
        return classString.toString();
      };
    }
    else if (!descriptor.hasOwnProperty('toString') && extend.prototype.hasOwnProperty('toString')) {
      // Use parent's toString
      descriptor.toString = extend.prototype.toString;
    }

    // The remaining properties in descriptor are our methods
    var methodsAndProps = descriptor;

    // Create an object with the prototype of the class we're extending
    var prototype = Object.create(extend && extend.prototype);

    // Store super class as a property of the new class' prototype
    prototype.superClass = extend.prototype;

    // Copy new methods into prototype
    if (methodsAndProps) {  
      for (var key in methodsAndProps) {
        if (methodsAndProps.hasOwnProperty(key)) {
          prototype[key] = methodsAndProps[key];

          // Store the method name so calls to inherited() work
          if (typeof methodsAndProps[key] === 'function') {
            prototype[key]._methodName = key;
            prototype[key]._parentProto = prototype;
          }
        }
      }
    }

    /**
      Call the superclass method with the same name as the currently executing method

      @name inherited
      @memberOf Base.prototype
      @function

      @param {Arguments} args  Unadulterated arguments array from calling function
     */
    prototype.inherited = function(args) {
      // Get the function that call us from the passed arguments objected
      var caller = args.callee;

      // Get the name of the method that called us from a property of the method
      var methodName = caller._methodName;

      if (!methodName) {
        throw new Class.MissingCalleeError(this.toString());
      }

      // Start iterating at the prototype that this function is defined in
      var curProto = caller._parentProto;
      var inheritedFunc = null;

      // Iterate up the prototype chain until we find the inherited function
      while (curProto.superClass) {
        curProto = curProto.superClass;
        inheritedFunc = curProto[methodName];
        if (typeof inheritedFunc === 'function')
          break;
      }

      if (typeof inheritedFunc === 'function') {
        // Store our inherited function
        var oldInherited = this.inherited;

        // Overwrite our inherited function with that of the prototype so the called function can call its parent
        this.inherited = curProto.inherited;

        // Call the inherited function our scope, apply the passed args array
        var retVal = inheritedFunc.apply(this, args);

        // Revert our inherited function to the old function
        this.inherited = oldInherited;

        // Return the value called by the inherited function
        return retVal;
      }
      else {
        throw new Class.InheritedMethodNotFoundError(this.toString(), methodName);
      }
    };

    // Add bind to the prototype of the class
    prototype.bind = bindFunc;

    /**
      Destroys this instance and frees associated memory. Destructors are chained automatically, so the <code class="prettyprint">destruct()</code> method of all inherited classes will be called for you

      @name destruct
      @memberOf Base.prototype
      @function
     */
    prototype.destruct = function() {
      // Call our destruct method first
      if (typeof destruct === 'function') {
        destruct.apply(this);
      }

      // Call superclass destruct method after this class' method
      if (extend && extend.prototype && typeof extend.prototype.destruct === 'function') {
        extend.prototype.destruct.apply(this);      
      }
    };

    // Create a chained construct function which calls the superclass' construct function
    prototype.construct = function() {
      // Add a blank object as the first arg to the constructor, if none provided
      var args = arguments; // get around JSHint complaining about modifying arguments
      if (args[0] === undefined) {
        args.length = 1;
        args[0] = {};
      }

      // call superclass constructor
      if (extend && extend.prototype && typeof extend.prototype.construct === 'function') {
        extend.prototype.construct.apply(this, arguments);      
      }

      // call constructor
      if (typeof construct === 'function') {
        construct.apply(this, arguments);
      }
    };

    // Create a function that generates instances of our class and calls our construct functions
    /** @ignore */
    var instanceGenerator = function() {
      // Create a new object with the prototype we built
      var instance = Object.create(prototype);

      // Call all inherited construct functions
      prototype.construct.apply(instance, arguments);

      return instance;
    };

    instanceGenerator.toString = prototype.toString;

    // Set the prototype of our instance generator to the prototype of our new class so things like MyClass.prototype.method.apply(this) work
    instanceGenerator.prototype = prototype;

    // Add extend to the instance generator for the class
    instanceGenerator.extend = extendClass;

    // The constructor, as far as JS is concerned, is actually our instance generator
    prototype.constructor = instanceGenerator;

    return instanceGenerator;
  };

  if (!Object.create) {
    /**
      Polyfill for Object.create. Creates a new object with the specified prototype.

      @author <a href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Object/create/">Mozilla MDN</a>

      @param {Object} prototype  The prototype to create a new object with
     */
    Object.create = function (prototype) {
      if (arguments.length > 1) {
        throw new Error('Object.create implementation only accepts the first parameter.');
      }
      function Func() {}
      Func.prototype = prototype;
      return new Func();
    };
  }

  if (!Function.prototype.bind) {
    /**
      Polyfill for Function.bind. Binds a function to always execute in a specific scope.

      @author <a href="https://developer.mozilla.org/en/JavaScript/Reference/Global_Objects/Function/bind">Mozilla MDN</a>

      @param {Object} scope  The scope to bind the function to
     */
    Function.prototype.bind = function (scope) {
      if (typeof this !== "function") {
        // closest thing possible to the ECMAScript 5 internal IsCallable function
        throw new TypeError("Function.prototype.bind - what is trying to be bound is not callable");
      }

      var aArgs = Array.prototype.slice.call(arguments, 1);
      var fToBind = this;
      /** @ignore */
      var NoOp = function() {};
      /** @ignore */
      var fBound = function() {
        return fToBind.apply(this instanceof NoOp ? this : scope, aArgs.concat(Array.prototype.slice.call(arguments)));
      };

      NoOp.prototype = this.prototype;
      fBound.prototype = new NoOp();

      return fBound;
    };
  }
  
  Exception = new Class({
    extend: Error,
    construct: function() {
      this.name = 'Error';
      this.message = 'General exception';
    },

    toString: function() {
      return this.name+': '+this.message;
    }
  });
  
  var ClassException = Exception.extend({
    name: 'Class Exception'
  });
  
  // Exceptions
  Class.NonTruthyExtendError = ClassException.extend({
    construct: function(className) {
      this.message = className+' attempted to extend a non-truthy object';
    }
  });
  
  Class.InheritedMethodNotFoundError = ClassException.extend({
    construct: function(className, methodName) {
      this.message = className+" can't call method '"+methodName+"', no method defined in parent classes";
    }
  });
  
  Class.MissingCalleeError = ClassException.extend({
    construct: function(className) {
      this.message = className+" can't call inherited method: calling method did not have _methodName";
    }
  });
}());

(function ($, window, undefined) {
  /**
   * @classdesc The main CUI namespace.
   * @namespace
   *
   * @property {Object} options Main options for CloudUI components.
   * @property {Boolean} options.debug If true, show debug messages for all components.
   * @property {Boolean} options.dataAPI If true, add listeners for widget data APIs.
   * @property {Object} Templates Contains templates used by CUI widgets
   *
   * @example
   * <caption>Change CUI options</caption>
   * <description>You can change CUI options by defining <code>CUI.options</code> before you load CUI.js</description>
   * &lt;script type=&quot;text/javascript&quot;&gt;
   * var CUI = {
   *   options: {
   *     debug: false,
   *     dataAPI: true
   *   }
   * };
   * &lt;/script&gt;
   * &lt;script src=&quot;js/CUI.js&quot;&gt;&lt;/script&gt;
   *
   * preferable include the CUI.js at the bottom before the body closes
   */
  window.CUI = window.CUI || {};

  CUI.options = $.extend({
    debug: false,
    dataAPI: true
  }, CUI.options);

  // REMARK: disabled for now
  // Register partials for all templates
  // Note: this requires the templates to be included BEFORE CUI.js
  /*for (var template in CUI.Templates) {
    Handlebars.registerPartial(template, CUI.Templates[template]);
  }*/

  /**
   * <p><code>cui-contentloaded</code> event is an event that is triggered when a new content is injected to the DOM,
   * which is very similar to {@link https://developer.mozilla.org/en-US/docs/DOM/DOM_event_reference/DOMContentLoaded|DOMContentLoaded} event.</p>
   * <p>This event is normally used so that a JavaScript code can be notified when new content needs to be enhanced (applying event handler, layout, etc).
   * The element where the new content is injected is available at event.target, like so:
   * <pre class="prettyprint linenums jsDocExample">$(document).on("cui-contentloaded", function(e) {
   * var container = e.target;
   * // the container is the element where new content is injected.
   * });</pre>
   * This way the listener can limit the scope of the selector accordingly.</p>
   * <p>It will be triggered at DOMContentLoaded event as well, so component can just listen to this event instead of DOMContentLoaded for enhancement purpose.
   * In that case, the value of event.target is <code>document</code>.</p>
   *
   * @event cui-contentloaded
   */
  $(function () {
    $(document).trigger("cui-contentloaded");
  });

}(jQuery, this));

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
(function ($, window, undefined) {

  /**
   * Load remote content in an element with a CUI spinner
   * @param {String} remote The remote URL to pass to $.load
   * @param {Boolean} [force] Set force to true to force the load to happen with every call, even if it has succeeded already. Otherwise, subsequent calls will simply return.
   * @param {Function} [callback] A function to execute in the scope of the jQuery $.load call when the load finishes (whether success or failure). The arguments to the callback are the load results: response, status, xhr.
   */
  $.fn.loadWithSpinner = function (remote, force, callback) {
    var $target = $(this);

    // load remote link, if necessary
    if (remote && (force || $target.data('loaded-remote') !== remote)) {
    // only show the spinner if the request takes an appreciable amount of time, otherwise
    // the flash of the spinner is a little ugly
    var timer = setTimeout(function() {
      $target.html('<div class="spinner large"></div>');
    }, 50);

    $target.load(remote, function(response, status, xhr) {
    clearTimeout(timer); // no need for the spinner anymore!

    if (status === 'error') {
      $target.html('<div class="alert error"><strong>ERROR</strong> Failed to load content: '+xhr.statusText+' ('+xhr.status+')</div>');
      $target.data('loaded-remote', '');
    }

    if (typeof callback === 'function') {
      callback.call(this, response, status, xhr);
    }
    });

    $target.data('loaded-remote', remote);
    }
  };

  /**
   * $.fn.on for touch devices only
   * @return {jquery} this, chainable
   */
  $.fn.finger = function () {
    if (CUI.util.isTouch) {
      this.on.apply(this, arguments);
    }
    return this;
  };

  /**
   * $.fn.on for pointer devices only
   * @return {jquery} this, chainable
   */
  $.fn.pointer = function () {
    if (!CUI.util.isTouch) {
      this.on.apply(this, arguments);
    }
    return this;
  };

  /**
   * $.fn.on for touch and pointer devices
   * the first parameter is the finger event the second the pointer event
   * @return {jquery} this, chainable
   */
  $.fn.fipo = function () {
    var args = Array.prototype.slice.call(arguments, 1, arguments.length);

    this.pointer.apply(this, args);

    args[0] = arguments[0];
    this.finger.apply(this, args);

    return this;
  };

  /**
   * :focusable and :tabbable selectors 
   * https://raw.github.com/jquery/jquery-ui/master/ui/jquery.ui.core.js
   * @ignore
   */
  function focusable (element, isTabIndexNotNaN) {
    var map, mapName, img,
    nodeName = element.nodeName.toLowerCase();
    if ( "area" === nodeName ) {
      map = element.parentNode;
      mapName = map.name;
      if ( !element.href || !mapName || map.nodeName.toLowerCase() !== "map" ) {
        return false;
      }
      img = $( "img[usemap=#" + mapName + "]" )[0];
      return !!img && visible( img );
    }
    return ( /input|select|textarea|button|object/.test( nodeName ) ?
      !element.disabled :
      "a" === nodeName ?
      element.href || isTabIndexNotNaN :
      isTabIndexNotNaN) &&
    // the element and all of its ancestors must be visible
    visible( element );
  }

  /**
   * :focusable and :tabbable selectors 
   * https://raw.github.com/jquery/jquery-ui/master/ui/jquery.ui.core.js
   * @ignore
   */
  function visible (element) {
    return $.expr.filters.visible( element ) &&
    !$( element ).parents().addBack().filter(function() {
      return $.css( this, "visibility" ) === "hidden";
    }).length;
  }

  /**
   * create pseudo selectors :focusable and :tabbable
   * https://raw.github.com/jquery/jquery-ui/master/ui/jquery.ui.core.js
   * support: jQuery >= 1.8
   */
  $.extend( $.expr[ ":" ], {
    data: $.expr.createPseudo(function( dataName ) {
      return function( elem ) {
        return !!$.data( elem, dataName );
      };
    }),

    /**
     * pseudo selector :focusable
     */
    focusable: function (element) {
      return focusable( element, !isNaN( $.attr( element, "tabindex" ) ) );
    },

    /**
     * pseudo selector :tabbable
     */
    tabbable: function (element) {
      var tabIndex = $.attr( element, "tabindex" ),
      isTabIndexNaN = isNaN( tabIndex );
      return ( isTabIndexNaN || tabIndex >= 0 ) && focusable( element, !isTabIndexNaN );
    }
  });

}(jQuery, this));
/*!
 * jQuery UI Position @VERSION
 * http://jqueryui.com
 *
 * Copyright 2013 jQuery Foundation and other contributors
 * Released under the MIT license.
 * http://jquery.org/license
 *
 * http://api.jqueryui.com/position/
 */
(function( $, undefined ) {

$.ui = $.ui || {};

var cachedScrollbarWidth,
	max = Math.max,
	abs = Math.abs,
	round = Math.round,
	rhorizontal = /left|center|right/,
	rvertical = /top|center|bottom/,
	roffset = /[\+\-]\d+(\.[\d]+)?%?/,
	rposition = /^\w+/,
	rpercent = /%$/,
	_position = $.fn.position;

function getOffsets( offsets, width, height ) {
	return [
		parseFloat( offsets[ 0 ] ) * ( rpercent.test( offsets[ 0 ] ) ? width / 100 : 1 ),
		parseFloat( offsets[ 1 ] ) * ( rpercent.test( offsets[ 1 ] ) ? height / 100 : 1 )
	];
}

function parseCss( element, property ) {
	return parseInt( $.css( element, property ), 10 ) || 0;
}

function getDimensions( elem ) {
	var raw = elem[0];
	if ( raw.nodeType === 9 ) {
		return {
			width: elem.width(),
			height: elem.height(),
			offset: { top: 0, left: 0 }
		};
	}
	if ( $.isWindow( raw ) ) {
		return {
			width: elem.width(),
			height: elem.height(),
			offset: { top: elem.scrollTop(), left: elem.scrollLeft() }
		};
	}
	if ( raw.preventDefault ) {
		return {
			width: 0,
			height: 0,
			offset: { top: raw.pageY, left: raw.pageX }
		};
	}
	return {
		width: elem.outerWidth(),
		height: elem.outerHeight(),
		offset: elem.offset()
	};
}

$.position = {
	scrollbarWidth: function() {
		if ( cachedScrollbarWidth !== undefined ) {
			return cachedScrollbarWidth;
		}
		var w1, w2,
			div = $( "<div style='display:block;width:50px;height:50px;overflow:hidden;'><div style='height:100px;width:auto;'></div></div>" ),
			innerDiv = div.children()[0];

		$( "body" ).append( div );
		w1 = innerDiv.offsetWidth;
		div.css( "overflow", "scroll" );

		w2 = innerDiv.offsetWidth;

		if ( w1 === w2 ) {
			w2 = div[0].clientWidth;
		}

		div.remove();

		return (cachedScrollbarWidth = w1 - w2);
	},
	getScrollInfo: function( within ) {
		var overflowX = within.isWindow ? "" : within.element.css( "overflow-x" ),
			overflowY = within.isWindow ? "" : within.element.css( "overflow-y" ),
			hasOverflowX = overflowX === "scroll" ||
				( overflowX === "auto" && within.width < within.element[0].scrollWidth ),
			hasOverflowY = overflowY === "scroll" ||
				( overflowY === "auto" && within.height < within.element[0].scrollHeight );
		return {
			width: hasOverflowY ? $.position.scrollbarWidth() : 0,
			height: hasOverflowX ? $.position.scrollbarWidth() : 0
		};
	},
	getWithinInfo: function( element ) {
		var withinElement = $( element || window ),
			isWindow = $.isWindow( withinElement[0] );
		return {
			element: withinElement,
			isWindow: isWindow,
			offset: withinElement.offset() || { left: 0, top: 0 },
			scrollLeft: withinElement.scrollLeft(),
			scrollTop: withinElement.scrollTop(),
			width: isWindow ? withinElement.width() : withinElement.outerWidth(),
			height: isWindow ? withinElement.height() : withinElement.outerHeight()
		};
	}
};

$.fn.position = function( options ) {
	if ( !options || !options.of ) {
		return _position.apply( this, arguments );
	}

	// make a copy, we don't want to modify arguments
	options = $.extend( {}, options );

	var atOffset, targetWidth, targetHeight, targetOffset, basePosition, dimensions,
		target = $( options.of ),
		within = $.position.getWithinInfo( options.within ),
		scrollInfo = $.position.getScrollInfo( within ),
		collision = ( options.collision || "flip" ).split( " " ),
		offsets = {};

	dimensions = getDimensions( target );
	if ( target[0].preventDefault ) {
		// force left top to allow flipping
		options.at = "left top";
	}
	targetWidth = dimensions.width;
	targetHeight = dimensions.height;
	targetOffset = dimensions.offset;
	// clone to reuse original targetOffset later
	basePosition = $.extend( {}, targetOffset );

	// force my and at to have valid horizontal and vertical positions
	// if a value is missing or invalid, it will be converted to center
	$.each( [ "my", "at" ], function() {
		var pos = ( options[ this ] || "" ).split( " " ),
			horizontalOffset,
			verticalOffset;

		if ( pos.length === 1) {
			pos = rhorizontal.test( pos[ 0 ] ) ?
				pos.concat( [ "center" ] ) :
				rvertical.test( pos[ 0 ] ) ?
					[ "center" ].concat( pos ) :
					[ "center", "center" ];
		}
		pos[ 0 ] = rhorizontal.test( pos[ 0 ] ) ? pos[ 0 ] : "center";
		pos[ 1 ] = rvertical.test( pos[ 1 ] ) ? pos[ 1 ] : "center";

		// calculate offsets
		horizontalOffset = roffset.exec( pos[ 0 ] );
		verticalOffset = roffset.exec( pos[ 1 ] );
		offsets[ this ] = [
			horizontalOffset ? horizontalOffset[ 0 ] : 0,
			verticalOffset ? verticalOffset[ 0 ] : 0
		];

		// reduce to just the positions without the offsets
		options[ this ] = [
			rposition.exec( pos[ 0 ] )[ 0 ],
			rposition.exec( pos[ 1 ] )[ 0 ]
		];
	});

	// normalize collision option
	if ( collision.length === 1 ) {
		collision[ 1 ] = collision[ 0 ];
	}

	if ( options.at[ 0 ] === "right" ) {
		basePosition.left += targetWidth;
	} else if ( options.at[ 0 ] === "center" ) {
		basePosition.left += targetWidth / 2;
	}

	if ( options.at[ 1 ] === "bottom" ) {
		basePosition.top += targetHeight;
	} else if ( options.at[ 1 ] === "center" ) {
		basePosition.top += targetHeight / 2;
	}

	atOffset = getOffsets( offsets.at, targetWidth, targetHeight );
	basePosition.left += atOffset[ 0 ];
	basePosition.top += atOffset[ 1 ];

	return this.each(function() {
		var collisionPosition, using,
			elem = $( this ),
			elemWidth = elem.outerWidth(),
			elemHeight = elem.outerHeight(),
			marginLeft = parseCss( this, "marginLeft" ),
			marginTop = parseCss( this, "marginTop" ),
			collisionWidth = elemWidth + marginLeft + parseCss( this, "marginRight" ) + scrollInfo.width,
			collisionHeight = elemHeight + marginTop + parseCss( this, "marginBottom" ) + scrollInfo.height,
			position = $.extend( {}, basePosition ),
			myOffset = getOffsets( offsets.my, elem.outerWidth(), elem.outerHeight() );

		if ( options.my[ 0 ] === "right" ) {
			position.left -= elemWidth;
		} else if ( options.my[ 0 ] === "center" ) {
			position.left -= elemWidth / 2;
		}

		if ( options.my[ 1 ] === "bottom" ) {
			position.top -= elemHeight;
		} else if ( options.my[ 1 ] === "center" ) {
			position.top -= elemHeight / 2;
		}

		position.left += myOffset[ 0 ];
		position.top += myOffset[ 1 ];

		// if the browser doesn't support fractions, then round for consistent results
		if ( !$.support.offsetFractions ) {
			position.left = round( position.left );
			position.top = round( position.top );
		}

		collisionPosition = {
			marginLeft: marginLeft,
			marginTop: marginTop
		};

		$.each( [ "left", "top" ], function( i, dir ) {
			if ( $.ui.position[ collision[ i ] ] ) {
				$.ui.position[ collision[ i ] ][ dir ]( position, {
					targetWidth: targetWidth,
					targetHeight: targetHeight,
					elemWidth: elemWidth,
					elemHeight: elemHeight,
					collisionPosition: collisionPosition,
					collisionWidth: collisionWidth,
					collisionHeight: collisionHeight,
					offset: [ atOffset[ 0 ] + myOffset[ 0 ], atOffset [ 1 ] + myOffset[ 1 ] ],
					my: options.my,
					at: options.at,
					within: within,
					elem : elem
				});
			}
		});

		if ( options.using ) {
			// adds feedback as second argument to using callback, if present
			using = function( props ) {
				var left = targetOffset.left - position.left,
					right = left + targetWidth - elemWidth,
					top = targetOffset.top - position.top,
					bottom = top + targetHeight - elemHeight,
					feedback = {
						target: {
							element: target,
							left: targetOffset.left,
							top: targetOffset.top,
							width: targetWidth,
							height: targetHeight
						},
						element: {
							element: elem,
							left: position.left,
							top: position.top,
							width: elemWidth,
							height: elemHeight
						},
						horizontal: right < 0 ? "left" : left > 0 ? "right" : "center",
						vertical: bottom < 0 ? "top" : top > 0 ? "bottom" : "middle"
					};
				if ( targetWidth < elemWidth && abs( left + right ) < targetWidth ) {
					feedback.horizontal = "center";
				}
				if ( targetHeight < elemHeight && abs( top + bottom ) < targetHeight ) {
					feedback.vertical = "middle";
				}
				if ( max( abs( left ), abs( right ) ) > max( abs( top ), abs( bottom ) ) ) {
					feedback.important = "horizontal";
				} else {
					feedback.important = "vertical";
				}
				options.using.call( this, props, feedback );
			};
		}

		elem.offset( $.extend( position, { using: using } ) );
	});
};

$.ui.position = {
	fit: {
		left: function( position, data ) {
			var within = data.within,
				withinOffset = within.isWindow ? within.scrollLeft : within.offset.left,
				outerWidth = within.width,
				collisionPosLeft = position.left - data.collisionPosition.marginLeft,
				overLeft = withinOffset - collisionPosLeft,
				overRight = collisionPosLeft + data.collisionWidth - outerWidth - withinOffset,
				newOverRight;

			// element is wider than within
			if ( data.collisionWidth > outerWidth ) {
				// element is initially over the left side of within
				if ( overLeft > 0 && overRight <= 0 ) {
					newOverRight = position.left + overLeft + data.collisionWidth - outerWidth - withinOffset;
					position.left += overLeft - newOverRight;
				// element is initially over right side of within
				} else if ( overRight > 0 && overLeft <= 0 ) {
					position.left = withinOffset;
				// element is initially over both left and right sides of within
				} else {
					if ( overLeft > overRight ) {
						position.left = withinOffset + outerWidth - data.collisionWidth;
					} else {
						position.left = withinOffset;
					}
				}
			// too far left -> align with left edge
			} else if ( overLeft > 0 ) {
				position.left += overLeft;
			// too far right -> align with right edge
			} else if ( overRight > 0 ) {
				position.left -= overRight;
			// adjust based on position and margin
			} else {
				position.left = max( position.left - collisionPosLeft, position.left );
			}
		},
		top: function( position, data ) {
			var within = data.within,
				withinOffset = within.isWindow ? within.scrollTop : within.offset.top,
				outerHeight = data.within.height,
				collisionPosTop = position.top - data.collisionPosition.marginTop,
				overTop = withinOffset - collisionPosTop,
				overBottom = collisionPosTop + data.collisionHeight - outerHeight - withinOffset,
				newOverBottom;

			// element is taller than within
			if ( data.collisionHeight > outerHeight ) {
				// element is initially over the top of within
				if ( overTop > 0 && overBottom <= 0 ) {
					newOverBottom = position.top + overTop + data.collisionHeight - outerHeight - withinOffset;
					position.top += overTop - newOverBottom;
				// element is initially over bottom of within
				} else if ( overBottom > 0 && overTop <= 0 ) {
					position.top = withinOffset;
				// element is initially over both top and bottom of within
				} else {
					if ( overTop > overBottom ) {
						position.top = withinOffset + outerHeight - data.collisionHeight;
					} else {
						position.top = withinOffset;
					}
				}
			// too far up -> align with top
			} else if ( overTop > 0 ) {
				position.top += overTop;
			// too far down -> align with bottom edge
			} else if ( overBottom > 0 ) {
				position.top -= overBottom;
			// adjust based on position and margin
			} else {
				position.top = max( position.top - collisionPosTop, position.top );
			}
		}
	},
	flip: {
		left: function( position, data ) {
			var within = data.within,
				withinOffset = within.offset.left + within.scrollLeft,
				outerWidth = within.width,
				offsetLeft = within.isWindow ? within.scrollLeft : within.offset.left,
				collisionPosLeft = position.left - data.collisionPosition.marginLeft,
				overLeft = collisionPosLeft - offsetLeft,
				overRight = collisionPosLeft + data.collisionWidth - outerWidth - offsetLeft,
				myOffset = data.my[ 0 ] === "left" ?
					-data.elemWidth :
					data.my[ 0 ] === "right" ?
						data.elemWidth :
						0,
				atOffset = data.at[ 0 ] === "left" ?
					data.targetWidth :
					data.at[ 0 ] === "right" ?
						-data.targetWidth :
						0,
				offset = -2 * data.offset[ 0 ],
				newOverRight,
				newOverLeft;

			if ( overLeft < 0 ) {
				newOverRight = position.left + myOffset + atOffset + offset + data.collisionWidth - outerWidth - withinOffset;
				if ( newOverRight < 0 || newOverRight < abs( overLeft ) ) {
					position.left += myOffset + atOffset + offset;
				}
			}
			else if ( overRight > 0 ) {
				newOverLeft = position.left - data.collisionPosition.marginLeft + myOffset + atOffset + offset - offsetLeft;
				if ( newOverLeft > 0 || abs( newOverLeft ) < overRight ) {
					position.left += myOffset + atOffset + offset;
				}
			}
		},
		top: function( position, data ) {
			var within = data.within,
				withinOffset = within.offset.top + within.scrollTop,
				outerHeight = within.height,
				offsetTop = within.isWindow ? within.scrollTop : within.offset.top,
				collisionPosTop = position.top - data.collisionPosition.marginTop,
				overTop = collisionPosTop - offsetTop,
				overBottom = collisionPosTop + data.collisionHeight - outerHeight - offsetTop,
				top = data.my[ 1 ] === "top",
				myOffset = top ?
					-data.elemHeight :
					data.my[ 1 ] === "bottom" ?
						data.elemHeight :
						0,
				atOffset = data.at[ 1 ] === "top" ?
					data.targetHeight :
					data.at[ 1 ] === "bottom" ?
						-data.targetHeight :
						0,
				offset = -2 * data.offset[ 1 ],
				newOverTop,
				newOverBottom;
			if ( overTop < 0 ) {
				newOverBottom = position.top + myOffset + atOffset + offset + data.collisionHeight - outerHeight - withinOffset;
				if ( ( position.top + myOffset + atOffset + offset) > overTop && ( newOverBottom < 0 || newOverBottom < abs( overTop ) ) ) {
					position.top += myOffset + atOffset + offset;
				}
			}
			else if ( overBottom > 0 ) {
				newOverTop = position.top -  data.collisionPosition.marginTop + myOffset + atOffset + offset - offsetTop;
				if ( ( position.top + myOffset + atOffset + offset) > overBottom && ( newOverTop > 0 || abs( newOverTop ) < overBottom ) ) {
					position.top += myOffset + atOffset + offset;
				}
			}
		}
	},
	flipfit: {
		left: function() {
			$.ui.position.flip.left.apply( this, arguments );
			$.ui.position.fit.left.apply( this, arguments );
		},
		top: function() {
			$.ui.position.flip.top.apply( this, arguments );
			$.ui.position.fit.top.apply( this, arguments );
		}
	}
};

// fraction support test
(function () {
	var testElement, testElementParent, testElementStyle, offsetLeft, i,
		body = document.getElementsByTagName( "body" )[ 0 ],
		div = document.createElement( "div" );

	//Create a "fake body" for testing based on method used in jQuery.support
	testElement = document.createElement( body ? "div" : "body" );
	testElementStyle = {
		visibility: "hidden",
		width: 0,
		height: 0,
		border: 0,
		margin: 0,
		background: "none"
	};
	if ( body ) {
		$.extend( testElementStyle, {
			position: "absolute",
			left: "-1000px",
			top: "-1000px"
		});
	}
	for ( i in testElementStyle ) {
		testElement.style[ i ] = testElementStyle[ i ];
	}
	testElement.appendChild( div );
	testElementParent = body || document.documentElement;
	testElementParent.insertBefore( testElement, testElementParent.firstChild );

	div.style.cssText = "position: absolute; left: 10.7432222px;";

	offsetLeft = $( div ).offset().left;
	$.support.offsetFractions = offsetLeft > 10 && offsetLeft < 11;

	testElement.innerHTML = "";
	testElementParent.removeChild( testElement );
})();

}( jQuery ) );

(function ($, window, undefined) {

  CUI.Widget = new Class(/** @lends CUI.Widget# */{
    toString: 'Widget',

    /**
     * @classdesc The base class for all widgets
     *
     * @desc Creates a new widget
     * @constructs
     * 
     * @param {Object} options Widget options
     * @param {Boolean} [options.visible=false] If True, show the widget immediately
     */
    construct: function (options) {
      // Store options
      this.options = $.extend({}, typeof this.defaults === 'object' && this.defaults, options);

      // Store jQuery object
      this.$element = $(options.element);

      // Add instance to element's data
      this.$element.data(CUI.util.decapitalize(this.toString()), this);

      // Bind functions commonly called by listeners
      this.bind(this.hide);
      this.bind(this.show);
      this.bind(this.toggleVisibility);

      // Show/hide when this.options.visible changes
      this.on('change:visible', function (evt) {
        this[evt.value ? '_show' : '_hide']();
      }.bind(this));
    },

    /**
     * Set a number of options using an object or a string
     * @name set
     * @memberOf CUI.Widget#
     * @function
     * 
     * @param {String|Object} option The option to set as a string, or an object of key/value pairs to set
     * @param {String} value The value to set the option to (is ignored when first argument is an object)
     * 
     * @return {CUI.Widget} this, chainable
     */
    set: function (optionOrObj, value) {
      if ($.isPlainObject(optionOrObj)) {
        // Set multiple options
        for (var option in optionOrObj) {
          this._set(option, optionOrObj[option]);
        }
      }
      else {
        // Set single option
        this._set(optionOrObj, value);
      }

      return this;
    },

    /**
     * @ignore
     */
    _set: function (option, value) {
      // Trigger a change event
      var e = $.Event('beforeChange:'+option, {
        widget: this, // We want to know who fired this event (used by CUI.Filters, CUI.DropdownList)
        option: option,
        currentValue: this.options[option],
        value: value
      });
      this.$element.trigger(e);

      // Don't set if prevented
      if (e.isDefaultPrevented()) return this;

      // Set value
      this.options[option] = value;

      e = $.Event('change:'+option, {
        widget: this,
        option: option,
        value: value
      });
      this.$element.trigger(e);
    },

    /**
     * Get the value of an option
     * @param {String} option The name of the option to fetch the value of
     * @return {Mixed} Option value
     */
    get: function (option) {
      return this.options[option];
    },

   /**
    * Add an event listener
    * @param {String} evtName The event name to listen for
    * @param {Function} func The function that will be called when the event is triggered
    * @return {CUI.Widget} this, chainable
    */
    on: function (evtName, func) {
      this.$element.on.apply(this.$element, arguments);
      return this;
    },

   /**
    * Remove an event listener
    * @param {String} evtName The event name to stop listening for
    * @param {Function} func     The function that was passed to on()
    * @return {CUI.Widget} this, chainable
    */
    off: function (evtName, func) {
      this.$element.off.apply(this.$element, arguments);
      return this;
    },

    /**
     * Show the widget
     * @return {CUI.Widget} this, chainable
     */
    show: function (evt) {
      evt = evt || {};

      if (this.options.visible)
        return this;

      if (!evt.silent) {
        // Trigger event
        var e = $.Event('show');
        this.$element.trigger(e);

        // Do nothing if event is prevented or we're already visible
        if (e.isDefaultPrevented()) return this;
      }

      this.options.visible = true;

      this._show(evt);

      return this;
    },
    
    /**
     * @ignore
     */
    _show: function (evt) {
      this.$element.show();
    },

    /**
     * Hide the widget
     * 
     * @return {CUI.Widget} this, chainable
     */
    hide: function (evt) {
      evt = evt || {};

      if (!this.options.visible)
        return this;

      if (!evt.silent) {
        // Trigger event
        var e = $.Event('hide');
        this.$element.trigger(e);

        if (e.isDefaultPrevented()) return this;
      }

      this.options.visible = false;

      this._hide(evt);

      return this;
    },

    /**
     * @ignore
     */
    _hide: function (evt) {
      this.$element.hide();
    },

   /**
    * Toggle the visibility of the widget
    * @return {CUI.Widget} this, chainable
    */
    toggleVisibility: function () {
      return this[!this.options.visible ? 'show' : 'hide']();
    },

    /**
     * Set a custom name for this widget.
     * 
     * @param {String} customName Component name
     * @return {CUI.Widget} this, chainable
     */
    setName: function (customName) {
      /** @ignore */
      this.toString = function () {
        return customName;
      };

      return this;
    }

    /**
      Triggered when the widget is shown

      @name CUI.Widget#show
      @event
      */

    /**
      Triggered when the widget is hidden

      @name CUI.Widget#hide
      @event
      */

    /**
      Triggered when before an option is changed

      @name CUI.Widget#beforeChange:*
      @event

      @param {Object} evt                    Event object
      @param {Mixed} evt.option              The option that changed
      @param {Mixed} evt.currentValue        The current value
      @param {Mixed} evt.value               The value this option will be changed to
      @param {Function} evt.preventDefault   Call to prevent the option from changing
      */

    /**
      Triggered when an option is changed

      @name CUI.Widget#change:*
      @event

      @param {Object} evt          Event object
      @param {Mixed} evt.option    The option that changed
      @param {Mixed} evt.value     The new value
      */
  });

}(jQuery, this));
(function ($, window, undefined) {
    CUI.TagList = new Class(/** @lends CUI.TagList# */{
        toString: 'TagList',

        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc A tag list for input widgets. This widget is intended to be used by other widgets.
         *
         * <h2 class="line">Examples</h2>
         *  
         * <ol class="taglist" data-init="taglist" data-fieldname="myrequestparam" style="margin: 2rem">
         *     <li>
         *         <button class="icon-close"></button>
         *         Carrot
         *         <input type="hidden" value="Carrot"/>
         *     </li>
         *     <li>
         *         <button class="icon-close"></button>
         *         Banana
         *         <input type="hidden" value="Banana"/>
         *     </li>
         *     <li>
         *         <button class="icon-close"></button>
         *         Apple
         *         <input type="hidden" value="Apple"/>
         *     </li>
         * </ol>
         *
         * @example
         * <caption>Data API: Instantiate, set options, and show</caption>
         * 
         * &lt;ol class=&quot;taglist&quot; data-init=&quot;taglist&quot; data-fieldname=&quot;myrequestparam&quot;&gt;
         *     &lt;li&gt;
         *         &lt;button class=&quot;icon-close&quot;&gt;&lt;/button&gt;
         *         Carrot
         *         &lt;input type=&quot;hidden&quot; value=&quot;Carrot&quot;/&gt;
         *     &lt;/li&gt;
         *     &lt;li&gt;
         *         &lt;button class=&quot;icon-close&quot;&gt;&lt;/button&gt;
         *         Banana
         *         &lt;input type=&quot;hidden&quot; value=&quot;Banana&quot;/&gt;
         *     &lt;/li&gt;
         *     &lt;li&gt;
         *         &lt;button class=&quot;icon-close&quot;&gt;&lt;/button&gt;
         *         Apple
         *         &lt;input type=&quot;hidden&quot; value=&quot;Apple&quot;/&gt;
         *     &lt;/li&gt;
         * &lt;/ol&gt;
         *
         * @description Creates a new tag list
         * @constructs
         * 
         * @param  {Object} options Component options
         * @param  {Mixed} options.element jQuery selector or DOM element to use for panel
         * @param  {String} options.fieldname fieldname for the input fields
         * @param  {Array} options.values to set the taglist
         *
         * @fires TagList#itemadded
         * @fires TagList#itemremoved
         * 
         */
        construct: function (options) {
            var self = this;

            this.applyOptions();

            this.$element
                .on('change:values', this._setValues.bind(this));

            this.$element.fipo('tap', 'click', 'button', function (event) {
                var elem = $(event.currentTarget).next('input');

                self.removeItem(elem.val());
            });

            // accessibility
            this._makeAccessible();
        },

        defaults: {
            fieldname: "",
            values: null,
            tag: 'li'
        },

        /**
         * existing values in the tag list
         * @private
         * @type {Array}
         */
        _existingValues: null,

        applyOptions: function () {
            var self = this;

            this._existingValues = [];

            this.options.values = this.options.values || [];

            // set values if given
            if (this.options.values.length > 0) {
                this._setValues();
            } else { // read from markup
                this.$element.find('input').each(function (i, e) {
                    var elem = $(e);
 
                    // add to options.values
                    self._existingValues.push(elem.attr('value'));
                });
            }
        },

        /**
         * @private
         */
        _setValues: function () {
            var items = this.options.values;

            // remove list elements
            this.$element.empty();

            // clear options to readd
            this.options.values = [];
            // add elements again
            this.addItem(items);
        },

        /**
         * adds some accessibility attributes and features
         * http://www.w3.org/WAI/PF/aria/roles#list
         * @private
         */
        _makeAccessible: function () {
            this.$element.attr({
                'role': 'list'
            });

            this.$element.children(this.options.tag).attr({
                'role': 'listitem'
            });
        },

        /**
         * @private
         */
        _show: function () {
            this.$element
                .show()
                .attr('aria-hidden', false);
        },

        /**
         * @private
         */
        _hide: function () {
            this.$element
                .hide()
                .attr('aria-hidden', true);
        },

        /**
         * remove an item from the DOM
         * @private
         * @param  {String} item
         */
        _removeItem: function (item) {
            var elem = this.$element.find('input[value="' + item + '"]');

            if (elem.length > 0) {
                elem.parent().remove();

                this.$element.trigger($.Event('itemremoved'), {
                    value: item
                });
            }
        },

        /**
         * adds a new item to the DOM
         * @private
         * @param  {String|Object} item entry to be displayed
         */
        _appendItem: function (item) {
            var display, val, elem;

            // see if string or object
            if ($.type(item) === "string") {
                display = val = item;
            } else {
                display = item.display;
                val = item.value;
            }

            // always be a string
            val += "";

            if (($.inArray(val, this._existingValues) > - 1) || val.length === 0) {
                return;
            }

            // add to internal storage
            this._existingValues.push(val); // store as string

            // add DOM element
            elem = $('<'+ this.options.tag +'/>', {
                'role': 'listitem',
                'text': display
            });

            $('<button/>', {
                'class': 'icon-close'
            }).prependTo(elem);

            $('<input/>', {
                'type': 'hidden',
                'value': val,
                'name': this.options.fieldname
            }).appendTo(elem);

            this.$element.append(elem);

            this.$element.trigger($.Event('itemadded'), {
                value: val,
                display: display
            });
        },

        /**
         * @param {String} item value to be deleted
         */
        removeItem: function (item) {
            var idx = this._existingValues.indexOf("" + item);

            if (idx > -1) {
                this._removeItem(item);
                this._existingValues.splice(idx, 1);
            }
        },

        /**
         * @param  {String|Object|Array} item
         * @param  {String} item.display
         * @param  {String} item.value
         */
        addItem: function (item) {
            var self = this,
                items = $.isArray(item) ? item : [item];

            $.each(items, function (i, item) {
                self._appendItem(item);
            });
        }
    });

    CUI.util.plugClass(CUI.TagList);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (event) {
            $('[data-init~=taglist]', event.target).tagList();
        });
    }

    /**
     * Triggered when an item was added
     *
     * @name CUI.TagList#itemadded
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.value value which was added
     * @param {String} event.display displayed text of the element
     */
    
    /**
     * Triggered when an item was removed
     *
     * @name CUI.TagList#itemremoved
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.value value which was removed
     */

}(jQuery, this));

(function ($, window, undefined) {
    CUI.SelectList = new Class(/** @lends CUI.SelectList# */{
        toString: 'SelectList',

        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc A select list for drop down widgets. This widget is intended to be used by other widgets.
         *
         * <h2 class="line">Examples</h2>
         * 
         * <ul class="selectlist" data-init="selectlist">
         *     <li data-value="expr1">Expression 1</li>
         *     <li data-value="expr2">Expression 2</li>
         *     <li data-value="expr3">Expression 3</li>
         * </ul>
         *
         * <ul class="selectlist" data-init="selectlist" data-multiple="true">
         *     <li class="optgroup">
         *         <span>Group 1</span>
         *         <ul>
         *             <li data-value="expr1">Expression 1</li>
         *             <li data-value="expr2">Expression 2</li>
         *             <li data-value="expr3">Expression 3</li>
         *         </ul>
         *     </li>
         *     <li class="optgroup">
         *         <span>Group 2</span>
         *         <ul>
         *             <li data-value="expr4">Expression 4</li>
         *             <li data-value="expr5">Expression 5</li>
         *         </ul>
         *     </li>
         * </ul>
         *
         * @example
         * <caption>Instantiate with Class</caption>
         * var selectlist = new CUI.SelectList({
         *     element: '#mySelectList'
         * });
         *
         * // show the select list
         * selectlist.show();
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#mySelectList').selectList({
         *
         * });
         *
         * // jQuery style works as well for show/hide
         * $('#mySelectList').selectList('show');
         *
         * @example
         * <caption>Data API: Instantiate, set options, and show</caption>
         *
         * &lt;ul class=&quot;selectlist&quot; data-init=&quot;selectlist&quot;&gt;
         *     &lt;li data-value=&quot;expr1&quot;&gt;Expression 1&lt;/li&gt;
         *     &lt;li data-value=&quot;expr2&quot;&gt;Expression 2&lt;/li&gt;
         *     &lt;li data-value=&quot;expr3&quot;&gt;Expression 3&lt;/li&gt;
         * &lt;/ul&gt;
         *
         * &lt;ul class=&quot;selectlist&quot; data-init=&quot;selectlist&quot; data-multiple=&quot;true&quot;&gt;
         *     &lt;li class=&quot;optgroup&quot;&gt;
         *         &lt;span&gt;Group 1&lt;/span&gt;
         *         &lt;ul&gt;
         *             &lt;li data-value=&quot;expr1&quot;&gt;Expression 1&lt;/li&gt;
         *             &lt;li data-value=&quot;expr2&quot;&gt;Expression 2&lt;/li&gt;
         *             &lt;li data-value=&quot;expr3&quot;&gt;Expression 3&lt;/li&gt;
         *         &lt;/ul&gt;
         *     &lt;/li&gt;
         *     &lt;li class=&quot;optgroup&quot;&gt;
         *         &lt;span&gt;Group 2&lt;/span&gt;
         *         &lt;ul&gt;
         *             &lt;li data-value=&quot;expr4&quot;&gt;Expression 4&lt;/li&gt;
         *             &lt;li data-value=&quot;expr5&quot;&gt;Expression 5&lt;/li&gt;
         *         &lt;/ul&gt;
         *     &lt;/li&gt;
         * &lt;/ul&gt;
         * 
         *
         * @example
         * <caption>Initialize with custom paramters to load remotely</caption>
         * 
         * &lt;ul class=&quot;selectlist&quot; data-init=&quot;selectlist&quot; data-type=&quot;dynamic&quot; data-dataurl=&quot;remotehtml.html&quot;&gt;
         *     
         * &lt;/ul&gt;
         *
         * @description Creates a new select list
         * @constructs
         * 
         * @param  {Object} options Component options
         * @param  {Mixed} options.element jQuery selector or DOM element to use for panel
         * @param  {String} [options.type=static] static or dynamic list
         * @param  {Boolean} [options.multiple=false] multiple selection or not
         * @param  {Object} options.relatedElement DOM element to position at
         * @param  {Boolean} [options.autofocus=true] automatically sets the focus on the list
         * @param  {Boolean} [options.autohide=true] automatically closes the list when it loses its focus
         * @param  {String} [options.dataurl] URL to receive values dynamically
         * @param  {String} [options.dataurlformat=html] format of the dynamic data load
         * @param  {Object} [options.dataadditional] additonal data to be sent
         * @param  {Function} [options.loadData] function to be called if more data is needed. This must not be used with a set dataurl.
         *
         * 
         */
        construct: function (options) {
            this.applyOptions();

            this.$element
                .on('change:type', this._setType.bind(this))
                .on('change:autohide', this._setAutohide.bind(this))
                .on('click', '[role="option"]', this._triggerSelected.bind(this));

            // accessibility
            this._makeAccessible();
        },

        defaults: {
            type: 'static', // static or dynamic
            multiple: false,
            relatedElement: null,
            autofocus: true, // autofocus on show
            autohide: true, // automatically hides the box if it loses focus
            dataurl: null,
            dataurlformat: 'html',
            datapaging: true,
            datapagesize: 10,
            dataadditional: null,
            loadData: $.noop, // function to receive more data
            position: 'center bottom-1'  // -1 to override the border
        },

        applyOptions: function () {
            this._setType();
        },

        /**
         * @private
         */
        _setAutohide: function () {
            var self = this,
                receivedFocus = false;

            if (this.options.autohide) {
                this.$element
                    .on('focusout.selectlist-autohide', function (event) {
                        clearTimeout(self._autohideTimer);
                        self._autohideTimer = setTimeout(function () {
                            if (!receivedFocus) {
                                self.hide();
                            }
                            receivedFocus = false;
                        }, 500);
                    })
                    .on('focusin.selectlist-autohide', function (event) {
                        receivedFocus = true;
                    });
            } else {
                this.$element.off('focusout.selectlist-autohide focusin.selectlist-autohide');
            }
        },

        /**
         * @private
         */
        _setType: function () {
            var self = this,
                timeout;

            function timeoutLoadFunc() {
                var elem = self.$element.get(0),
                    scrollHeight = elem.scrollHeight,
                    scrollTop = elem.scrollTop;

                if ((scrollHeight - self.$element.height()) <= (scrollTop + 30)) {
                    self._handleLoadData();
                }
            }

            // we have a dynamic list of values
            if (this.options.type === 'dynamic') {

                this.$element.on('scroll.selectlist-dynamic-load', function (event) {
                    // debounce
                    if (timeout) {
                        clearTimeout(timeout);
                    }

                    if (self._loadingComplete || this._loadingIsActive) {
                        return;
                    }

                    timeout = setTimeout(timeoutLoadFunc, 500);
                });
            } else { // static
                this.$element.off('scroll.selectlist-dynamic-load');
            }
        },

        /**
         * adds some accessibility attributes and features
         * http://www.w3.org/WAI/PF/aria/roles#listbox
         * @private
         */
        _makeAccessible: function () {
            this.$element.attr({
                'role': 'listbox',
                'tabindex': -1, // the list itself is not focusable
                'aria-hidden': true,
                'aria-multiselectable': this.options.multiple
            });

            this._makeAccessibleListOption(this.$element.children());

            // setting tabindex
            this.$element.on('focusin focusout', 'li[role="option"]', function (event) {
                $(event.currentTarget).attr('tabindex', event.type === 'focusin' ? -1 : 0);
            });

            // keyboard handling
            this.$element.on('keydown', 'li[role="option"]', function (event) {
                // enables keyboard support

                var elem = $(event.currentTarget),
                    entries = $(event.delegateTarget)
                        .find('[role="option"]')
                        .not('[aria-disabled="true"]'), // ignore disabled
                    focusElem = elem,
                    keymatch = true,
                    idx = entries.index(elem);

                switch (event.which) {
                    case 13: // enter
                    case 32: // space
                        // choose element
                        elem.trigger('click');
                        event.preventDefault();
                        keymatch = false;
                        break;
                    case 27: //esc
                        elem.trigger('blur');
                        keymatch = false;
                        break;
                    case 33: //page up
                    case 37: //left arrow
                    case 38: //up arrow
                        focusElem = idx-1 > -1 ? entries[idx-1] : entries[entries.length-1];
                        break;
                    case 34: //page down
                    case 39: //right arrow 
                    case 40: //down arrow
                        focusElem = idx+1 < entries.length ? entries[idx+1] : entries[0];
                        break;
                    case 36: //home
                        focusElem = entries[0];
                        break;
                    case 35: //end
                        focusElem = entries[entries.length-1];
                        break;
                    default:
                        keymatch = false;
                        break;
                }

                if (keymatch) { // if a key matched then we set the currently focused element
                    event.preventDefault();
                    $(focusElem).trigger('focus');
                }
            });
        },

        /**
         * makes the list options accessible
         * @private
         * @param  {jQuery} elem
         */
        _makeAccessibleListOption: function (elem) {
            elem.each(function (i, e) {
                var entry = $(e);

                // group header
                if (entry.hasClass('optgroup')) {
                    entry.attr({
                        'role': 'presentation',
                        'tabindex': -1
                    }).children('ul').attr({
                        'role': 'group'
                    }).children('li').attr({
                        'role': 'option',
                        'tabindex': 0
                    });

                } else {
                    entry.attr({
                        'role': 'option',
                        'tabindex': 0
                    });
                }
            });
        },

        /**
         * @private
         */
        _show: function () {
            var self = this;

            this.$element
                .addClass('visible')
                .attr('aria-hidden', false);

            this.$element.position({
                my: 'top',
                at: this.options.position,
                of: this.options.relatedElement
            });

            if (this.options.autofocus) {
                this.$element.find('li[role="option"]:first').trigger('focus');
            }

            // if dynamic start loading
            if (this.options.type === 'dynamic') {
                this._handleLoadData().done(function () {
                    self.$element.find('li[role="option"]:first').trigger('focus');
                    this._setAutohide();
                });
            } else { // otherwise set autohide immediately
                this._setAutohide();
            }
        },

        /**
         * @private
         */
        _hide: function () {
            if (this._autohideTimer) {
                clearTimeout(this._autohideTimer);
            }
            this.$element
                .removeClass('visible')
                .attr('aria-hidden', true);

            
            this.reset();
        },

        /**
         * triggers an event for the currently selected element
         * @fires SelectList#selected
         * @private
         */
        _triggerSelected: function (event) {
            var cur = $(event.currentTarget),
                val = cur.data('value'),
                display = cur.text();

            cur.trigger($.Event('selected', {
                selectedValue: val,
                displayedValue: display
            }));
        },

        /**
         * deletes the item from the dom
         */
        clearItems: function () {
            this.$element.empty();
        },

        /**
         * current position for the pagination
         * @private
         * @type {Number}
         */
        _pagestart: 0,

        /**
         * indicates if all data was fetched
         * @private
         * @type {Boolean}
         */
        _loadingComplete: false,

        /**
         * indicates if currently data is fetched
         * @private
         * @type {Boolean}
         */
        _loadingIsActive: false,

        /**
         * handle asynchronous loading of data (type == dynamic)
         * @private
         */
        _handleLoadData: function () {
            var promise,
                self = this,
                end = this._pagestart + this.options.datapagesize,
                wait = $('<div/>',{
                    'class': 'selectlist-wait'
                }).append($('<span/>', {
                    'class': 'wait'
                }));

            if (this._loadingIsActive) {
                return;
            }

            // activate fetching
            this._loadingIsActive = true;

            // add wait
            this.$element.append(wait);

            // load from given URL
            if (this.options.dataurl) {
                promise = $.ajax({
                    url: this.options.dataurl,
                    context: this,
                    dataType: this.options.dataurlformat,
                    data: $.extend({
                        start: this._pagestart,
                        end: end
                    }, this.options.dataadditional || {})
                }).done(function (data) {
                    var cnt = 0;

                    if (self.options.dataurlformat === 'html') {
                        var elem = $(data);

                        cnt = elem.filter('li').length;

                        self._makeAccessibleListOption(elem);
                        self.$element.append(elem);
                    }

                    // if not enough elements came back then the loading is complete
                    if (cnt < self.options.datapagesize) {
                        this._loadingComplete = true;
                    }

                });

            } else { // expect custom function to handle
                promise = this.options.loadData.call(this, this._pagestart, end);
            }

            // increase to next page
            this._pagestart = end;

            promise.always(function () {
                wait.remove();
                this._loadingIsActive = false;
            });

            return promise;
        },

        /**
         * resets the dynamic loaded data
         */
        reset: function () {
            if (this.options.type === 'dynamic') {
                this.clearItems();
                this._pagestart = 0;
                this._loadingComplete = false;
            }
        },

        /**
         * triggers a loading operation 
         * this requires to have the selectlist in a dynamic configuration
         * @param  {Boolean} reset resets pagination
         */
        triggerLoadData: function (reset) {
            if (reset) {
                this.reset();
            }

            this._handleLoadData();
        }
    });

    CUI.util.plugClass(CUI.SelectList);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (event) {
            $('[data-init~=selectlist]', event.target).selectList();
        });
    }

    /**
     * Triggered when option was selected
     *
     * @name CUI.SelectList#selected
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.selectedValue value which was selected
     * @param {String} event.displayedValue displayed text of the selected element
     */
    
    /**
     * Triggered when option was unselected
     *
     * @name CUI.SelectList#unselected
     * @event
     *
     * @param {Object} event Event object
     * @param {String} event.selectedValue value which was unselected
     * @param {String} event.displayedValue displayed text of the unselected element
     */

}(jQuery, this));

(function ($, window, undefined) {
    CUI.Autocomplete = new Class(/** @lends CUI.Autocomplete# */{
        toString: 'Autocomplete',

        extend: CUI.Widget,

        defaults: {
            mode: 'starts', // filter mode ['starts', 'contains']
            delay: 500,
            showtypeahead: false,
            showsuggestions: false,
            showclearbutton: false,
            showtags: false,

            selectlistConfig: null,
            tagConfig: null
        },

        construct: function () {
            var self = this;

            // find elements
            this._input = this.$element.children('input');
            this._selectlist = this.$element.find('.selectlist');
            this._tags = this.$element.find('.taglist');

            this._suggestionsBtn = this.$element.find('.autocomplete-suggestion-toggle');


            // apply
            this.applyOptions();
        },

        applyOptions: function () {
            this._setClearButton();

            this._setTags();
            this._setSelectlist();
            this._setTypeahead();
            this._setSuggestions();

            this._setType();
        },

        /**
         * initializes the type of the autocomplete
         */
        _setType: function () {
            if (this._selectListWidget.options.type === 'static') {
                this.$element.on('query', this.handleStaticFilter.bind(this));
            } else if (this._selectListWidget.options.type === 'dynamic') {
                this.$element.on('query', this.handleDynamicFilter.bind(this));
            }
        },

        /**
         * initialize the clear button
         * @private
         */
        _setClearButton: function () {
            var self = this;

            if (this.options.showclearbutton) {

                // create button if not there
                if (!this._clearBtn) {
                    this._clearBtn = $('<button/>', {
                        'class': 'autocomplete-clear icon-close'
                    }).fipo('tap', 'click', function (event) {
                        event.preventDefault();

                        self.clear();
                        self._input.focus();
                    }).finger('click', false);
                }

                this._clearBtn.appendTo(this.$element);
                this._input.on('keyup.autocomplete-clearbtn', this._refreshClear.bind(this));
                this._refreshClear();
            } else {
                if (this._clearBtn) {
                    this._clearBtn.detach();
                }
                this._input.off('keyup.autocomplete-clearbtn');
            }
        },

        /*_setSuggestions: function () {
            var self = this;

            if (this.options.showsuggestions) {

                // if the element is not there, create it
                if (this._suggestions.length === 0) {
                    this._suggestions = $('<ul/>', {
                        'class': 'selectlist autocomplete-suggestions'
                    }).appendTo(this.$element);
                }

                this._suggestions.selectList($.extend({
                    relatedElement: this._input
                }, this.options.suggestionConfig || {}));

                this._selectListSuggestion = this._suggestions.data('selectList');

                // if the button to trigger the suggestion box is not there, 
                // then we add it
                if (this._suggestionsBtn.length === 0) {

                    this._suggestionsBtn = $('<button/>', {
                        'class': 'autocomplete-suggestion-toggle'
                    });

                    this._suggestionsBtn.appendTo(this.$element);
                }

                // handler to open usggestion box
                this._suggestionsBtn.fipo('tap', 'click', function (event) {
                    event.preventDefault();
                    self._toggleSuggestions();
                }).finger('click', false);


                this._suggestions
                    // receive the value from the list
                    .on('selected.autcomplete-suggestion', this._handleSuggestionSelected.bind(this))
                    // handle open/hide for the button
                    .on('show.autcomplete-suggestion hide.autcomplete-suggestion', function (event) {
                        self._suggestionsBtn.toggleClass('active', event.type === 'show');
                    });
                // add class to input to to increase padding right for the button
                this._input.addClass('autocomplete-has-suggestion-btn');
            } else {
                this._suggestionsBtn.remove();
                this._suggestions.off('selected.autcomplete-suggestion show.autcomplete-suggestion hide.autcomplete-suggestion');
                this._input.removeClass('autocomplete-has-suggestion-btn');
            }
        },*/

        /**
         * initializes the select list widget
         * @private
         */
        _setSelectlist: function () {
            var self = this;

            // if the element is not there, create it
            if (this._selectlist.length === 0) {
                this._selectlist = $('<ul/>', {
                    'class': 'selectlist'
                }).appendTo(this.$element);
            }

            this._selectlist.selectList($.extend({
                relatedElement: this._input,
                autofocus: false,
                autohide: false
            }, this.options.selectlistConfig || {}));

            this._selectListWidget = this._selectlist.data('selectList');

            this._selectlist
                // receive the value from the list
                .on('selected.autcomplete', this._handleSelected.bind(this));
        },

        /**
         * initializes the tags for multiple options
         * @private
         */
        _setTags: function () {
            if (this.options.showtags) {

                // if the element is not there, create it
                if (this._tags.length === 0) {
                    this._tags = $('<ul/>', {
                        'class': 'taglist'
                    }).appendTo(this.$element);
                }

                this._tags.tagList(this.options.tagConfig || {});
                this._tagList = this._tags.data('tagList');

                this._input.on('keyup.autocomplete-addtag', this._addTag.bind(this));

            } else {
                this._input.off('keyup.autocomplete-addtag');
            }
        },

        /**
         * initializes the typeahead functionality
         * @fires Autocomplete#query
         * @private
         */
        _setTypeahead: function () {
            var self = this,
                timeout;

            function timeoutLoadFunc() {
                self.$element.trigger($.Event('query', {
                    value: self._input.val()
                }));
            }

            if (this.options.showtypeahead) {

                // bind keyboard input listening
                this._input.on('keyup.autocomplete', function (event) {
                    // debounce
                    if (timeout) {
                        clearTimeout(timeout);
                    }

                    timeout = setTimeout(timeoutLoadFunc, self.options.delay);
                });

            } else {
                this._input.off('keyup.autocomplete');
            }
        },

        _setSuggestions: function () {
            var self = this;

            if (this.options.showsuggestions) {

                // if the button to trigger the suggestion box is not there, 
                // then we add it
                if (this._suggestionsBtn.length === 0) {

                    this._suggestionsBtn = $('<button/>', {
                        'class': 'autocomplete-suggestion-toggle'
                    });

                    this._suggestionsBtn.appendTo(this.$element);
                }

                // handler to open usggestion box
                this._suggestionsBtn.fipo('tap', 'click', function (event) {
                    event.preventDefault();
                    self._toggleSuggestions();
                }).finger('click', false);

                // add class to input to to increase padding right for the button
                this._input.addClass('autocomplete-has-suggestion-btn');
            } else {
                this._suggestionsBtn.remove();
                this._input.removeClass('autocomplete-has-suggestion-btn');
            }
        },

        /*
        _setTypeahead: function () {
            var self = this,
                timeout;

            function timeoutLoadFunc() {
                self._selectListTypeahead.set('dataadditional', {
                    value: self._input.val()
                });
                self._selectListTypeahead.show();
                self._selectListTypeahead.triggerLoadData(true);
            }

            if (this.options.showtypeahead) {

                // if the element is not there, create it
                if (this._typeahead.length === 0) {
                    this._typeahead = $('<ul/>', {
                        'class': 'selectlist autocomplete-typeahead'
                    }).appendTo(this.$element);
                }

                this._typeahead.selectList($.extend({
                    relatedElement: this._input,
                    autofocus: false,
                    autohide: false
                }, this.options.typeaheadConfig || {}));

                this._selectListTypeahead = this._typeahead.data('selectList');

                // bind keyboard input listening
                this._input.on('keyup.autocomplete-typeahead', function (event) {
                    // debounce
                    if (timeout) {
                        clearTimeout(timeout);
                    }

                    timeout = setTimeout(timeoutLoadFunc, 500);
                });

            } else {
                this._input.off('keyup.autocomplete-typeahead');
            }
        },*/

        /**
         * adds a new tag when pressed button was Enter
         * @private
         * @param {jQuery.Event} event
         */
        _addTag: function (event) {
            if (event.which !== 13) {
                return;
            }

            this._tagList.addItem(this._input.val());
            this.clear();
        },

        _handleSelected: function (event) {
            this._selectListWidget.hide();
            
            if (this.options.showtags) {
                this._tagList.addItem(event.displayedValue);
            } else {
                this._input.val(event.displayedValue);
            }

            this._input.trigger('focus');
        },

        _toggleSuggestions: function () {
            this._selectListWidget.toggleVisibility();
        },

        _refreshClear: function () {
            this._clearBtn.toggleClass('hide', this._input.val().length === 0);
        },

        /**
         * handles a static list filter (type == static) based on the defined mode
         * @param  {jQuery.Event} event
         */
        handleStaticFilter: function (event) {
            this._selectList.find('[role="option"]').each(function (i, e) {

            });
        },

        /**
         * handles a static list filter (type == static) based on the defined mode
         * @param  {jQuery.Event} event
         */
        handleDynamicFilter: function (event) {
            this._selectListWidget.set('dataadditional', {
                value: event.value
            });
            this._selectListWidget.show();
            this._selectListWidget.triggerLoadData(true);
        },

        /**
         * clears the autocomplete input field
         */
        clear: function () {
            this._input.val('');
            this._refreshClear();
        },

        /**
         * disables the autocomplete
         */
        disable: function () {
            this.$element.addClass('disabled');
            this._input.prop('disabled', true);
            this._suggestionsBtn.prop('disabled', true);
        },

        /**
         * enables the autocomplete
         */
        enable: function () {
            this.$element.removeClass('disabled');
            this._input.prop('disabled', false);
            this._suggestionsBtn.prop('disabled', false);
        }
    });

    CUI.util.plugClass(CUI.Autocomplete);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (e) {
            $('[data-init~=autocomplete]', e.target).autocomplete();
        });
    }

}(jQuery, this));

(function ($, window, undefined) {
    CUI.Select = new Class(/** @lends CUI.Select# */{
        toString: 'Select',

        extend: CUI.Widget,
        
        /**
         * @extends CUI.Widget
         * @classdesc a widget which is similar to the native &lt;select&gt;
         *
         * <h2 class="line">Examples</h2>
         *
         * <span class="select" data-init="select">
         *     <button type="button">Select</button>
         *     <select>
         *         <option value="1">One</option>
         *         <option value="2">Two</option>
         *         <option value="3">Three</option>
         *     </select>
         * </span>
         *
         * <span class="select" data-init="select">
         *     <button type="button">Select</button>
         *     <select multiple="true">
         *         <option value="1">One</option>
         *         <option value="2">Two</option>
         *         <option value="3">Three</option>
         *     </select>
         * </span>
         *
         * @example
         * <caption>Instantiate with Class</caption>
         * var selectlist = new CUI.Select({
         *     element: '#mySelect'
         * });
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#mySelect').select({
         *
         * });
         *
         * @example
         * <caption>Data API: Instantiate, set options</caption>
         *
         * &lt;span class=&quot;select&quot; data-init=&quot;select&quot;&gt;
         *     &lt;button type=&quot;button&quot;&gt;Select&lt;/button&gt;
         *     &lt;select&gt;
         *         &lt;option value=&quot;1&quot;&gt;One&lt;/option&gt;
         *         &lt;option value=&quot;2&quot;&gt;Two&lt;/option&gt;
         *         &lt;option value=&quot;3&quot;&gt;Three&lt;/option&gt;
         *     &lt;/select&gt;
         * &lt;/span&gt;
         *
         * @description Creates a new select
         * @constructs
         *
         * @param {Object} options Component options
         * @param {Mixed} options.element jQuery selector or DOM element to use for panel
         * @param {String} [options.type=static] static or dynamic list
         * @param {Boolean} [nativewidget=false] shows a native &lt;select&gt; instead of a SelectList widget
         * @param {Boolean} [nativewidgetonmobile=true] forces a native &lt;select&gt; on a mobile device if possible
         * @param {Boolean} [multiple=false] multiple selection, will automatically be detected form a given &lt;select&gt; source
         */
        construct: function () {
            var self = this;

            // find elements
            this._button = this.$element.children('button');
            this._select = this.$element.children('select');
            this._selectList = this.$element.children('.selectlist');
            this._tagList = this.$element.children('.taglist');
            this._valueInput = this.$element.children('input[type=hidden]');

            // apply
            this.applyOptions();
        },

        defaults: {
            type: 'static',
            nativewidget: false,
            nativewidgetonmobile: true,
            multiple: false,
            tagConfig: null,
            selectlistConfig: null
        },

        applyOptions: function () {
            var forcedNativeWidget = this.options.nativewidgetonmobile && CUI.util.isTouch && this.options.type === 'static';

            // there is a select given so read the "native" config options
            if (this._select.length > 0) {
                // if multiple set multiple
                if (this._select.prop('multiple')) {
                    this.options.multiple = true;
                }
            }
            

            if (this.options.nativewidget || forcedNativeWidget) {
                this._setNativeWidget(forcedNativeWidget);
            } else {
                this._setSelectList();
            }

            this._setTagList();

            // if we have a static <select> based list
            // load the values from markup
            if (this.options.type === 'static') {
                this._handleNativeSelect();
            }
        },

        /**
         * this option is mainly supposed to be used on mobile
         * and will just work with static lists
         * @private
         * @param {Boolean} [force]
         */
        _setNativeWidget: function (force) {
            var self = this;

            if (this.options.nativewidget || force) {
                this._select.css({
                    display: 'block',
                    width: this._button.outerWidth(),
                    height: this._button.outerHeight(),
                    opacity: 0.01
                });

                this._select.position({
                    my: 'left top',
                    at: 'left top',
                    of: this._button
                });

                if (this.options.multiple) {
                    this._setTagList();
                }

                // if it is in single selection mode, 
                // then the btn receives the label of the selected item
                this._select.on('change.select', this._handleNativeSelect.bind(this));

            } else {
                this._select.off('change.select');
            }
        },

        /**
         * handles a native change event on the select
         * @private
         */
        _handleNativeSelect: function (event) {
            var self = this;

            if (self.options.multiple) {
                // loop over all options
                $.each(self._select[0].options, function (i, opt) {
                    if (opt.selected) {
                        self._tagListWidget.addItem({
                            value: opt.value,
                            display: opt.text
                        });
                    } else {
                        self._tagListWidget.removeItem(opt.value);
                    }
                });
            } else {
                self._button.text(self._select[0][self._select[0].selectedIndex].text);
            }
        },

        /**
         * this function parses the values from the native select
         * and prints the right markup for the SelectList widget
         * This function may only be called in SelectList widget mode.
         * @private
         */
        _parseMarkup: function () {
            var self = this,
                optgroup = this._select.children('optgroup');

            function parseGroup(parent, dest) {
                parent.children('option').each(function (i, e) {
                    var opt = $(e);

                    $('<li/>', {
                        'data-value': opt.val(),
                        'text': opt.text()
                    }).appendTo(dest);
                });
            }

            // optgroups are part of the select -> different markup
            if (optgroup.length > 0) {
                optgroup.each(function (i, e) {
                    var group = $(e),
                        entry = $('<li/>', {
                                'class': 'optgroup'
                            }).append($('<span/>', {
                                'text': group.attr('label')
                            }));

                    parseGroup(group, $('<ul/>').appendTo(entry));

                    self._selectList.append(entry);
                });
            } else { // flat select list
                parseGroup(this._select, this._selectList);
            }
        },

        /**
         * set SelectList widget
         * @private
         */
        _setSelectList: function () {
            var self = this,
                type = 'static';

            // if the element is not there, create it
            if (this._selectList.length === 0) {
                this._selectList = $('<ul/>', {
                    'class': 'selectlist'
                }).appendTo(this.$element);
            }

            // read values from markup
            if (this._select.length > 0) {
                this._parseMarkup();
            } else { // if no <select> wa found then a dynamic list is expected
                type = 'dynamic';
            }

            this._selectList.selectList($.extend({
                relatedElement: this._button,
                type: type
            }, this.options.selectlistConfig || {}));

            this._selectListWidget = this._selectList.data('selectList');

            // handler to open usggestion box
            this._button.fipo('tap', 'click', function (event) {
                event.preventDefault();
                self._toggleList();
            }).finger('click', false);

            this._selectList
                // receive the value from the list
                .on('selected.select', this._handleSelected.bind(this))
                // handle open/hide for the button
                .on('show.dropdown hide.select', function (event) {
                    self._button.toggleClass('active', event.type === 'show');
                });
        },

        /**
         * sets a tag list for the multiple selection
         * @private
         */
        _setTagList: function () {
            if (this.options.multiple) {
                // if the element is not there, create it
                if (this._tagList.length === 0) {
                    this._tagList = $('<ol/>', {
                        'class': 'taglist'
                    }).appendTo(this.$element);
                }

                this._tagList.tagList(this.options.tagConfig || {});

                this._tagListWidget = this._tagList.data('tagList');
            }
        },

        /**
         * handles a select of a SelectList widget
         * @private
         */
        _handleSelected: function (event) {
            this._selectListWidget.hide();

            // set select value
            this._select.val(event.selectedValue);

            if (this.options.multiple) {
                this._tagListWidget.addItem({
                    value: event.selectedValue,
                    display: event.displayedValue
                });
            } else {
                // set the button label
                this._button.text(event.displayedValue);
                // in case it is dynamic a value input should be existing
                this._valueInput.val(event.selectedValue);
            }

            this._button.trigger('focus');
        },

        /**
         * toggles the visibility of a SelectList widget
         * @private
         */
        _toggleList: function () {
            this._selectListWidget.toggleVisibility();
        }
    });

    CUI.util.plugClass(CUI.Select);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (e) {
            $('[data-init~=select]', e.target).select();
        });
    }

}(jQuery, this));

(function ($, window, undefined) {
    var storageKey = 'cui-state',
        storageLoadEvent = 'cui-state-restore',
        store = {},
        loaded = false,
        $doc = $(document);

    /**
     * state object to enable UI page refresh stable states
     * TODO:
     *  - all states are global, lack of an auto restore mode which is aware of the URL
     *  - client side only (localStorage)
     *  - lack of an abstraction layer for the client side storage
     * @type {Object}
     */
    CUI.util.state = {

        /*saveForm: function (form, elem) {

        },*/

        config: {
            serverpersistence: true
        },

        /**
         * Persist attributes of a DOM node
         *
         * @param {String} selector
         * @param {String|Array} [attribute] single attribute or list of attributes to be saved. If null then all attributes will be saved
         * @param {Boolean} [autorestore]
         * @param {String} [customEvent] custom event name
         */
        save: function (selector, attribute, autorestore, customEvent) {
            var elem = $(selector),
                saveLoop = function (i, attr) {
                    store.global[selector] = store.global[selector] || {};
                    store.global[selector][attr] = store.global[selector][attr] || {};
                    store.global[selector][attr].val = elem.attr(attr);
                    store.global[selector][attr].autorestore = autorestore || false;
                    store.global[selector][attr].customEvent = customEvent || null;
                };

            
            if (attribute) { // save single or multiple attributes
                if ($.isArray(attribute)) { // multiple values to save
                    $.each(attribute, saveLoop);
                } else { // save all attributes
                    saveLoop(0, attribute);
                }
            } else { // save all attributes
                // TODO
                // not supported yet because the browser implementation of Node.attributes is a mess
                // https://developer.mozilla.org/en-US/docs/DOM/Node.attributes
            }

            localStorage.setItem(storageKey, JSON.stringify(store));
            
            if (CUI.util.state.config.serverpersistence) {
                $.cookie(storageKey, JSON.stringify(store), {
                    expires: 7,
                    path: '/'
                });
            }
        },

        /**
         *
         * @param {String} [selector]
         * @param {Function} [filter] filter function for the attributes of the given selector
         */
        restore: function (selector, filter) {
            var check = filter || function () {
                    return true;
                },
                sel,
                elem,
                selectorLoop = function (item, noop) {
                    sel = item;
                    elem = $(sel);

                    if (store.global[sel]) {
                        $.each(store.global[sel], restoreLoop);
                    }
                },
                restoreLoop = function (attr, obj) {
                    if (check(sel, attr, obj)) {
                        elem.attr(attr, obj.val);

                        if (obj.customEvent) {
                            $doc.trigger(obj.customEvent, [elem, obj]);
                        }

                        $doc.trigger(storageLoadEvent, [elem, obj]);
                    }
                };

            if (!loaded) {
                loaded = CUI.util.state.load();
            }

            
            if (selector) { // restore single selector
                selectorLoop(selector);
            } else { // restore everything
                $.each(store.global, selectorLoop);
            }
        },

        load: function () {
            var val = localStorage.getItem(storageKey);

            store = val ? JSON.parse(val) : {
                global: {}
            };

            return true;
        },

        // support for "temporary" storage that will be automatically cleared if
        // the browser session ends; currently uses a set/get pattern rather than
        // loading the entire thing on document ready. Also note that the data is currently
        // not sent to the server.

        setSessionItem: function(name, value, ns) {
            var key = name;
            if (ns) {
                key = name + ":" + ns;
            }
            sessionStorage.setItem(key, JSON.stringify(value));
        },

        getSessionItem: function(name, ns) {
            var key = name;
            if (ns) {
                key = name + ":" + ns;
            }
            var value = sessionStorage.getItem(key);
            if (value) {
                value = JSON.parse(value);
            }
            return value;
        },

        removeSessionItem: function(name, ns) {
            var key = name;
            if (ns) {
                key = name + ":" + ns;
            }
            sessionStorage.removeItem(key);
        },

        clearSessionItems: function(ns) {
            if (ns) {
                ns = ":" + ns;
                var keyCnt = sessionStorage.length;
                var toRemove = [ ];
                for (var k = 0; k < keyCnt; k++) {
                    var keyToCheck = sessionStorage.key(k);
                    var keyLen = keyToCheck.length;
                    if (keyLen > ns.length) {
                        if (keyToCheck.substring(keyLen - ns.length) === ns) {
                            toRemove.push(keyToCheck);
                        }
                    }
                }
                var removeCnt = toRemove.length;
                for (var r = 0; r < removeCnt; r++) {
                    sessionStorage.removeItem(toRemove[r]);
                }
            }
        }

    };

    $doc.ready(function () {
        CUI.util.state.restore(null, function (selector, attr, val) {
            if (val.autorestore) {
                return true;
            }

            return false;
        });
    });
}(jQuery, this));
/**
 HTTP Utility functions used by CoralUI widgets

 @namespace
 */
CUI.util.HTTP = {
    /**
     * Checks whether the specified status code is OK.
     * @static
     * @param {Number} status The status code
     * @return {Boolean} True if the status is OK, else false
     */
    isOkStatus: function(status) {
        try {
            return (String(status).indexOf("2") === 0);
        } catch (e) {
            return false;
        }
    },

    /**
     * Returns <code>true</code> if HTML5 Upload is supported
     * @return {Boolean} HTML5 Upload support status
     */
    html5UploadSupported: function() {
        var xhr = new XMLHttpRequest();
        return !! (
            xhr && ('upload' in xhr) && ('onprogress' in xhr.upload)
        );
    }

};

(function ($, window, undefined) {
/**
 HTTP Utility functions used by CoralUI colorpicker for color transformation

 @namespace
 */
CUI.util.color = {
    
    /**
     * Transforms a string color or part of color (r,g,b) into a hexa value
     * @static
     * @param {String} x The string color or part of color
     * @return {String} Hexa representation
     */
    hex : function (x) {
        return ("0" + parseInt(x, 10).toString(16)).slice(-2);
    },
    
    /**
     * Transforms a hexa color into RGB representation
     * @static
     * @param {String} hex The string color hexa representation
     * @return {Object} {r, g, b}
     */
    HexToRGB : function(hex) {
        hex = parseInt(((hex.indexOf("#") > -1) ? hex.substring(1) : hex), 16);
        return {
            r : hex >> 16,
            g : (hex & 0x00FF00) >> 8,
            b : (hex & 0x0000FF)
        };
    },
    
    /**
     * Transforms a rgba color into RGB representation
     * @static
     * @param {String} hex The string color rgba representation
     * @return {String} Hexa representation of the color
     */
    RGBAToHex : function(rgbaVal) {
        var rgba = rgbaVal.substring(rgbaVal.indexOf('(') + 1, rgbaVal.lastIndexOf(')')).split(/,\s*/);
        return '#' + this.hex(rgba[0]) + this.hex(rgba[1]) + this.hex(rgba[2]);
    },
    
    /**
     * Transforms a rgb color into hexa representation
     * @static
     * @param {Object} {r, g, b}
     * @return {String} The string color hexa representation
     */
    RGBToHex : function(rgb) {
        return '#' + this.hex(rgb.r) + this.hex(rgb.g) + this.hex(rgb.b);
    },
    
    /**
     * Transforms a cmyk color into RGB representation
     * @static
     * @param {Object} {c, m, y, k}
     * @return {Object} {r, g, b}
     */
    CMYKtoRGB : function (cmyk){
        var result = {r:0, g:0, b:0};
 
        var c = parseInt(cmyk.c, 10) / 100;
        var m = parseInt(cmyk.m, 10) / 100;
        var y = parseInt(cmyk.y, 10) / 100;
        var k = parseInt(cmyk.k, 10) / 100;
 
        result.r = 1 - Math.min( 1, c * ( 1 - k ) + k );
        result.g = 1 - Math.min( 1, m * ( 1 - k ) + k );
        result.b = 1 - Math.min( 1, y * ( 1 - k ) + k );
 
        result.r = Math.round( result.r * 255 );
        result.g = Math.round( result.g * 255 );
        result.b = Math.round( result.b * 255 );
 
        return result;
    },
 
    /**
     * Transforms a rgb color into cmyk representation
     * @static
     * @param {Object} {r, g, b}
     * @return {Object} {c, m, y, k}
     */
    RGBtoCMYK : function (rgb){
        var result = {c:0, m:0, y:0, k:0};
        
        if (parseInt(rgb.r, 10) === 0 && parseInt(rgb.g, 10) === 0 && parseInt(rgb.b, 10) === 0) {
            result.k = 100;
            return result;
        }
 
        var r = parseInt(rgb.r, 10) / 255;
        var g = parseInt(rgb.g, 10) / 255;
        var b = parseInt(rgb.b, 10) / 255;
 
        result.k = Math.min( 1 - r, 1 - g, 1 - b );
        result.c = ( 1 - r - result.k ) / ( 1 - result.k );
        result.m = ( 1 - g - result.k ) / ( 1 - result.k );
        result.y = ( 1 - b - result.k ) / ( 1 - result.k );
 
        result.c = Math.round( result.c * 100 );
        result.m = Math.round( result.m * 100 );
        result.y = Math.round( result.y * 100 );
        result.k = Math.round( result.k * 100 );
 
        return result;
    },
    
    /**
     * Corrects a hexa value, if it is represented by 3 or 6 characters with or without '#'
     * @static
     * @param {String} hex The string representation of the hexa value
     * @return {String} Hexa corrected string or empty string if tghe hex value is not valid
     */
    fixHex : function(hex) {
        if (hex.length === 3) {
            hex = hex.charAt(0) + hex.charAt(0) + hex.charAt(1) +
                    hex.charAt(1) + hex.charAt(2) + hex.charAt(2);
        }
        if (hex.indexOf("#") === -1) {
            hex = "#" + hex;
        }
        var isOk = /(^#[0-9A-F]{6})|(^#[0-9A-F]{3})$/i.test(hex);
        if (!isOk) {
            this.$element.find("[name=':hex']").val("");
            return "";
        }

        return hex;
    },
    
    /**
     * Compares string representations of 2 colors
     * @static
     * @param {String} c1 The string representation of the first color
     * @param {String} c2 The string representation of the seccond color
     * @return {Boolean} True if they are equal, false otherwise
     */
    isSameColor : function(c1, c2) {
        return c1 && c2 && (c1 === c2);
    },
    
    isValid : function(colorFormat, colorAsStr){
        if(colorAsStr.indexOf(colorFormat) != -1){
            return this.fixHex(this.RGBAToHex(colorAsStr)) !== "";
        }
        return false;
    }

};

}(jQuery, this));
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

(function ($, window, undefined) {

    CUI.Modal = new Class(/** @lends CUI.Modal# */{
        toString: 'Modal',

        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc A dialog that prevents interaction with page elements while displayed. Modal will use existing markup if it is present, or create markup if <code>options.element</code> has no children.
         *
         * <h2 class="line">Example</h2>
         *     <a href="#myModal" class="button" data-toggle="modal">Show Modal</a>
         *     <div id="myModal" class="modal">
         *         <div class="modal-header">
         *         <h2>A Sample Modal</h2>
         *         <button type="button" class="close" data-dismiss="modal">&times;</button>
         *     </div>
         *     <div class="modal-body">
         *         <p>Some sample content.</p>
         *     </div>
         *     <div class="modal-footer">
         *         <button data-dismiss="modal">Ok</button>
         *     </div>
         * </div>
         *
         * @example
         * <caption>Instantiate with Class</caption>
         * var modal = new CUI.Modal({
         *     element: '#myModal',
         *     header: 'My Modal',
         *     content: '&lt;p&gt;Content here.&lt;/p&gt;',
         *     buttons: [{
         *         label: 'Save',
         *         className: 'primary',
         *         click: function(evt) {
         *             console.log('Modal: This would usually trigger a save...');
         *             this.hide(); // could also use evt.dialog.hide();
         *         }
         *     }]
         * });
         *
         * // Hide the modal, change the heading, then show it again
         * modal.hide().set({ header: 'My Modal Again'}).show();
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#myModal').modal({
         *     header: 'My Modal',
         *     content: '&lt;p&gt;Content here.&lt;/p&gt;',
         *     buttons: [{
         *         label: 'Close',
         *         click: 'hide', // Specifying 'hide' causes the dialog to close when clicked
         *     }]
         * });
         *
         * // jQuery style works as well
         * $('#myModal').modal('hide');
         *
         * // A reference to the element's modal instance is stored as data-modal
         * var modal = $('#myModal').data('modal');
         * modal.hide();
         *
         * @example 
         * <caption>Data API: Instantiate and show modal</caption>
         * <description>When using a <code class="prettify">&lt;button&gt;</code>, specify the jQuery selector for the element using <code>data-target</code>. Markup should exist already if no options are specified.</description>
         * &lt;button data-target=&quot;#myModal&quot; data-toggle=&quot;modal&quot;&gt;Show Modal&lt;/button&gt;
         *
         * @example
         * <caption>Data API: Instantiate, set options, and show</caption>
         * <description>When using an <code class="prettify">&lt;a&gt;</code>, specify the jQuery selector for the element using <code>href</code>. Markup is optional since options are specified as data attributes.</description>
         * &lt;a 
         *     href=&quot;#modal&quot;
         *     data-toggle=&quot;modal&quot;
         *     data-heading=&quot;Test Modal&quot;
         *     data-content=&quot;&amp;lt;p&amp;gt;Test content&amp;lt;/p&amp;gt;&quot;
         *     data-buttons=&#x27;[{ &quot;label&quot;: &quot;Close&quot;, &quot;click&quot;: &quot;close&quot; }]&#x27;
         * &gt;Show Modal&lt;/a&gt;
         *
         * @example
         * <caption>Data API: Instantiate, load content asynchronously, and show</caption>
         * <description>When loading content asynchronously, regardless of what tag is used, specify the jQuery selector for the element using <code>data-target</code> and the URL of the content to load with <code>href</code>.</description>
         * &lt;button
         *     data-target="#myModal"
         *     data-toggle=&quot;modal&quot;
         *     href=&quot;content.html&quot;
         * &gt;Show Modal&lt;/button&gt;
         *
         * @example
         * <caption>Markup</caption>
         * &lt;h2 class=&quot;line&quot;&gt;Example&lt;/h2&gt;
         *     &lt;a href=&quot;#myModal&quot; class=&quot;button&quot; data-toggle=&quot;modal&quot;&gt;Show Modal&lt;/a&gt;
         *     &lt;div id=&quot;myModal&quot; class=&quot;modal&quot;&gt;
         *         &lt;div class=&quot;modal-header&quot;&gt;
         *         &lt;h2&gt;A Sample Modal&lt;/h2&gt;
         *         &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;modal&quot;&gt;&amp;times;&lt;/button&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-body&quot;&gt;
         *         &lt;p&gt;Some sample content.&lt;/p&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-footer&quot;&gt;
         *         &lt;button data-dismiss=&quot;modal&quot;&gt;Ok&lt;/button&gt;
         *     &lt;/div&gt;
         * &lt;/div&gt;
         *
         * @example
         * <caption>Markup with &lt;form&gt; tag</caption>
         * <description>Modals can be created from the <code class="prettify">&lt;form&gt;</code> tag as well. Make sure to set <code class="prettify">type="button"</code> on buttons that should not perform a submit.</description>
         * &lt;form id=&quot;myModal&quot; class=&quot;modal&quot; action="/users" method="post"&gt;
         *     &lt;div class=&quot;modal-header&quot;&gt;
         *         &lt;h2&gt;Create User&lt;/h2&gt;
         *             &lt;button type=&quot;button&quot; class=&quot;close&quot; data-dismiss=&quot;modal&quot;&gt;&amp;times;&lt;/button&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-body&quot;&gt;
         *         &lt;label for=&quot;name&quot;&gt;Name&lt;/label&gt;&lt;input id=&quot;name&quot; name=&quot;name&quot; type=&quot;text&quot;&gt;
         *     &lt;/div&gt;
         *     &lt;div class=&quot;modal-footer&quot;&gt;
         *         &lt;button type="button" data-dismiss=&quot;modal&quot;&gt;Cancel&lt;/button&gt;
         *         &lt;button type="submit"&gt;Submit&lt;/button&gt;
         *     &lt;/div&gt;
         * &lt;/form&gt;
         * 
         * 
         * @desc Creates a new modal dialog
         * @constructs
         * @param {Object} options Component options
         * @param {Mixed} options.element jQuery selector or DOM element to use for dialog
         * @param {String} options.header Title of the modal dialog (HTML)
         * @param {String} options.content Title of the modal dialog (HTML)
         * @param {String} [options.type=default] Type of dialog to display. One of default, error, notice, success, help, or info
         * @param {Array} [options.buttons] Array of button descriptors
         * @param {String} [options.buttons.label] Button label (HTML)
         * @param {String} [options.buttons.className] CSS class name to apply to the button
         * @param {Mixed} [options.buttons.click] Click handler function or string 'hide' to hide the dialog
         * @param {String} [options.remote] URL to asynchronously load content from the first time the modal is shown
         * @param {Mixed} [options.backdrop=static] False to not display transparent underlay, True to display and close when clicked, 'static' to display and not close when clicked
         * @param {Mixed} [options.visible=true] True to display immediately, False to defer display until show() called
         * 
         */
        construct: function (options) {
            // @deprecated, rather the template engine should be agonistic
            // Render template, if necessary
            // disabled for now
            if (this.$element.children().length === 0) {
                this.$element.html(CUI.Templates['modal']($.extend({}, this.options, { buttons: '' })));
            }

            // @deprecated adding a styling class blocks the reusability
            // of the modal's javascript. This will be removed in future
            // add styling
            this.$element.addClass('modal');

            // modal parts
            this.header = this.$element.find('.modal-header');
            this.body = this.$element.find('.modal-body');
            this.footer = this.$element.find('.modal-footer');

            // previous focus element
            this._previousFocus = $();

            // creates a backdrop object
            // but it does not attach it to the document
            this.backdrop = $('<div/>', {
                'class': 'modal-backdrop',
                'style': 'display: none;'
            }).fipo('tap', 'click', function (event) {
                if (this.options.backdrop !== 'static') {
                    this.hide();
                }
            }.bind(this));

            // Fetch content asynchronously, if remote is defined
            this.body.loadWithSpinner(this.options.remote);

            this.applyOptions();

            this.$element.on('change:heading', this._setHeading.bind(this)) // @deprecated
                .on('change:header', this._setHeader.bind(this))
                .on('change:content', this._setContent.bind(this))
                .on('change:buttons', this._setFooter.bind(this))
                .on('change:type', this._setType.bind(this))
                .on('change:fullscreen', this._setFullscreen.bind(this))
                // close when a click was fired on a close trigger (e.g. button)
                .fipo('tap', 'click', '[data-dismiss="modal"]', this.hide.bind(this));

            this._makeAccessible();
        },

        defaults: {
            backdrop: 'static',
            visible: true,
            type: 'default',
            fullscreen: false,
            attachToBody: true
        },

        _types: [
            'default',
            'error',
            'notice',
            'success',
            'help',
            'info'
        ],

        applyOptions: function () {
            this._setHeader();
            this._setHeading();  // @deprecated
            this._setContent();
            this._setFooter();
            this._setType();
            this._setFullscreen();

            if (this.options.visible) {
                // Show immediately
                this.options.visible = false;
                this.show();
            }
        },

        /**
         * adds some accessibility attributes and features
         * http://www.w3.org/WAI/PF/aria-practices/#dialog_modal
         * @private
         */
        _makeAccessible: function () {
            var self = this,
                idPrefix = 'modal-header' + new Date().getTime() + '-';

            // the element has the role dialog
            this.$element.attr({
                'role': 'dialog',
                'aria-hidden': !this.options.visible,
                'aria-labelledby': idPrefix + 'label',
                'aria-describedby': idPrefix + 'message',
                'tabindex': -1
            });

            this.header.find('h2').attr({
                'id': idPrefix + 'label',
                'tabindex': 0
            });

            // Message areas have role document and tabindex="0"
            this.body.attr({
                'id': idPrefix + 'message',
                'role': 'document',
                'tabindex': 0
            });

            // keyboard handling
            this.$element.on('keydown', ':focusable', function (event) {
                // enables keyboard support

                var elem = $(event.currentTarget),
                    tabbables = self.$element.find(':tabbable'),
                    focusElem;

                switch (event.which) {
                    case 9: //tab
                        if (event.shiftKey && event.currentTarget === tabbables[0]) {
                            // in case it is the first one, we switch to the last one
                            focusElem = tabbables.last();
                        } else if (!event.shiftKey && event.currentTarget === tabbables[tabbables.length-1]) {
                            // in case it is the last one, we switch to the first one
                            focusElem = tabbables.first();
                        }
                        break;
                }

                if (focusElem) { // if a key matched then we set the currently focused element
                    event.preventDefault();
                    focusElem.trigger('focus');
                }
            });
        },

        /**
         * sets the type of the modal
         * @private
         */
        _setType: function () {
            if (!this.options.type) {
                return;
            }

            // Remove old type
            this.$element.removeClass(this._types.join(' '));

            // Add new type
            if (this.options.type !== 'default') {
                this.$element.addClass(this.options.type);
            }
        },

        /**
         * sets the header of the modal
         * @private
         */
        _setHeader: function () {
            if (!this.options.header) {
                return;
            }

            this.header.find('h2').html(this.options.header);
        },

        /**
         * @deprecated rather use #_setHeader
         * @private
         */
        _setHeading: function () {
            if (!this.options.heading) {
                return;
            }

            this.options.header = this.options.heading;
            this._setHeader.apply(this, arguments);
        },

        /**
         * sets the content of the modal body
         * @private
         */
        _setContent: function () {
            if (!this.options.content) {
                return;
            }

            this.body.html(this.options.content);
        },

        /**
         * sets the buttons into the footer from the config
         * @private
         */
        _setFooter: function () {
            if (!$.isArray(this.options.buttons)) {
                return;
            }

            var self = this;

            // remove existing buttons
            this.footer.empty();

            $.each(this.options.buttons, function(idx, button) {
                // Create an anchor if href is provided
                var btn = button.href ? $('<a/>', {
                        'class': 'button'
                    }) : $('<button/>', {
                        'type': 'button'
                    });

                // Add label
                btn.html(button.label);

                // attach event handler
                if (button.click === 'hide') {
                    btn.attr('data-dismiss', 'modal');
                } else if ($.isFunction(button.click)) {
                    btn.fipo('tap', 'click', button.click.bind(self, {
                        dialog: self
                    }));
                }

                if (button.href) {
                    btn.attr('href', button.href);
                }

                if (button.className) {
                    btn.addClass(button.className);
                }

                self.footer.append(btn);
            });
        },

        /**
         * sets the fullscreen css class
         * @private
         */
        _setFullscreen: function () {
            if (this.options.fullscreen) {
                this.$element.addClass('fullscreen');
            } else {
                this.$element.removeClass('fullscreen');
            }
        },

        /**
         * @private
         * @event beforeshow
         */
        _show: function () {
            var body = $('body'),
                tabcapture,
                self = this;

            // ARIA: http://www.w3.org/WAI/PF/aria-practices/#dialog_modal
            // When the dialog is closed or cancelled focus should 
            // return to the element in the application which had focus 
            // before the dialog is invoked
            this._previousFocus = $(':focus'); //save previously focused element
            this._previousFocus.trigger('blur');

            body.addClass('modal-open');

            // center before showing
            this.center();

            // fire event before showing the modal
            this.$element.trigger('beforeshow');

            this._toggleBackdrop(true);

            // Move to the bottom of body so we're outside of any relative/absolute context
            // This allows us to know we'll always float above the backdrop
            if (this.options.attachToBody) {
                if (this.$element.parent('body').length === 0) {
                    this.$element.appendTo(body);
                }
                // ARIA
                // Hide sibling elements from assistive technologies, 
                // but first store the state of any siblings already have the aria-hidden attribute 
                this.$element.siblings('[aria-hidden]').each(function(index, element) {
                    $(element).data('aria-hidden', $(element).attr('aria-hidden'));
                });            
                this.$element.siblings().not('script, link, style').attr('aria-hidden', this.options.visible);
            }

            this.$element.attr('aria-hidden', !this.options.visible);

            // fadeIn
            this.$element.fadeIn();

            // When a modal dialog opens focus goes to the first focusable item in the dialog
            this.$element.find(':tabbable:not(.modal-header .close):first').focus();
            
            // add tab-focusable divs to capture and forward focus to the modal dialog when page regains focus
            tabcapture = $('<div class="cui-modal-tabcapture" tabindex="0"/>');
            tabcapture.on('focus.modal-tabcapture', function(event) {
                var tabbables = self.$element.find(':tabbable'),
                    tabcaptures = $('body > .cui-modal-tabcapture'),
                    lasttabcapture  = tabcaptures.last(),
                    focusElem;
                
                if (event.currentTarget === lasttabcapture[0]) {
                    focusElem = tabbables.filter(':not(.modal-header .close):last');
                } else {
                    focusElem = tabbables.filter(':not(.modal-header .close):first');
                }

                if (focusElem.length === 0) {
                    focusElem = self.$element;
                }

                focusElem.trigger('focus');
            })
            .prependTo(body)
            .clone(true)
            .appendTo(body);

            // add escape handler
            $(document).on('keydown.modal-escape', this._escapeKeyHandler.bind(this));

            return this;
        },

        /**
         * @private
         * @event beforehide
         */
        _hide: function () {
            $('body').removeClass('modal-open')
                .find('.cui-modal-tabcapture').off('focus.modal-tabcapture').remove();

            // remove escape handler
            $(document).off('keydown.modal-escape');

            // fire event before showing the modal
            this.$element.trigger('beforehide');

            this._toggleBackdrop(false);

            this.$element.attr('aria-hidden', !this.options.visible);
            
            this.$element.siblings()
                .removeAttr('aria-hidden')
                .filter(':data("aria-hidden")')
                .each(function(index, element) {
                    $(element).attr('aria-hidden', $(element).data('aria-hidden'))
                        .removeData('aria-hidden');
                });

            // fadeOut
            this.$element.fadeOut().trigger('blur');

            // ARIA: http://www.w3.org/WAI/PF/aria-practices/#dialog_modal
            // When the dialog is closed or cancelled focus should 
            // return to the element in the application which had focus 
            // before the dialog is invoked
            this._previousFocus.trigger('focus');

            return this;
        },

        /**
         * centers the modal in the middle of the screen
         * @returns {CUI.Modal} this, chainable
         */
        center: function () {
            var width = this.$element.outerWidth(),
                height = this.$element.outerHeight();

            this.$element.css({
                'margin-left': - (width / 2),
                'margin-top': - (height / 2)
            });
        },

        /**
         * toggles back drop
         * @private
         * @param  {Boolean} [show] true/false to force state
         */
        _toggleBackdrop: function (show) {
            if (!this.options.backdrop) {
                return;
            }

            var body = $('body');

            if ((show || this.backdrop.is(':hidden')) && show !== false) {
                this.backdrop.appendTo(body).fadeIn();
            }
            else {
                this.backdrop.fadeOut(function () {
                    $(this).detach();
                });
            }
        },

        /**
         * handler to close the dialog on escape key
         * @private
         */
        _escapeKeyHandler: function (event) {
            if (event.which === 27) {
                this.hide();
            }

        }
    });

    CUI.util.plugClass(CUI.Modal);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (event) {
            // initialize the modal dialogs
            $('[data-init~=modal]', event.target).modal();
        });


        // @deprecated
        // this differs from other components
        // rather in future we use data-init~="modal-trigger" to intialize a trigger
        // and require data-init~="modal" on the modal to indicate it is a modal
        $(document).fipo('tap.modal.data-api', 'click.modal.data-api', '[data-toggle="modal"]', function (e) {
            var $trigger = $(this);

            // Get the target from data attributes
            var $target = CUI.util.getDataTarget($trigger);

            // Pass configuration based on data attributes in the triggering link
            var href = $trigger.attr('href');
            var options = $.extend({ remote: !/#/.test(href) && href }, $target.data(), $trigger.data());

            // Parse buttons
            if (typeof options.buttons === 'string') {
                options.buttons = JSON.parse(options.buttons);
            }

            // If a modal already exists, show it
            var instance = $target.data('modal');
            var show = true;
            if (instance && instance.get('visible'))
            show = false;

            // Apply the options from the data attributes of the trigger
            // When the dialog is closed, focus on the button that triggered its display
            $target.modal(options);

            // Perform visibility toggle if we're not creating a new instance
            if (instance)
            $target.data('modal').set({ visible: show });

            // Stop links from navigating
            e.preventDefault();
        }).finger('click.modal.data-api', '[data-toggle="modal"]', false);
    }

}(jQuery, this));

(function ($, window, undefined) {
    CUI.Tabs = new Class(/** @lends CUI.Tabs# */{
        toString: 'Tabs',

        extend: CUI.Widget,

        /**
         * @extends CUI.Widget
         * @classdesc A tabbed panel with several variants. A tabs instance ($.tabs or new CUI.Tabs) is not needed for basic functionality, only if programmatic access is necessary.
         *
         * <h2 class="line">Examples</h2>
         *
         * <h3>Default</h3>
         * <div class="tabs" data-init="tabs">
         *     <nav>
         *         <a href="#" data-toggle="tab" class="active">Tab 1</a>
         *         <a href="#" data-toggle="tab">Tab 2</a>
         *         <a href="../examples/remote.html" data-target="#" data-toggle="tab">Tab 3</a>
         *         <a href="#" data-toggle="tab" class="disabled">Disabled Tab</a>
         *     </nav>
         *     <section class="active">Lorizzle ipsizzle fo shizzle mah nizzle fo rizzle.</section>
         *     <section>Nulla gangsta. Brizzle shizzlin dizzle pharetra.</section>
         *     <section>This will be replaced :)</section>
         *     <section>This section will never be shown :(</section>
         * </div>
         *
         * <h3>White</h3>
         * <div class="tabs white" data-init="tabs">
         *     <nav>
         *         <a href="#" data-toggle="tab" class="active">Tab 1</a>
         *         <a href="#" data-toggle="tab">Tab 2</a>
         *         <a href="#" data-toggle="tab" class="disabled">Disabled Tab</a>
         *     </nav>
         *     <section class="active">Lorizzle ipsizzle fo shizzle mah nizzle fo rizzle.</section>
         *     <section>Nulla gangsta. Brizzle shizzlin dizzle pharetra.</section>
         *     <section>This section will never be shown :(</section>
         * </div>
         *
         * <h3>Stacked</h3>
         * <div class="tabs stacked" data-init="tabs">
         *     <nav>
         *         <a href="#" data-toggle="tab" class="active">Tab 1</a>
         *         <a href="#" data-toggle="tab">Tab 2</a>
         *         <a href="#" data-toggle="tab" class="disabled">Disabled Tab</a>
         *     </nav>
         *     <section class="active">Lorizzle ipsizzle fo shizzle mah nizzle fo rizzle.</section>
         *     <section>Nulla gangsta. Brizzle shizzlin dizzle pharetra.</section>
         *     <section>This section will never be shown :(</section>
         * </div>
         *
         * <h3>Nav</h3>
         * <div class="tabs nav" data-init="tabs">
         *     <nav>
         *         <a href="#" data-toggle="tab" class="active">Tab 1</a>
         *         <a href="#" data-toggle="tab">Tab 2</a>
         *         <a href="#" data-toggle="tab" class="disabled">Disabled Tab</a>
         *     </nav>
         *     <section class="active">Lorizzle ipsizzle fo shizzle mah nizzle fo rizzle.</section>
         *     <section>Nulla gangsta. Brizzle shizzlin dizzle pharetra.</section>
         *     <section>This section will never be shown :(</section>
         * </div>
         *
         * @example
         * <caption>Instantiate with Class</caption>
         * var tabs = new CUI.Tabs({
         *     element: '#myTabs'
         * });
         *
         * // Hide the tabs, set the active tab, and show it again
         * tabs.hide().set({active: 'second-tab'}).show();
         *
         * @example
         * <caption>Instantiate with jQuery</caption>
         * $('#myTabs').tabs({
         *     type: 'stacked'
         * });
         *  
         * // jQuery style works as well for show/hide
         * $('#myTabs').tabs('show');
         *
         * // A reference to the element's tabs instance is stored as data-tabs
         * var tabs = $('#myTabs').data('tabs');
         * tabs.hide();
         *
         * @example
         * <caption>Data API: Instantiate, set options, and show</caption>
         * <description>you do not need to explicitly instantiate a tabs instance to use the tabs functionality. The data API will handle switching between tabs as long as you have created a <code class="prettify">&lt;div&gt;</code> with the <code class="prettify">tabs</code> class. When using markup to instantiate tabs, the overall container is <code class="prettify">div class=&quot;tabs&quot</code>. The tabs themselves are specified within the <code>nav</code> block as simple <code class="prettify">a</code> elements. The <code class="prettify">data-toggle=&quot;tab&quot;</code> attribute on <code>a</code> nav links is essential for the data API; do not omit. The <code>href</code> can either be an id of a following <code>section</code>, a simple anchor: <code>#</code>, or a remote link (see next example).</description>
         * &lt;div class=&quot;tabs&quot; data-init=&quot;tabs&quot;&gt;
         *     &lt;nav&gt;
         *         &lt;a href=&quot;#&quot; data-toggle=&quot;tab&quot; class=&quot;active&quot;&gt;Tab 1&lt;/a&gt;
         *         &lt;a href=&quot;#&quot; data-toggle=&quot;tab&quot;&gt;Tab 2&lt;/a&gt;
         *         &lt;a href=&quot;../examples/remote.html&quot; data-target=&quot;#&quot; data-toggle=&quot;tab&quot;&gt;Tab 3&lt;/a&gt;
         *         &lt;a href=&quot;#&quot; data-toggle=&quot;tab&quot; class=&quot;disabled&quot;&gt;Disabled Tab&lt;/a&gt;
         *     &lt;/nav&gt;
         *     &lt;section class=&quot;active&quot;&gt;Lorizzle ipsizzle fo shizzle mah nizzle fo rizzle.&lt;/section&gt;
         *     &lt;section&gt;Nulla gangsta. Brizzle shizzlin dizzle pharetra.&lt;/section&gt;
         *     &lt;section&gt;This will be replaced :)&lt;/section&gt;
         *     &lt;section&gt;This section will never be shown :(&lt;/section&gt;
         * &lt;/div&gt; 
         *
         * @example
         * <caption>Variants</caption>
         * <description>The possible variants, <code class="prettify">white</code>, <code class="prettify">stacked</code>, and <code class="prettify">nav</code>, are specified either via the <code>type</code> argument to the constructor, or via manually specifying the class alongside <code>tabs</code>.</description>
         * &lt;div class=&quot;tabs nav&quot; data-init=&quot;tabs&quot;&gt;
         *     &lt;nav&gt;
         *         &lt;a href=&quot;#&quot; data-toggle=&quot;tab&quot; class=&quot;active&quot;&gt;Tab 1&lt;/a&gt;
         *         &lt;a href=&quot;#&quot; data-toggle=&quot;tab&quot;&gt;Tab 2&lt;/a&gt;
         *         &lt;a href=&quot;#&quot; data-toggle=&quot;tab&quot; class=&quot;disabled&quot;&gt;Disabled Tab&lt;/a&gt;
         *     &lt;/nav&gt;
         *     &lt;section class=&quot;active&quot;&gt;Lorizzle ipsizzle fo shizzle mah nizzle fo rizzle.&lt;/section&gt;
         *     &lt;section&gt;Nulla gangsta. Brizzle shizzlin dizzle pharetra.&lt;/section&gt;
         *     &lt;section&gt;This section will never be shown :(&lt;/section&gt;
         * &lt;/div&gt;
         *
         * @description Creates a new tab panel
         * @constructs
         * 
         * @param  {Object} options Component options
         * @param  {Mixed} options.element jQuery selector or DOM element to use for panel
         * @param  {String} [options.type=""] Type of the tabs. Can be blank, or one of white, stacked, or nav
         * @param  {Number} [options.active=0] index of active tab
         */
        construct: function(options) {
            // find elements for tab widget
            this.tablist = this.$element.find('> nav');
            this.tabs = this.tablist.find('> a[data-toggle~="tab"]');
            this.panels = this.$element.find('> section');

            this.applyOptions();

            // set up listeners for change events
            this.$element.on('change:type', this._setType.bind(this));
            this.$element.on('change:active', this._setActive.bind(this));
        },

        defaults: {},

        _types: [
            'white',
            'nav',
            'stacked'
        ],

        /**
         * sets all options
         */
        applyOptions: function () {
            var activeTab = this.tabs.filter('.active');

            // ensure the type is set correctly
            if (this.options.type) {
                this._setType(this.options.type);
            }

            // init tab switch
            this._initTabswitch();

            // accessibility
            this._makeAccessible();

            // set an active tab if there is non flagged as active
            if (activeTab.length === 0) {
                this._setActive(this.options.active || 0);
            } else {
                // call the activation logic 
                // in case the initial tab has remote content
                this._activateTab(activeTab, true);
            }
        },

        /**
         * sets a certain tab as active
         * @private
         * @param  {Number} idx 
         */
        _setActive: function (idx) {
            var activeTab = this.tabs.eq($.isNumeric(idx) ? idx : this.options.active);

            // Activate the tab, but don't focus
            this._activateTab(activeTab, true);
        },

        /**
         * disables a tab
         * @param  {jQuery} tab
         * @return {jQuery} this, chainable
         */
        setDisabled: function (tab, switcher) {
            var hop = switcher || false;

            tab.toggleClass('disabled', hop)
                .prop('aria-disabled', hop);
            return this;
        },

        /**
         * enables a tab
         * @param  {jQuery} tab
         * @return {jQuery} this, chainable
         */
        setEnabled: function (tab) {
            return this.setDisabled(tab, true);
        },

        /**
         * sets the type of the tabs
         * @private
         * @param  {String} type of the tabs: 'default', 'white', 'nav', 'stacked'
         */
        _setType: function (type) {
            var ty = $.type(type) === 'string' ? type : this.options.type;

            // Remove old type
            this.$element.removeClass(this._types.join(' '));

            // Add new type
            if (ty !== 'default') {
                this.$element.addClass(ty);
            }
        },

        /**
         * activates the given tab
         * @param  {jQuery} tab
         * @param  {Boolean} noFocus
         */
        _activateTab: function (tab, noFocus) {
            var href = tab.attr('href'),
                activeClass = 'active',
                panel;

            // do not allow to enable disabled tabs
            if (tab.hasClass('disabled')) {
                tab.blur(); // ensure disabled tabs do not receive focus
                return false;
            }

            // get panel based on aria control attribute
            panel = this.panels.filter('#' + tab.attr('aria-controls'));

            // supposed to be remote url
            if (href.charAt(0) !== '#') {
                panel.loadWithSpinner(href);
            }

            this.tabs.removeClass(activeClass).attr({
                'aria-selected': false,
                'tabindex': -1 // just the active one is able to tabbed
            });
            this.panels.removeClass(activeClass).attr({
                'aria-hidden': true
            });

            tab.addClass(activeClass).attr({
                'aria-selected': true,
                'tabindex': 0 // just the active one is able to tabbed
            });
            panel.addClass(activeClass).attr({
                'aria-hidden': false
            });

            if (!noFocus) {
                tab.trigger('focus');
            }
        }, // _activateTab

        /**
         * add the switching functionality
         * @private
         */
        _initTabswitch: function () {
            var self = this,
                sel = '> nav > a[data-toggle="tab"]';

            this.$element.fipo('tap', 'click', sel, function (event) {
                var tab = $(event.currentTarget);

                // prevent the default anchor
                event.preventDefault();

                self._activateTab(tab);
            }).finger('click', sel, false);
        }, // _initTabswitch

        /**
         * adds some accessibility attributes and features
         * http://www.w3.org/WAI/PF/aria-practices/#tabpanel
         * @private
         */
        _makeAccessible: function () {
            // init the key handling for tabs
            var self = this,
                idPrefix = 'tabs-panel-' + new Date().getTime() + '-',
                tabSelector = '> [role="tab"]';

            // the nav around the tabs has a tablist role
            this.tablist.attr('role', 'tablist');

            // set tab props
            this.tabs.each(function (i, e) {
                var tab = $(e);

                tab.attr({
                    'role': 'tab',
                    'tabindex': -1,
                    'aria-selected': false,
                    'aria-controls': idPrefix + i,
                    'aria-disabled': tab.hasClass('disabled')
                });
            });

            // set panel props
            this.panels.each(function (i, e) {
                var panel = $(e);

                panel.attr({
                    'id': idPrefix + i,
                    'role': 'tabpanel',
                    'aria-hidden': true
                });
            });

            // keyboard handling
            this.tablist.on('keydown', tabSelector, function (event) {
                // enables keyboard support

                var elem = $(event.currentTarget),
                    tabs = $(event.delegateTarget)
                        .find(tabSelector)
                        .not('[aria-disabled="true"]'), // ignore disabled tabs
                    focusElem = elem,
                    keymatch = true,
                    idx = tabs.index(elem);

                switch (event.which) {
                    case 33: //page up
                    case 37: //left arrow
                    case 38: //up arrow
                        focusElem = idx-1 > -1 ? tabs[idx-1] : tabs[tabs.length-1];
                        break;
                    case 34: //page down
                    case 39: //right arrow 
                    case 40: //down arrow
                        focusElem = idx+1 < tabs.length ? tabs[idx+1] : tabs[0];
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
                    focusElem = $(focusElem);
                    self._activateTab(focusElem);
                }
            });
        } // _makeAccessible

    });

    CUI.util.plugClass(CUI.Tabs);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on('cui-contentloaded.data-api', function (event) {
            $('[data-init~=tabs]', event.target).tabs();
        });
    }

}(jQuery, this));

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

(function($) {
  CUI.DropdownList = new Class(/** @lends CUI.DropdownList# */{
    toString: 'DropdownList',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc A dropdown list widget
      
      <p>
        Dropdown lists are meant to be used by other widgets like Filters and Dropdowns.
        Dropdown lists are invisible by default and can only be made visible by explicitly calling the methods
        "show" (resp. "hide") on the widget.
      </p>
      
      @desc Creates a dropdown list appended to any DOM element
      @constructs
      
      @param {Object}          options                               Component options
      @param {Array}           [options.options]                Array of options in the list
      @param {Function}        [options.optionRenderer=null]                Callback function to render one option
      @param {jQuery|Object}   [options.positioningElement=null]     If this optional element is given, the dropdown list
                                                                     will be placed beyond it instead of the standard element
      @param {String}   [options.cssClass=null]     An optional CSS class string that will be added to the list.

      @param {int}      [options.scrollBuffer]      Distance from bottom of list (px) before scrolled bottom event is fired. Use with infinite loading.
      @param {String}   [options.loadingIndicator]  HTML markup to show when loading in new content.
      @param {String}   [options.noMoreOptions]     Text to show when there are no more options to load.
      @param {String}   [options.position="below"]      Where to position the dropdown list. "above" or "below"
      @param {boolean}  [options.autoPosition=true]      Should the dropdown auto position itself if there's not enough space for the default position in the window?
    */
    construct: function(options) {

        var container = $("<div class=\"dropdown-container\">");
         
        var el = (this.options.positioningElement) ? this.options.positioningElement : this.$element;
        el.after(container);
        el.detach();
        container.append(el);
        this.containerElement = container;

        this.$element.on('change:options change:optionRenderer', function (event) {
            if (event.widget !== this) return; // Only listen to own events
            this.update();
        }.bind(this));
     

        // Listen to events 
        this.$element.on("keydown", "", this._keyPressed.bind(this));

        this.$element.on("blur", "", function() {
           // Did anyone want us to prevent hiding?
           if (this.preventHiding) {
             this.preventHiding = false;
             return;
           }
           this.hide();
        }.bind(this));
       
    },
    
    defaults: {
        positioningElement: null,
        position: "below",
        autoPosition: true,
        optionRenderer: null,
        options: ["Apples", "Pears", "Bananas", "Strawberries"],
        cssClass: null,
        visible: false,
        scrollBuffer: 10,
        loadingIndicator: "<div class='spinner'></div>",
        noWidth: false
    },

    listElement: null,
    containerElement: null,
    currentIndex: -1,
    preventHiding: false,
    
    /**
     * Show this list
     */
    show: function() {
        // Hide old list (if any!)
        this._unrender();
        this.currentIndex = -1;
        this.$element.focus();
        this._render();
    },
    
    /**
     * Hide this list with an optional delay in millis
     * @param int delay Delay before hiding element in milliseconds
     * @return void or timeout object if a delay was given
     */
    hide: function(delay) {
        if (delay > 0) {
            return setTimeout(this._unrender.bind(this), delay);
        } else {
            this._unrender();
        }
        return null;
    },
    
    /**
     * @return boolean true, if this list is currently visible
     */
    isVisible: function() {
        return this.options.visible;
    },
    
    /**
     * Toggle list visibility based on current status
     */
    toggle: function() {
        return this.isVisible() ? this.hide() : this.show();
    },

    /**
     * Updates the rendering of this widget.
     */
    update: function() {
        if (this.listElement) {
            this._unrender();
            this._render();
        }
    },

    /**
    * Append items to the end of the list.
    */
    addItems: function(items) {
        var offset = this.listElement.find('li').not('.loading-indicator').length;
        if(this.listElement) {
            var list = this.listElement.find('ul');
            $.each(items, function(index, value) {
                var el = (this.options.optionRenderer) ? this.options.optionRenderer(index, value) : $("<span>" + value.toString() + "</span>");
                var li = $("<li data-id=\"" + (offset+index) + "\">");
                if (index === this.currentIndex) li.addClass("selected");
                
                // Set class on list items from data-item-class set on options
                var optionClassAttribute = this.options.getOptionClassAttribute(index);
                if (optionClassAttribute) {
                    li.addClass(optionClassAttribute);
                }

                li.append(el);
                list.append(li);
                this.options.options.push(value);
            }.bind(this));
        }
    },
    
    getNumberOfItems: function() {
        var offset = this.listElement.find('li').not('.loading-indicator').length;
        return offset;
    },

    /**
    * Appends a loading indicator to the end of the list. Useful for loading in extra content
    */
    addLoadingIndicator: function() {
        if(this.listElement) {
            this.listElement.find("ul").append($("<li>" + this.options.loadingIndicator + "</li>").addClass("loading-indicator"));
        }
    },

    /**
    * Removes loading indicator from the list.
    */
    removeLoadingIndicator: function() {
      if (this.listElement) {
        this.listElement.find(".loading-indicator").remove();
      }
    },
    
    /** @ignore */    
    _keyPressed: function(event) {        
        var key = event.keyCode;
        
        // Only listen to keys if there is an autocomplete box right now
        if (!this.listElement) {
            return;
        }

        var currentIndex = this.currentIndex;
        
        if (key === 38) { // up
            event.preventDefault();
            if (currentIndex > 0) currentIndex--;
        }
        
        if (key === 40) { // down
            event.preventDefault();
            if (currentIndex < (this.listElement.find("li").length - 1)) currentIndex++;
        }
        
        if (key === 27) { // escape
            event.preventDefault();
            this.hide();
            return;
        }
        
        if (key === 13 || key === 20) { // return or space
           event.preventDefault();
           if (currentIndex >= 0) {
                this._triggerSelect(currentIndex, event);
                return;
           }
        }
        
        this.currentIndex = currentIndex;

        // Set new css classes
        var listItems = this.listElement.find("li");
        listItems.removeClass("selected");
        if (currentIndex >= 0) {
            var el = $(listItems[currentIndex]);
            el.addClass("selected");

            // Scroll to position if necessary
            var t = el.position().top;
            this.listElement.animate({scrollTop: t}, 50);
        }
    },
    /** @ignore */    
    _unrender: function() {       
        if (this.listElement) {
            this.listElement.remove();
            this.listElement = null;  
        }

        this.containerElement.removeClass("dropdown-visible");

        this.options.visible = false;
    },

    /** @ignore */    
    _render: function() {
        var options = this.options.options,
            pos;

        if (options.length === 0) return;
               
        var list = $("<ul></ul>");
        if (this.options.cssClass) list.addClass(this.options.cssClass);

        $.each(options, function(index, value) {
            var el = (this.options.optionRenderer) ? this.options.optionRenderer(index, value) : $("<span>" + value.toString() + "</span>");
            var li = $("<li data-id=\"" + index + "\">");
            if (index === this.currentIndex) li.addClass("selected");

            // Set class on list items from data-item-class set on options
            var optionClassAttribute = this.options.getOptionClassAttribute? this.options.getOptionClassAttribute(index):undefined;
            if (optionClassAttribute) {
                li.addClass(optionClassAttribute);
            }

            li.append(el);
            list.append(li);
        }.bind(this));
        
        list.on("click", "li:not(.loading-indicator)", function(event) {
            event.preventDefault();
            this._triggerSelect($(event.target).closest("li").attr("data-id"), event);
        }.bind(this));

        var el = (this.options.positioningElement) ? this.options.positioningElement : this.$element;

        var container = $("<div class=\"dropdown-list\">");
        container.append(list);

        this.listElement = list = container;

        // Append the list to body and hide it, allows us to calculate its natural height
        list.css({top: 0, left: 0, visibility: "hidden", display: "block"}).appendTo(document.body);

        if (!this.options.noWidth) {
            list.css('width', el.outerWidth(false) + "px");
        }

        pos = this._calcPosition();

        list.css({top: pos.top + "px", left: pos.left + "px", visibility: "visible"});

        this.containerElement.addClass("dropdown-visible");

        list.on("scroll", "", function(event) {
            // Trigger scroll event
            this._listScrolled();
        }.bind(this));

        list.on("mousedown", "", function(event) {
            event.preventDefault();
            event.stopPropagation();
        }.bind(this));

        this.options.visible = true;
    },

      /** @ignore **/
      _calcPosition: function() {
          var el = (this.options.positioningElement) ? this.options.positioningElement : this.$element,
              topb = el.offset().top + el.outerHeight(),
              topa = el.offset().top - this.listElement.outerHeight(),
              pos = {
                  left: el.offset().left,
                  top: topb
              },
              spaceWinBelow = topb + this.listElement.outerHeight() - $(window).scrollTop() < $(window).height(),
              spaceWinAbove = topa > $(window).scrollTop(),
              spaceDocBelow = topb + this.listElement.outerHeight() < $(document).height(),
              spaceDocAbove = topa > 0;

          pos['top'] = (this.options.position === "above") ? topa : topb;

          if(this.options.autoPosition) {
              if (this.options.position === "above" && !spaceWinAbove) {
                  pos['top'] = (spaceWinBelow || (!spaceDocAbove && spaceDocBelow)) ? topb : topa;
              } else if (this.options.position === "below" && !spaceWinBelow) {
                  pos['top'] = (spaceWinAbove || (!spaceDocBelow && spaceDocAbove)) ? topa : topb;
              }
          }

          return pos;
      },

    /** @ignore **/
    _listScrolled: function() {
      if(this._reachedListBottom()) {
          this._triggerScrolledBottom();
      }
    },

    /** @ignore **/
    _reachedListBottom: function() {
      var listWrapper = this.listElement;
      var list = this.listElement.find('ul');
      return (list.height() - listWrapper.height() <= listWrapper.scrollTop() + this.options.scrollBuffer);
    },
    
    /** @ignore */    
    _triggerSelect: function(index, event) {
        // Trigger a change event
        this.$element.focus();
        var e = $.Event('dropdown-list:select', {
          selectedIndex: index,
          selectedValue: this.options.options[index],
          source: this,
          originalEvent: event //passing originalEvent, for finding the exact target if required.
        });
        this.$element.trigger(e);    
    },

    /** @ignore */
    _triggerScrolledBottom: function(index) {
        // Trigger a scrolled bottom event
        var e = $.Event('dropdown-list:scrolled-bottom');
        this.$element.trigger(e);
    }
    
  });

  CUI.util.plugClass(CUI.DropdownList);
}(window.jQuery));

(function($) {
  CUI.Dropdown = new Class(/** @lends CUI.Dropdown# */{
    toString: 'Dropdown',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc A dropdown widget
      
        <p>
            <select name="foo" data-init="dropdown" multiple data-placeholder="Please Select items">
                <option>One</option>
                <option>Two</option>
                <option>Three</option>
            </select>
        </p>
        <p>
        Currently this widget does only support creation from an existing &lt;select&gt;&lt;/select&gt; field.
        </p>
    @example
    <caption>Instantiate by data API</caption>
    &lt;select name="foo" data-init="dropdown" multiple data-placeholder="Please Select items"&gt;
        &lt;option&gt;One&lt;/option&gt;
        &lt;option&gt;Two&lt;/option&gt;
        &lt;option&gt;Three&lt;/option&gt;
    &lt;/select&gt;

    @example
    <caption>Instantiate with Class</caption>
    var dropdown = new CUI.Dropdown({
      element: '#myOrdinarySelectBox'
    });

    // Changes the select box into a beautiful widget.

    @example
    <caption>Instantiate by jQuery plugin</caption>
    $("select").dropdown();

    // Changes all select boxes into beautiful widgets.
       
       
      @desc Creates a dropdown from any select element
      @constructs
      
      @param {Object}   options                               Component options
      @param {Array} [options.options=empty array]      Selectable options
      @param {boolean} [options.multiple=false]      Is this a multiselect widget?
      @param {boolean} [options.editable=false]      May the user edit the option text?
      @param {String} [options.placeholder="Select"]      Placeholder string to display in empty widget
      @param {boolean} [options.disabled=false]      Is this widget disabled?
      @param {boolean} [options.hasError=false]      Does this widget contain an error?
      @param {boolean} [options.noWidth=false]      Don't set dropdown list width?
      @param {Function} [options.autocompleteCallback=use options]      Callback for autocompletion: callback(handler, searchFor) with handler is a result callback function with handler(results, searchFor). See example page.
      @param {String}   [options.position="below"]      Where to position the dropdown list. "above" or "below"
      @param {boolean}  [options.autoPosition=true]      Should the dropdown auto position itself if there's not enough space for the default position in the window?
    */
    construct: function(options) {

        if (this.options.autocompleteCallback) {
            // Enable editing for autocompleter
            this.options.editable = true;
        }

        this._render();

        // isMobile should be replace with a CUI.Util method
        // Editable dropdown can't be natively rendered.
        if (this._isMobile() && !this.options.editable) {
            this._initForMobile();
        } else {
            this._initForDesktop();
        }
        
        var $button = this.$element.find('>div>button');
        if ($button.length > 0 && $button.attr('type') === undefined) {
            $button[0].setAttribute('type', 'button');
        }

    },
    
    defaults: {
        options: [],
        multiple: false,
        placeholder: "Select",
        disabled: false,
        editable: false,
        hasError: false,
        noWidth: false,
        position: "below",
        autoPosition: true
    },
    
    dropdownList: null,
    autocompleteList: null,
    syncSelectElement: null,
    buttonElement: null,
    positioningElement: null,
    inputElement: null,
    hasFocus: false,

    _initForMobile: function() {
        this.$element.addClass('mobile');

        this.buttonElement.on("click", function() {
            this._openSelectInput();
        }.bind(this));

        this.$element.find('select').on("change", function() {
            this._update(true);
        }.bind(this));

        // place the hidden select input at the right position for ipad and iphone
        this._placeSelect();
    },

    _initForDesktop: function() {
        this.dropdownList = new CUI.DropdownList({
            element: this.buttonElement,
            positioningElement: this.positioningElement,
            position: this.options.position,
            autoPosition: this.options.autoPosition,
            options: this.options.options,
            optionRenderer: this._optionRenderer.bind(this),
            getOptionClassAttribute: this._getOptionClassAttribute.bind(this),
            noWidth: this.options.noWidth
        });

        if (this.options.editable) {
            this.autocompleteList = new CUI.DropdownList({
                element: this.inputElement,
                positioningElement: this.positioningElement,
                position: this.options.position,
                autoPosition: this.options.autoPosition,
                options: this.options.options,
                optionRenderer: this._optionRendererAutocomplete.bind(this),
                getOptionClassAttribute: this._getOptionClassAttribute.bind(this),
                noWidth: this.options.noWidth,
                cssClass: "autocomplete-results"
            });
        }
        
        this.buttonElement.on("dropdown-list:select", this._processSelect.bind(this));
        
        this.buttonElement.on("mousedown", "", function(event) {
            event.preventDefault();
            if (this.autocompleteList !== null) {
                this._adjustAutocompleter();
            } else {
                this.dropdownList.toggle();
            }
        }.bind(this));
        this.buttonElement.on("mouseup", "", function(event) {
            event.preventDefault();
        }.bind(this));

        // Auto completion
        this.inputElement.on("click", "", function() {
           if (this.autocompleteList !== null) this._adjustAutocompleter();
        }.bind(this));
        this.inputElement.on("input", "", function() {
           if (this.autocompleteList !== null) this._adjustAutocompleter({toggleList : false});
        }.bind(this));
        this.inputElement.on("dropdown-list:select", "", function(event) {
            //this.inputElement.val(event.selectedValue);
            //this.autocompleteList.hide();
            this._processSelect(event);
        }.bind(this));
        
        // Correct focus
        this.$element.children().on("focus", "", function() {
            this.hasFocus = true;
            this._update();
        }.bind(this));
        this.$element.children().on("blur", "", function() {
            this.hasFocus = false;
            this._update();
        }.bind(this));

        this.$element.find('select').on("change", function() {
            this._update(true);
        }.bind(this));
    },

    _placeSelect: function() {
        var $select = this.$element.find('select').first();

        $select.css({
            position: 'absolute',
            left: 0,
            top: 0,
            right: 0,
            bottom: 0,
            width: 'auto',
            height: 'auto'
        });
    },

    _openSelectInput: function() {
        var selectElement = this.$element.find('select')[0];

        if (document.createEvent) {
            var e = document.createEvent("MouseEvents");
            e.initMouseEvent("mousedown", true, true, window, 0, 0, 0, 0, 0, false, false, false, false, 0, null);
            selectElement.dispatchEvent(e);
        } else if (selectElement.fireEvent) {
            selectElement.fireEvent("onmousedown");
        }
    },

    /** @ignore */
    _adjustAutocompleter: function(args) {
        var searchFor = this.inputElement.val();
        var defaults = { toggleList : true };
        var options = $.extend(defaults, args);

        var showResults = function(result, searchFor) {
            this.autocompleteList.set({
               options: result
            });
            
            if (options.toggleList) {
                this.autocompleteList.toggle();
            }
            else {
                this.autocompleteList.show();
            }
        }.bind(this);
        
        if (this.options.autocompleteCallback) {
            this.options.autocompleteCallback(showResults, searchFor);
        } else {
            var result = [];
            $.each(this.options.options, function(index, value) {
                 if (value.toLowerCase().indexOf(searchFor.toLowerCase(), 0) >= 0 ) result.push(value);
            });
            showResults(result, searchFor);
        }
       
    },

    /** @ignore */
    _optionRenderer: function(index, option) {
        var el = $("<span>" + option + "</span>");
        if (this.options.multiple) {
            var checkbox = $("<div class=\"checkbox\">");
            if (this.syncSelectElement && $(this.syncSelectElement.find("option").get(index)).attr("selected")) {
                checkbox.addClass("checked");
            }
            el.prepend(checkbox);
        }
        return el;
    },

    /** @ignore */
    _optionRendererAutocomplete: function(index, value) {
        var searchFor = this.inputElement.val();
        var i = value.toLowerCase().indexOf(searchFor.toLowerCase());
        if (i >= 0) {
            value = value.substr(0, i) + "<em>" + value.substr(i, searchFor.length) + "</em>" + value.substr(i + searchFor.length);
        }
        
        return $("<span>" + value + "</span>");
    },

    /** @ignore */
    _getOptionClassAttribute: function(index) {
        return (this.syncSelectElement) ? $(this.syncSelectElement.find("option").get(index)).data("itemclass") : "";
    },
    
    /** @ignore */
    _processSelect: function(event) {
        if (this.syncSelectElement) {

            var value = event.selectedValue;
            var current = null;

            // Synchronize select element
            if (event.source === this.autocompleteList) {
                this.syncSelectElement.find("option").each(function() {
                    if ($(this).attr("value") === value) current = $(this);
                });
                if (current === null) {
                    current = $("<option>");
                    current.attr("value", value).text(value);
                    this.syncSelectElement.append(current);
                }
                this.autocompleteList.hide();
            } else {
                current = $(this.syncSelectElement.find("option").get(event.selectedIndex));
                value = current.attr("value");
            }
            
            if (this.options.multiple) {
                var v = this.syncSelectElement.val();
                if (v === null) v = [];
                if (v.indexOf(value) >= 0) {
                    v.splice(v.indexOf(value), 1);
                } else {
                    v.push(value);
                }
                this.syncSelectElement.val(v);
                this.dropdownList.update();
            } else {
                this.syncSelectElement.val(value);
                this.dropdownList.hide();
            }
            this.syncSelectElement.change();
        }

        this._update(true);
    },
    
    /** @ignore */
    _render: function() {
        this._readDataFromMarkup();
        
        if (this.$element.get(0).tagName !== "DIV") {
            var div = $("<div></div>");
            this.$element.after(div);
            this.$element.detach();
            div.append(this.$element);
            this.$element = div;
        }

        this._createMissingElements();
        this.buttonElement = this.$element.find("button");
        this.syncSelectElement = this.$element.find("select");
        this.inputElement = this.$element.find("input");
        this.positioningElement = (this.options.editable) ? this.$element : this.buttonElement;
        
        // Build the button text placeholder. Used for managing text overflow
        if(!this.options.editable) {
            this.buttonText = $(this.buttonElement).wrapInner("<span />").find("span");
        }

        if (!this.inputElement.attr("name")) this.inputElement.attr("name", this.syncSelectElement.attr("name") + ".edit");
        if (this.syncSelectElement.attr("multiple")) this.options.multiple = true;
        
        this.$element.addClass("dropdown");
        if (this.options.editable) this.$element.addClass("dropdown-editable");
        

        if (this.$element.find("select option").length > 0 && this.options.options.length === 0) {
            this.options.options = [];
            this.$element.find("select option").each(function(i, e) {
                this.options.options.push($(e).html());
            }.bind(this));
        }
        
        // Set several options
        if (this.options.multiple) {
            this.syncSelectElement.attr("multiple", "multiple");
        } else {
            this.syncSelectElement.removeAttr("multiple", "multiple");
        }
        if (this.options.placeholder) {
            if(!this.options.editable) {
                this.buttonText.text(this.options.placeholder);
            } else {
                this.buttonElement.text(this.options.placeholder);
            }
            this.inputElement.attr("placeholder", this.options.placeholder);
        }

        this._update(true);
    },
    
    /** @ignore */
    _readDataFromMarkup: function() {
        if (this.$element.attr("disabled")) this.options.disabled = true;
        if (this.$element.attr("data-disabled")) this.options.disabled = true;
        if (this.$element.attr("multiple")) this.options.multiple = true;
        if (this.$element.attr("data-multiple")) this.options.multiple = true;
        if (this.$element.attr("placeholder")) this.options.placeholder = this.$element.attr("placeholder");
        if (this.$element.attr("data-placeholder")) this.options.placeholder = this.$element.attr("data-placeholder");
        if (this.$element.attr("data-editable")) this.options.editable = true;
        if (this.$element.attr("data-error")) this.options.hasError = true;
        if (this.$element.hasClass("error")) this.options.hasError = true;
        if (this.$element.attr("data-nowidth")) this.options.noWidth = true;
    },
    
    /** @ignore */
    _createMissingElements: function() {
        if (this.$element.find("button").length === 0) {
            var button = $("<button/>", {
                text: this.options.placeholder,
                type: 'button'
            });
            button.addClass("dropdown");
            this.$element.append(button);
        }
        if (this.options.editable && this.$element.find("input").length === 0) {
            var input = $("<input type=\"text\">");
            this.$element.prepend(input);
        }
        if (this.$element.find("select").length === 0) {
            var select = $("<select>");
            this.$element.append(select);
        }
    },
    
    /** @ignore */
    _update: function(updateContent) {
        if (updateContent) {
            if (this.syncSelectElement && !this.options.multiple) {
                var option = this.syncSelectElement.find("option:selected");
                if (option) {
                    var selectedIndex = option.index();
                    var text = option.text();
                    var html = option.html();
                    if (this.inputElement.length > 0) {
                        this.inputElement.val(text).trigger('change');
                    } else {
                        this.buttonText.html(html);
                    }
                }
            }
        }
        if (this.options.disabled) {
            this.buttonElement.attr("disabled", "disabled");
            this.inputElement.attr("disabled", "disabled");
        } else {
            this.buttonElement.removeAttr("disabled");
            this.inputElement.removeAttr("disabled");
        }
        if (this.hasFocus) {
            this.$element.addClass("focus");
        } else {
            this.$element.removeClass("focus");
        }
        if (this.options.hasError) {
            this.$element.addClass("error");
        } else {
            this.$element.removeClass("error");
        }
    },
    
    /** @ignore */
    _isMobile: function() {
        return typeof window.ontouchstart === 'object';
    }
    
  });

  CUI.util.plugClass(CUI.Dropdown);
  
  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
        $("[data-init~=dropdown]", e.target).dropdown();
    });
  }
}(window.jQuery));

(function($) {
  CUI.Filters = new Class(/** @lends CUI.Filters# */{
    toString: 'Filters', // Plural here as jQuery already has a method "filter"
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc An autocompletable filters&tags widget
      
      <p>
      <select data-init="filters" data-placeholder="Add filter" multiple data-stacking="true">
            <option>red</option>
            <option>green</option>
            <option>blue</option>
            <option>yellow</option>
            <option>orange</option>
      </select>
      </p>

        <h2>Hints &amp; Tricks</h2>
      <p>
       <ul>
            <li>
               If you change the <code>options</code> with the <code>set</code>-Method after initialising the element,
                don't forgt to also set the <code>optionDisplayStrings</code>.
            </li>
        </ul>
      
    </p>
      
    @example
    <caption>Instantiate with Data API</caption>
&lt;select data-init="filters" data-placeholder="Add filter" multiple data-stacking="true"&gt;
      &lt;option&gt;red&lt;/option&gt;
      &lt;option&gt;green&lt;/option&gt;
      &lt;option&gt;blue&lt;/option&gt;
      &lt;option&gt;yellow&lt;/option&gt;
      &lt;option&gt;orange&lt;/option&gt;
&lt;/select&gt;

Currently there are the following data options:
  data-init="filters"         Inits the filter widget after page load
  data-placeholder            Same as option "placeholder"
  data-multiple               Sets field to "multiple" if given (with any non-empty value)
  data-stacking               Sets field to "stacking" if given (with any non-empty value)
  data-disabled               Sets field to "disabled" if given (with any non-empty value)
  data-allow="create"         If given the user is allowed to add new entries to the option list
  data-option-renderer        Either "default" for default behavior or "cqTag" for correct display of CQ5 Tags.

  "data-multiple", "data-disabled" and "data-placeholder" can be replaced by the native HTML attributes "multiple", "disabled" and
  "placeholder" if the current element allows them.
      
      @example
<caption>Instantiate with Class</caption>
var filters = new CUI.Filters({
  element: '#myFilter',
  options: ["Apples", "Pears", "Oranges"]
});

// Set the currently selected option
filters.setSelectedIndex(1);
        
      @example
<caption>Instantiate with jQuery</caption>
$('#myFilters').alert({
  options: ["Apples", "Pears", "Oranges"]
});

// A reference to the element's filters instance is stored as data-filters
var filters = $('#myFilters').data('filters');
var index = filters.getSelectedIndex();
            

      @desc Creates a filters field
      
            <h4>Callbacks</h4>
            <p>
      There are two callbacks you can use to customize your filters widget and that can be configured within the options array.
      </p><p>
      With the <code>autocompleteCallback(Function handler, String searchFor, int offset, int length)</code> you can customize the results that are displayed in the autocomplete dropdown list. The handler function to be called after your autocomplete function has finished accepts an array of result objects. These objects can hold any data as long as they define a convenient <code>toString()</code> method that returns the right option value.
            </p>
            <p>The additional parameters <code>offset</code> and <code>length</code> are only important when infinite list loading is activated: They define the part of the list to be loaded</p>
            <p>
            The <code>optionRenderer(int index, Object option, boolean highlight, boolean showIcon)</code> callback can be used to customize the HTML markup of the items in the dropdown. <code>index</code> is the position in the current list, <code>option</code> is the current option to display (your custom Object if you defined an autocompleteCallback, a string otherwise), <code>highlight</code> defines wether you should highlight the search term and <code>showIcon</code> defines wether you should add an icon to your markup. You have to return a valid jQuery element.
            </p>
      
      @constructs
      
      @param {Object}   options                                    Component options
      @param {Array}    [options.options=empty array]              Array of available options (will be read from &lt;select&gt; by default)
      @param {Array}    [options.optionDisplayStrings=empty array] Array of alternate strings for display (will be read from &lt;select&gt; by default)
      @param {boolean}  [options.multiple=false]                   Can the user select more than one option?
      @param {boolean}  [options.stacking=false]                   Uses a slightly different style, implies multiple
      @param {boolean}  [options.allowCreate=false]                Allow the creation of new tags.
      @param {boolean}  [options.placeholder=null]                 Define a placeholder for the input field
      @param {int}      [options.delay=200]                        Delay before starting autocomplete when typing
      @param {int}      [options.disabled=false]                   Is this component disabled?
      @param {boolean}  [options.highlight=true]                   Highlight search string in results
      @param {String}   [options.name=null]                        (Optional) name for an underlying form field.
      @param {Object}   [options.icons=empty object]               Icons to display. Option key paired with either Coral icon css class or image url.
      @param {String}   [options.iconSize=small]                   Icon size from: xsmall (12x12), small (16x16), medium (24x24), large (32x32).
      @param {Function} [options.autocompleteCallback=use options] Callback for autocompletion
      @param {Function} [options.optionRenderer=default renderer]  (Optional) Renderer for the autocompleter and the tag badges
      @param {boolean}  [options.infiniteLoad=false]               Should extra content be loaded dynamically when the list is scrolled to bottom?
      @param {boolean}  [options.maxLoadingItems=20]               Maximum number of items to load per request on infinite list loading.
      @param {boolean}  [options.hasError=false]                   Set to true to display widget as erroneous, regardless if widgets detects an error or not
      @param {String}   [options.position="below"]                 Where to position the dropdown list. "above" or "below"
      @param {boolean}  [options.autoPosition=true]                Should the dropdown auto position itself if there's not enough space for the default position in the window?
          */
    construct: function(options) {
        this.selectedIndices = []; // Initialise fresh array
        this.createdIndices = []; // Initialise fresh array
        
        // Set callbacks to default if there are none
        if (!this.options.autocompleteCallback) {
            this.options.autocompleteCallback = this._defaultAutocompleteCallback.bind(this);
        } else {
            this.usingExternalData = true;
        }
        if (!this.options.optionRenderer) this.options.optionRenderer = CUI.Filters.defaultOptionRenderer;
        
        // Read options from markup
        this._readDataFromMarkup();
        
        // Stacking forces multiple
        if (this.options.stacking) this.options.multiple = true;
        
        // Adjust DOM to our needs
        this._render();
        
        // Populate alternative display strings if necessary
        if (this.options.optionDisplayStrings.length > this.options.options.length) {
            this.options.optionDisplayStrings = this.options.optionDisplayStrings.slice(0, this.options.options.length);
        }
        while (this.options.optionDisplayStrings.length < this.options.options.length) {
            this.options.optionDisplayStrings.push(this.options.options[this.options.optionDisplayStrings.length]);
        }
        
        // Generate Dropdown List widget
        this.dropdownList = new CUI.DropdownList({
            element: this.inputElement,
            positioningElement: (this.options.stacking) ? this.$element : this.inputElement,
            position: this.options.position,
            autoPosition: this.options.autoPosition,
            cssClass: "autocomplete-results"
        });
        
        this._addEventListeners();
       
    },
    
    defaults: {
        autocompleteCallback: null,
        optionRenderer: null,
        options: [],
        optionDisplayStrings: [],
        multiple: false,
        delay: 200,
        highlight: true,
        stacking: false,
        placeholder: null,
        allowCreate: false,
        icons: null,
        iconSize: "small",
        infiniteLoad: false,
        maxLoadingItems: 20,
        hasError: false,
        position: "below",
        autoPosition: true
    },

    dropdownList: null, // Reference to instance of CUI.DropdownList
    syncSelectElement: null,
    inputElement: null,
    typeTimeout: null,
    
    selectedIndex: -1,         // For single term only
    selectedIndices: null,     // For multiple terms
    createdIndices: null,      // Newly created indices
    triggeredBackspace: false,
    usingExternalData: false,  // Using autocomplete callback for loading external data?
    selectedValue: null,       // Used to store returned value when data is loaded externally

    // Infinite loading
    isLoadingExternal: false,  // Has an external call for data been started?
    loadedEverything: false,   // Have we loaded all the items that we can?

    // TODO switch selectedIndex/selectedIndices to store keys rather than indexes so that they
    // can be used with external data. Remove selectedValue variable on completion.

    /**
     * @param {int} index     Sets the currently selected option by its index or, if options.multiple ist set,
     *                          adds it to the list of selected indices. -1 is valid for single term use and removes any
     *                          selected index.
     */
    setSelectedIndex: function(index) {
        if (index < -1 || index >= this.options.options.length) return;
        this.inputElement.attr("value", "");
        this.selectedIndex = index;
        if (this.options.multiple && index >=0 && this.selectedIndices.indexOf(index * 1) < 0) {
            this.selectedIndices.push(index * 1); // force numeric
        }
        if (!this.options.multiple) {
            this.selectedIndices = (index >= 0) ? [index] : [];
        }
        this._update();
    },
    
    
    /**
     *  Removes the given index from the list of selected indices. Only applies to multi term use.
     **/
    removeSelectedIndex: function(index) {
        var i = this.selectedIndices.indexOf(index * 1);
        if (i < 0) return;
        this.selectedIndices.splice(i, 1);
        this._update();
    },
    
    /**
     * @return {int} The currently selected options by index or -1 if none is selected
     */
    getSelectedIndex: function() {
        return this.selectedIndex;
    },
    

    /**
     * @param {array} Replace the list of currently selected indices. Only applies to multi term use.
     */
    setSelectedIndices: function(indices) {
        this.selectedIndices = indices.slice(0); // Make copy of parameter!
        this._update();
    },
    
    /**
     * @return {array} The currently selected options by index or an empty array if none is selected
     */
    getSelectedIndices: function() {
        return this.selectedIndices.slice(0); // Make copy before returning!
    },
    
    /** @ignore */
    _addEventListeners: function() {
        // Listen to property changes
        this.$element.on('change:disabled', this._update.bind(this));
        this.$element.on('change:placeholder', this._update.bind(this));
        
        this.$element.on('change:options', this._changeOptions.bind(this));
        
        // Listen to events
        this.$element.on("input", "input", function() {
            if (this.options.disabled) return;
            if (this.typeTimeout) clearTimeout(this.typeTimeout);
            this.typeTimeout = setTimeout(this._inputChanged.bind(this), this.options.delay);
        }.bind(this));
        
        this.$element.on("click touchend", "input", function() {
            if (this.options.disabled) return;
            this.inputElement.focus();
            this._inputChanged();
        }.bind(this));
                
        this.$element.on("blur", "input", function() {
            if (this.options.disabled) return;
            if (this.typeTimeout) clearTimeout(this.typeTimeout);
            this.typeTimeout = null;
            // Set to existing selection for single term use
            if (!this.options.multiple && this.selectedIndex >=0) {
                if (this.inputElement.attr("value") === "") {
                    this.setSelectedIndex(-1);
                } else {
                    this._update();
                }
            }
            var hasError = this.options.hasError || (!this.options.multiple && this.inputElement.val().length > 0 && this.selectedIndex < 0);
            this.$element.toggleClass("error", hasError);
        }.bind(this));
       
        this.$element.on("keydown", "input", this._keyPressed.bind(this));
        this.$element.on("keyup", "input", this._keyUp.bind(this));
        
        this.dropdownList.on("dropdown-list:select", "", function(event) {

            if(this.options.infiniteLoad) {
                this.loadedEverything = false;
            }

            this.dropdownList.hide(200);

            if(this.usingExternalData) {
                this._createNewOption(event.selectedValue.toString(), event.selectedValue.toString(), true);
                this._update();
            }
            
            var pos = $.inArray(event.selectedValue.toString(), this.options.options);
            this.setSelectedIndex(pos * 1);
        }.bind(this));

        if(this.options.infiniteLoad) {
            this.dropdownList.on("dropdown-list:scrolled-bottom", "", function(event) {
                if(!this.isLoadingExternal && !this.loadedEverything) {
                    this.isLoadingExternal = true;
                    
                    this.dropdownList.addLoadingIndicator();
                    var offset = this.dropdownList.getNumberOfItems();
                    var searchFor = this.inputElement.attr("value");
                    this.options.autocompleteCallback($.proxy(this._appendLoadedData, this), searchFor, offset, this.options.maxLoadingItems);
                }
            }.bind(this));
        }

        this.$element.on("click", "[data-dismiss=filter]", function(event) {
            if (this.options.disabled) return;
            var e = $(event.target).closest("[data-id]");
            this.removeSelectedIndex(e.attr("data-id"));
            return false;
        }.bind(this));
        
        // .. and some more event handlers to style our widget
        this.$element.on("input", "input", function() {
            if (this.options.disabled) return;
            this._correctInputFieldWidth();
        }.bind(this));
        
        this.$element.on("focus", "input", function(event) {
            if (this.options.disabled) return;
            this.$element.addClass("focus");
            event.preventDefault();
        }.bind(this));
        
        this.$element.on("blur", "input", function() {
            if (this.options.disabled) return;
            this.$element.removeClass("focus");
        }.bind(this));
    },
        
    /** @ignore */
    _changeOptions: function(event) {
        if (event.widget !== this) return;
        this.selectedIndex = -1;
        this.selectedIndices = [];
        this.createdIndices = [];
        this._update();
    },

    /** @ignore */
    _render: function() {
        var div;
        // if current element is select field -> turn into input field, but hold reference to select to update it on change
        if (this.$element.get(0).tagName === "SELECT") {        
            div = $("<div></div>");
            this.$element.after(div);
            this.$element.detach();
            div.append(this.$element);
            this.$element = div;
        }
        
        // if current element is input field -> wrap it into DIV
        if (this.$element.get(0).tagName === "INPUT") {
            div = $("<div></div>");
            this.$element.after(div);
            this.$element.detach();
            div.prepend(this.$element);
            this.$element = div;
        }

        // If there was an select in markup: use it for generating options
        if (this.$element.find("select option").length > 0 && this.options.options.length === 0) {
            this.options.options = [];
            this.options.optionDisplayStrings = [];
            this.$element.find("select option").each(function(i, e) {
            this.options.options.push($(e).val());
            this.options.optionDisplayStrings.push($.trim($(e).text()));
            
            // Save selected state
            if ($(e).attr("selected")) {
	            this.selectedIndices.push(i);		
	            this.selectedIndex = i;		
            }

            }.bind(this));
        }

        this._createMissingElements();
        
        this.syncSelectElement = this.$element.find("select");
        this.inputElement = this.$element.find("input");
        
        this.$element.addClass("filters");
        this.$element.removeClass("focus");

        if (!this.options.placeholder) this.options.placeholder = this.inputElement.attr("placeholder");
        if (this.options.name) this.syncSelectElement.attr("name", this.options.name);
        if (this.options.stacking) this.$element.addClass("stacking"); else this.$element.removeClass("stacking");

       
        this._update();
    },
    
    _createMissingElements: function() {
        if (this.$element.find("select").length === 0) {
            this.$element.append($("<select " + (this.options.multiple ? "multiple" : "") + "></select>"));
        }
        if (this.$element.find("input").length === 0) {
            this.$element.prepend($("<input type=\"text\">"));
        }
        
        // Create all options where neccessary
        if (this.$element.find("select option").length < this.options.options.length) {
            for(var i = this.$element.find("select option").length; i < this.options.options.length; i++) {
                var value = this.options.options[i];
                var name = this.options.optionDisplayStrings[i] || this.options.options[i];
                var opt = $("<option></option>");
                opt.text(name);
                opt.attr("value", value);
                this.$element.find("select").append(opt);
            }
        }
        
    },

    /** @ignore */
    _readDataFromMarkup: function() {
            if (this.$element.attr("multiple")) this.options.multiple = true;
            if (this.$element.attr("data-multiple")) this.options.multiple = true;
            if (this.$element.attr("data-stacking")) this.options.stacking = true;
            if (this.$element.attr("placeholder")) this.options.placeholder = this.$element.attr("placeholder");
            if (this.$element.attr("data-placeholder")) this.options.placeholder = this.$element.attr("data-placeholder");
            if (this.$element.attr("disabled")) this.options.disabled = true;
            if (this.$element.hasClass("error")) this.options.hasError = true;
            if (this.$element.find("input").hasClass("error")) this.options.hasError = true;                
            if (this.$element.attr("data-disabled")) this.options.disabled = true;
            if (this.$element.attr("data-allow") === "create") this.options.allowCreate = true;
            if (this.$element.attr("data-option-renderer")) {
                // Allow to choose from default option Renderers
                this.options.optionRenderer = CUI.Filters[this.$element.attr("data-option-renderer") + "OptionRenderer"];
                if (!this.options.optionRenderer) this.options.optionRenderer = CUI.Filters.defaultOptionRenderer;
            }
   },
   
    /** @ignore */
    _update: function() {
        
        if (this.options.placeholder) this.inputElement.attr("placeholder", this.options.placeholder);
        
        if (this.options.disabled) {
            this.$element.addClass("disabled");
            this.inputElement.attr("disabled", "disabled");
        } else {
           this.$element.removeClass("disabled");
           this.inputElement.removeAttr("disabled");
        }
        
        // Update single term fields
        if (!this.options.multiple) {
            if (this.syncSelectElement) this.syncSelectElement.find("option:selected").removeAttr("selected");

            if (this.selectedIndex >= 0) {
                if (this.syncSelectElement) $(this.syncSelectElement.find("option").get(this.selectedIndex)).attr("selected", "selected");
                var option = this.options.options[this.selectedIndex];
                if (this.options.optionDisplayStrings[this.selectedIndex]) { // Use alternative display strings
                    option = this.options.optionDisplayStrings[this.selectedIndex];
                }
                this.inputElement.attr("value", option);
            } else {
                this.inputElement.attr("value", "");
            }
            this.$element.find('select').change();
            return;
        }

        // Update multiple term fields
        if (this.syncSelectElement) {
            this.syncSelectElement.find("option:selected").removeAttr("selected");
            
            for(var i = 0; i < this.selectedIndices.length; i++) {
                var index = this.selectedIndices[i];
                $(this.syncSelectElement.find("option").get(index)).attr("selected", "selected");
                
            }
        }
        
        // Create selected tag list
        var ul = $("<ul class=\"tags\"></ul>");
        
        $.each(this.selectedIndices, function(iterator, index) {
            var option = this.options.options[index];
            var el = (this.options.optionRenderer.bind(this))(index, option, false, false);
            var li = $("<li data-id=\"" + index + "\"><button data-dismiss=\"filter\">&times;</button></li>");
            ul.append(li);
            li.append(el);
            
        }.bind(this));
        
        // Add new list to widget
        this.$element.find("ul").remove();
        if (ul.children().length > 0) {
            if (this.options.stacking) {
                this.$element.prepend(ul);        
            } else {
                this.$element.append(ul);
            }
        }
        
        // Correct input field length of stacking fields
        this._correctInputFieldWidth();
        this.$element.find('select').change();
    },

    /** @ignore */
    _keyUp: function(event) {
        var key = event.keyCode;
        if (key === 8) {  
            this.triggeredBackspace = false; // Release the key event
        }
    },
    
    /** @ignore */
    _keyPressed: function(event) {        
        var key = event.keyCode;
        if (key === 8) { 
            if (this.triggeredBackspace === false && this.inputElement.attr("value").length === 0) {
                if (this.options.multiple && this.selectedIndices.length > 0) {
                    event.preventDefault();
                    this.removeSelectedIndex(this.selectedIndices[this.selectedIndices.length - 1]);
                    this._inputChanged();
                }
            }
           
            this.triggeredBackspace = true; // Remember this key down event
        }
        
        if (!this.dropdownList.isVisible()) {
            if (key === 40) {
                this._inputChanged(); // Show box now!
                event.preventDefault();
            }
            if (key === 13) { // Create new item
                var val = this.inputElement.val();
                if (val.length > 0) {
                    this._createNewOption(val, val, false);
                    event.preventDefault();
                }
            }
        } else {
            if (key === 13 && this.dropdownList.currentIndex < 0) {
                // Close dropdownlist, even if nothing is selected
                this.dropdownList.hide();
                event.preventDefault();
            }
        }
    },
    
    _createNewOption: function(name, displayName, fromInternal) {
        
        var existingIndex = -1;
        
        // First check if there really is no option with this name
        $.each(this.options.options, function(index, optionName) {
           if (this.options.optionDisplayStrings[index] === name) existingIndex = index;
           if (optionName === name) existingIndex = index;           
        }.bind(this));
        
        if (existingIndex >=0) {
            this.setSelectedIndex(existingIndex);
            return;
        }
        
        // Is it allowed to add new options?
        if (!this.options.allowCreate && !fromInternal) return;
        
        var index = this.options.options.length;

        this.options.options.push(name);
        this.options.optionDisplayStrings.push(displayName);
        if (this.syncSelectElement) {
            var el = $("<option></option>");
            el.text(displayName);
            el.attr("value", name);
            if (!fromInternal) el.attr("data-new", "true");
            this.syncSelectElement.append(el);
            //console.log("Compare indices", el.index(), index);
        }
        if (!fromInternal) this.createdIndices.push(index);
        this.setSelectedIndex(index);
    },
    
    /** @ignore */
    _correctInputFieldWidth: function() {
        if (!this.options.stacking) return;
        
        var i = this.inputElement;
        var text = i.attr("value") || '';
        if (text.length === 0) text = i.attr("placeholder");
        var styles = ["font-family", "font-weight", "font-style", "font-size", "letter-spacing", "line-height", "text-transform"];
        var div = $("<div style=\"position: absolute; display: none;\"></div>");
        $.each(styles, function(x, style) {
            div.css(style, i.css(style));
        });
        div.text(text);
        $("body").append(div);
        var w = div.width() + 25;
        div.remove();
        var border = parseFloat(i.css("margin-left")) + parseFloat(i.css("margin-right"));        
        var isEmpty = (this.selectedIndices.length === 0);
        if (isEmpty || w > (this.$element.width() - border)) {
            w = (this.$element.width() - border);
        }
        i.width(w);
    },
    
    /** @ignore */
    _inputChanged: function() {

        if(this.options.infiniteLoad) {
            this.loadedEverything = false;
        }

        var searchFor = this.inputElement.attr("value");
        this.options.autocompleteCallback($.proxy(this._showAutocompleter, this), searchFor, 0, (this.options.infiniteLoad) ? this.options.maxLoadingItems : 500); // Do not load more than 500 items without infinite loading
    },

    /** @ignore */
    _appendLoadedData: function(results) {

        this.dropdownList.removeLoadingIndicator();

        // No results back, must be no more data to load
        if(results.length === 0) {
            this.loadedEverything = true;
            this.isLoadingExternal = false;
            return;
        }

        // Append the fetched items to the end of the currently open list
        this.dropdownList.addItems(results);

        // We're ready to load content again
        this.isLoadingExternal = false;
    },
    
    /** @ignore */
    _showAutocompleter: function(results) {
        this.dropdownList.hide();
        
        if (this.options.multiple) {
            // Do not show already selected indices
            var l = [];
            $.each(results, function(iterator, key) {
                // TODO: can't call this.options.options when we are using external data source
                var pos = $.inArray(key, this.options.options);
                if (this.selectedIndices.indexOf(pos) >= 0) return;
                l.push(key);
            }.bind(this));
            results = l;
        }        
        if (results.length === 0) return;

        var optionRenderer = function(index, key) {
            return (this.options.optionRenderer.bind(this))(index, key, this.options.highlight, !$.isEmptyObject(this.options.icons));
        };

        this.dropdownList.set("optionRenderer", optionRenderer.bind(this));
        this.dropdownList.set("options", results);
        
        this.dropdownList.show();
        
    },
    
    /** @ignore */
    _defaultAutocompleteCallback: function(handler, searchFor) {
        var result = [];

        $.each(this.options.options, function(index, key) {
            var name = key;
            
            if (this.options.optionDisplayStrings[index]) { // Use alternate display names for autocomplete (if possible)
                name = this.options.optionDisplayStrings[index];
            }
            if (name.toLowerCase().indexOf(searchFor.toLowerCase(), 0) >= 0 ) result.push(key);

        }.bind(this));

        handler(result, searchFor);
    },

    /** @ignore */
    _buildIcon: function(type, attr, size) {
        if(type === "url") {
            return '<i class="' + size + ' icon-inline-bg-image" style="background-image: url(' + attr + ')">' + 'icon' + '</i>';
        } else if(type === "cuiIcon") {
            return '<i class="' + attr + ' ' + size + '">' + attr.split('icon-')[1] + '</i>';
        }
    }
  });

  CUI.util.plugClass(CUI.Filters);
  
  // Data API
  $(document).on("cui-contentloaded.data-api", function(e) {
    $("[data-init~=filters]", e.target).filters();
  });

  CUI.Filters.defaultOptionRenderer = function(index, key, highlight, icon) {

    var pos = $.inArray(key, this.options.options);
    var value = key;

    if (this.options.optionDisplayStrings[pos]) { // Use alternate display strings if possible
        value = this.options.optionDisplayStrings[pos];
    }

    var searchFor = this.inputElement.val();

    if (highlight) {
        var i = value.toLowerCase().indexOf(searchFor.toLowerCase());
        if (i >= 0) {
            value = value.substr(0, i) + "<em>" + value.substr(i, searchFor.length) + "</em>" + value.substr(i + searchFor.length);
        }
    }
    
    // Check if this tag is new
    if (this.createdIndices.indexOf(pos) >= 0) {
        value = value + "&nbsp;*";
    }

    // If we allow icons here, build icon if provided for option depending on type (image url or cui icon class)
    if(icon) {
        var attr;
        if(typeof(attr = this.options.icons[key]) !== "undefined") {
            var type = (/^icon-/i.test(attr)) ? "cuiIcon" : "url";
            value = this._buildIcon(type,attr,this.options.iconSize) + value;
        }
    }

    return $("<span>" + value + "</span>");
};

CUI.Filters.cqTagOptionRenderer = function(iterator, key, highlight) {
    var index = $.inArray(key, this.options.options);
    var value = key;
    
    if (this.options.optionDisplayStrings[index]) { // Use alternate display strings if possible
        value = this.options.optionDisplayStrings[index];
    }
    var searchFor = this.inputElement.val();
    
    var namespaced = value.split(":");
    if (namespaced.length < 2) namespaced[1] = "";
    
    var namespace = namespaced[0];
    var restPath = namespaced[1];
    
    var pathParts = restPath.split("/");

    
    // html encode fn
    function e(text) {
        return $("<div>").text(text).html();
    }
    
    value = namespace + ": ";
    for(var q = 0; q < pathParts.length; q++) {
        var part = pathParts[q];

        if (highlight) {
            var i = part.toLowerCase().indexOf(searchFor.toLowerCase());
            if (i >= 0) {
                part = e(part.substr(0, i)) + "<em>" + e(part.substr(i, searchFor.length)) + "</em>" + e(part.substr(i + searchFor.length));
            } else {
                part = e(part);
            }
        } else {
            part = e(part);
        }
        
        if (q > 0) value += " / ";
        if (q === pathParts.length - 1) part = "<b>" + part + "</b>";
        value = value + part;
    }
        
    // Check if this tag is new
    if (this.createdIndices.indexOf(index) >= 0) {
        value = value + "&nbsp;*";
    }

    
    return $("<span>" + value + "</span>");
  };
}(window.jQuery));

(function($) {
  CUI.Slider = new Class(/** @lends CUI.Slider# */{
    toString: 'Slider',
    extend: CUI.Widget,

    /**
      @extends CUI.Widget
      @classdesc <p><span id="slider-label">A slider widget</span></p>

        <div class="slider ticked filled tooltips" data-init="slider">
            <input aria-labelledby="slider-label" type="range" value="14" min="10" max="20" step="2">
        </div>

        <p>
        You should provide some information in the HTML markup, as this widget will read most of its options
        directly from the HTML elements:
        </p>
        <ul>
            <li>Current, min, max and step values are read from the input element</li>
            <li>The toggles for tooltips, ticks, vertical orientation and filled bars are set if the CSS classes tooltips, ticked, vertical or filled are present</li>
            <li>Use the attribute data-slide='true' to make handles slide smoothly. Use with care: This can make the slider unresponsive on some systems.</li>
        </ul>
        <p>
        As an alternative you can also directly create an instance of this widget with the class constructor CUI.Slider() or with the jQUery plugin $.slider().
        </p>
    @example
    <caption>Simple horizontal slider</caption>
    &lt;div class="slider" data-init="slider"&gt;
        &lt;label&gt;
            Horizontal Slider
            &lt;input type="range" value="14" min="10" max="20" step="2"&gt;
        &lt;/label&gt;
    &lt;/div&gt;
    @example
    <caption>Full-featured slider with two handles, tooltips, ticks and a filled bar</caption>
    &lt;div class="slider tooltips ticked filled" data-init="slider"&gt;    
        &lt;fieldset&gt;
            &lt;legend&gt;Vertical Range Slider with Tooltips&lt;/legend&gt;
            &lt;label&gt;
                Minimum value
                &lt;input type="range" value="14" min="10" max="20" step="2"&gt;
            &lt;/label&gt;
            &lt;label&gt;
                Maximum value
                &lt;input type="range" value="16" min="10" max="20" step="2"&gt;
            &lt;/label&gt;
        &lt;/fieldset&gt;
    &lt;/div&gt;

    @example
    <caption>Instantiate by jQuery plugin</caption>
    $(".slider-markupless").slider({
            min: 0,
            max: 100,
            step: 5,
            value: 50,
            ticks: true,
            filled: true,
            orientation: "vertical",
            tooltips: true,
            slide: true
        });

      @desc Creates a slider from a div
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
    construct: function(options) {
        var that = this;

        // sane defaults for the options
        that.options = $.extend({}, this.defaults, options);

        // setting default dom attributes if needed
        if (this.$element.hasClass('vertical')) {
            that.options.orientation = 'vertical';
            that.isVertical = true;
        }

        if(that.$element.hasClass('tooltips')) {
            that.options.tooltips = true;
        }

        if(that.$element.hasClass('ticked')) {
            that.options.ticks = true;
        }

        if(this.$element.hasClass('filled')) {
            that.options.filled = true;
        }

        if (this.$element.data("slide")) {
            that.options.slide = true;
        }

        if(this.$element.hasClass('bound')) {
            that.options.bound = true;
        }
        
        var elementId = this.$element.attr('id');
        // if the element doesn't have an id, build a unique id using new Date().getTime()
        if(!elementId) {
            this.$element.attr("id","cui-slider-" + new Date().getTime());
            elementId = this.$element.attr('id');
        }
                
        this._renderMissingElements();

        // sliders with two inputs should be contained within a fieldset to provide a label for the grouping
        var $fieldset = that.$element.find("fieldset");
        var $legend;
        if ($fieldset.length) {
            // move all fieldset children other than the legend to be children of the element.
            that.$element.append($fieldset.contents(":not(legend)"));
            
            // create a new wrapper div with role="group" and class="sliderfieldset," which will behave as a fieldset but render as an inline block
            var $newFieldset = $('<div role="group" class="sliderfieldset" />');
                        
            // wrap the element with the new "sliderfieldset" div
            that.$element.wrap($newFieldset);

            // get the first legend. there should only be one
            $legend = $fieldset.find("legend").first();
            if ($legend.length) {
                // create new label element and append the contents of the legend
                var $newLegend = $('<label/>').append($legend.contents());

                // give the new label element all the same attributes as the legend
                $.each($legend.prop("attributes"), function() {
                    $newLegend.attr(this.name, this.value);
                });
                                
                // if the new label/legend has no id, assign one.
                if (!$newLegend.attr("id")) {
                    $newLegend.attr("id", elementId + "-legend");
                }
                                
                $newFieldset.attr("aria-labelledby", $newLegend.attr("id"));
                                
                // replace the original fieldset, which now only contains the original legend, with the new legend label element
                $fieldset.replaceWith($newLegend);
                                
                // insert the new label/legend before the element
                $legend = $newLegend.insertBefore(that.$element);        
            }
        }

        that.$inputs = this.$element.find('input');

        var values = [];

        that.$inputs.each(function(index) {
            var $this = $(this);
            var thisId = $this.attr("id");
            // if the input doesn't have an id, make one
            if (!thisId) {
                $this.attr("id",elementId+"-input"+index);
                thisId = $this.attr("id");
            }
            
            if (!$this.attr("aria-labelledby")) {
                $this.attr("aria-labelledby","");
            }
            
            // existing labels that use the "for" attribute to identify the input
            var $label = that.$element.find("label[for='"+thisId+"']");
            
            // if we have a legend, the input should first be labelled by the legend
            if ($legend) {
                if($this.attr("aria-labelledby").indexOf($legend.attr("id"))===-1) {
                    $this.attr("aria-labelledby", $legend.attr("id")+($this.attr("aria-labelledby").length ? " ":"")+$this.attr("aria-labelledby"));
                }
            }
            
            // for existing labels that use the "for" attribute to identify the input
            if ($label.length) {
                // the label is not the inputs parent, move it before the slider element tag
                $label.not($this.parent()).insertBefore(that.$element);
                $label.each(function(index) {
                    // if the label doesn't have an id, create one
                    if (!$(this).attr("id")) {
                        $(this).attr("id",thisId+"-label"+index);
                    }
                    
                    // explicity identify the input's label
                    if($this.attr("aria-labelledby").indexOf(thisId+"-label"+index)===-1) {
                        $this.attr("aria-labelledby", ($this.attr("aria-labelledby").length ? " ":"")+thisId+"-label"+index);
                    }
                    
                    if (!CUI.util.isTouch)
                    {
                        $(this).fipo("touchstart", "mousedown", function(event) {
                            that.$handles.eq(index).focus();
                        }.bind(this));
                    }
                });
            }
            
            // if the input is contained by a label
            if ($this.parent().is("label")) {
                $label = $this.parent();

                // make sure it has an id
                if (!$label.attr("id")) {
                    $label.attr("id",thisId+"-label");
                }
                
                // make sure it explicitly identifies the input it labels
                if (!$label.attr("for")) {
                    $label.attr("for",thisId);
                }

                // move the input after the label
                $this.insertAfter($label);
                
                // if there is a legend, this is a two thumb slider; internal labels identify the minimum and maximum, and they should have the class="hidden-accessible" 
                if ($legend) {
                    $label.addClass("hidden-accessible");
                }
                
                // move the label outside the slider element tag
                $label.insertBefore(that.$element);
            }
            
            // if the input has a label and it is not included in the aria-labelledby attribute, add the label id to the "aria-labelledby" attribute
            if ($label.length && $this.attr("aria-labelledby").indexOf($label.attr("id"))===-1)
            {
                $this.attr("aria-labelledby", $this.attr("aria-labelledby")+($this.attr("aria-labelledby").length ? " ":"")+$label.attr("id"));
            }
            
            if ($label.length===0 && $this.attr("aria-labelledby").length>0)
            {
                $label = $("#"+$this.attr("aria-labelledby").split(" ")[0]);
            }
            
            if ($this.attr("aria-labelledby").length===0)
            {
                $this.removeAttr("aria-labelledby");
            }

            // setting default step
            if (!$this.is("[step]")) $this.attr('step', that.options.step);

            // setting default min
            if (!$this.is("[min]")) $this.attr('min', that.options.min);

            // setting default max
            if (!$this.is("[max]")) $this.attr('max', that.options.max);

            // setting default value
            if (!$this.is("[value]")) {
                $this.attr({'value':that.options.value,'aria-valuetext':that.options.valuetextFormatter(that.options.value)});
                values.push(that.options.value);
            } else {
                values.push($this.attr('value'));
            }

            if(index === 0) {
                if($this.is(":disabled")) {
                    that.options.disabled = true;
                    that.$element.addClass("disabled");
                } else {
                    if(that.options.disabled) {
                        $this.attr("disabled","disabled");
                        that.$element.addClass("disabled");
                    }
                }
            }            
            
            if (CUI.util.isTouch)
            {
                // handle input value changes 
                $this.on("change", function(event) {
                    if (that.options.disabled) return;
                    if ($this.val()===that.values[index]) return;
                    that.setValue($this.val(), index);
                }.bind(this));
                
                // On mobile devices, the input receives focus; listen for focus and blur events, so that the parent style updates appropriately.
                $this.on("focus", function(event) {
                    that._focus(event);
                }.bind(this));

                $this.on("blur", function(event) {
                    that._blur(event);
                }.bind(this));
            } else {
                // on desktop, we don't want the input to receive focus
                $this.attr({"aria-hidden":true,"tabindex":-1,"hidden":"hidden"});
                
                if (index===0) {
                    if ($label) {
                        $label.on("click", function(event) {
                           if (that.options.disabled) return;
                           that._clickLabel(event);
                        }.bind(this));
                    }
                    
                    if ($legend) {
                        $legend.on("click", function(event) {
                            if (that.options.disabled) return;
                            that._clickLabel(event);
                        }.bind(this));
                    }
                }
            }
        });

        that.values = values;
        if (this.options.orientation === 'vertical') this.isVertical = true;
        
        // Set up event handling
        this.$element.fipo("touchstart", "mousedown", function(event) {
            this._mouseDown(event);
        }.bind(this));

        // Listen to changes to configuration
        this.$element.on('change:value', this._processValueChanged.bind(this));
        this.$element.on('change:disabled', this._processDisabledChanged.bind(this));      
        this.$element.on('change:min', this._processMinMaxStepChanged.bind(this));      
        this.$element.on('change:max', this._processMinMaxStepChanged.bind(this));      
        this.$element.on('change:step', this._processMinMaxStepChanged.bind(this));
                              
        // Adjust dom to our needs
        this._render();
    },
    
    defaults: {
      step: '1',
      min: '1',
      max: '100',
      value: '1',
      orientation: 'horizontal',
      slide: false,
      disabled: false,
      tooltips: false,
      tooltipFormatter: function(value) { return value.toString(); },
	  valuetextFormatter: function(value) { return value.toString(); },
      ticks: false,
      filled: false,
      bound: false
    },

    values: [],
    $inputs: null,
    $ticks: null,
    $fill: null,
    $handles: null,
    $tooltips: null,
    isVertical: false,
    draggingPosition: -1,
    
    /**
     * Set the current value of the slider
     * @param {int}   value   The new value for the slider
     * @param {int}   handleNumber   If the slider has 2 handles, you can specify which one to change, either 0 or 1
     */
    setValue: function(value, handleNumber) {
        handleNumber = handleNumber || 0;
        
        this._updateValue(handleNumber, value, true); // Do not trigger change event on programmatic value update!
        this._moveHandles();
        if(this.options.filled) {
            this._updateFill();
        }        
    },
    
    _renderMissingElements: function() {
        if (!this.$element.find("input").length) {
            var that = this,
                el, 
                values = ($.isArray(this.options.value)) ? this.options.value : [this.options.value];
                $.each(values, function(index, value) {
                    el = $("<input>");
                    el.attr({
                        "type": "range",
                        "min": that.options.min,
                        "max": that.options.max,
                        "step": that.options.step,
                        "value": value              
                    }).val(value);
                    that.$element.append(el);
                });
        }
        
        if (!this.$element.find("div.clickarea").length) {
            var el2 = $("<div class=\"clickarea\">");
            this.$element.prepend(el2); // Prepend: Must be first element to not hide handles!
        }
        
        this.$element.toggleClass("slider", true);
        this.$element.toggleClass("vertical", this.options.orientation === 'vertical' );
        this.$element.toggleClass("tooltips", this.options.tooltips);
        this.$element.toggleClass("ticked", this.options.ticks);
        this.$element.toggleClass("filled", this.options.filled);                                
    },
    
    _processValueChanged: function() {
        var that = this, 
            values = ($.isArray(this.options.value)) ? this.options.value : [this.options.value];
        $.each(values, function(index, value) {
            that._updateValue(index, value, true); // Do not trigger change event on programmatic value update!
        });
        this._moveHandles();
        if(this.options.filled) {
            this._updateFill();
        }   
    },

    _processMinMaxStepChanged: function() {
        var that = this;
        this.$element.find("input").attr("min", this.options.min);
        this.$element.find("input").attr("max", this.options.max);
        this.$element.find("input").attr("step", this.options.step);
        
        $.each(this.values, function(index, value) {
            that._updateValue(index, value, true); // Ensure current values are between min and max
        });
        
        if(this.options.ticks) {
            this.$element.find(".ticks").remove();
            this._buildTicks();
        }
        
        if(this.options.filled) {
            this.$element.find(".fill").remove();
            this._buildFill();
        }
        
        this._moveHandles();
        if(this.options.filled) {
            this._updateFill();
        }   
    },
        
    _processDisabledChanged: function() {
        if (this.options.disabled) {
            this.$inputs.attr("disabled","disabled");
            this.$handles.each(function() {
                $(this).removeClass("focus");
                $(this).parent().removeClass("focus");
            });            
            if (CUI.util.isTouch) 
                this.$handles.attr("aria-disabled",true).removeAttr("tabindex");
        } else {
            this.$inputs.removeAttr("disabled");
            if (CUI.util.isTouch)
                this.$handles.removeAttr("aria-disabled").attr("tabindex",0);
        }
        this.$element.toggleClass("disabled", this.options.disabled);                 
    },   
    _render: function() {
        var that = this;

        // get maximum max value
        var maximums = that.$inputs.map(function() {return $(this).attr('max');});
        that.options.max = Math.max.apply(null, maximums.toArray());

        // get minimum min value
        var minimums = that.$inputs.map(function() {return $(this).attr('min');});
        that.options.min = Math.min.apply(null, minimums.toArray());

        // get minimum step value
        var steps = that.$inputs.map(function() {return $(this).attr('step');});
        that.options.step = Math.min.apply(null, steps.toArray());

        // Todo: do not add already existing elements or remove them before adding new elements
        // build ticks if needed
        if(that.options.ticks) {
            that._buildTicks();
        }

        // build fill if needed
        if(that.options.filled) {
            that._buildFill();
        }

        that._buildHandles();
    },

    _buildTicks: function() {
        var that = this;

        // The ticks holder
        var $ticks = $("<div></div>").addClass('ticks');
        this.$element.prepend($ticks);

        var numberOfTicks = Math.round((that.options.max - that.options.min) / that.options.step) - 1;
        var trackDimensions = that.isVertical ? that.$element.height() : that.$element.width();
        for (var i = 0; i < numberOfTicks; i++) {
            var position = (i+1) * (trackDimensions / (numberOfTicks+1));
            var percent = (position / trackDimensions) * 100;
            var tick = $("<div></div>").addClass('tick').css((that.isVertical ? 'bottom' : 'left'), percent + "%");
            $ticks.append(tick);
        }
        that.$ticks = $ticks.find('.tick');
        if(that.options.filled) {
            that._coverTicks();
        }
    },

    _buildFill: function() {
        var that = this;

        this.$fill = $("<div></div>").addClass('fill');

        if(that.values.length !== 0) {
            var percent, fillPercent;
            if(that.values.length < 2) {
                percent = (that.values[0] - that.options.min) / (that.options.max - that.options.min) * 100;
                this.$fill.css((that.isVertical ? 'height' : 'width'), percent + "%");
            } else {
                percent = (this._getLowestValue() - that.options.min) / (that.options.max - that.options.min) * 100;
                fillPercent = (this._getHighestValue() - this._getLowestValue()) / (that.options.max - that.options.min) * 100;
                this.$fill.css((that.isVertical ? 'height' : 'width'), fillPercent + "%")
                    .css((that.isVertical ? 'bottom' : 'left'), percent + "%");
            }
        }
        this.$element.prepend(this.$fill);
        that.options.filled = true;
    },

    _buildHandles: function() {
        var that = this;

        // Wrap each input field and add handles and tooltips (if required)
        that.$inputs.each(function(index) {

            var wrap = $(this).wrap("<div></div>").parent().addClass("value");

            // Add handle for input field
            var percent = (that.values[index] - that.options.min) / (that.options.max - that.options.min) * 100;
            var handle = $('<div></div>').addClass('handle').css((that.isVertical ? 'bottom' : 'left'), percent + "%")
            .attr({
                "role":"slider",
                "id":$(this).attr("id")+"-handle",
                "aria-valuemin":that.options.min,
                "aria-valuemax":that.options.max,
                "aria-valuenow":that.values[index],
                "aria-valuetext":that.options.valuetextFormatter(that.values[index])
            });
            
            // position the input relative to the slider container element
            $(this).css((that.isVertical ? 'bottom' : 'left'), percent + "%");
            $(wrap).append(handle);

            // Add tooltip to handle if required
            if(that.options.tooltips) {
                var tooltip = $("<output>" + $(this).attr('value') + "</output>").addClass('tooltip').addClass(that.isVertical ? 'arrow-left' : 'arrow-bottom')
                .attr({'id':$(this).attr("id")+"-tooltip",'for':$(this).attr("id")});
                handle.append(tooltip);
            }
            
            if ($(this).attr("aria-labelledby")) {
                handle.attr("aria-labelledby",$(this).attr("aria-labelledby")); 
            }
            
            if (that.$inputs.length>1 && $(this).attr("aria-labelledby")) {
                var inputlabelids = $(this).attr("aria-labelledby").split(" "),
                    label;
                for(var i = 0; i<inputlabelids.length; i++)
                {
                    label = $("#"+inputlabelids[i]);
                    if (i>0)
                    {
                        label.removeAttr("for");
                        handle.prepend(label);
                    }
                }
            }
            
            if (CUI.util.isTouch) {
                handle.attr("aria-hidden", true);
                $(this).attr("tabindex", 0).removeAttr("aria-hidden").removeAttr("hidden");
            } else {
                handle.on("focus", function(event) {
                    that._focus(event);
                }.bind(this));

                handle.on("blur", function(event) {
                    that._blur(event);
                }.bind(this));

                handle.on("keydown", function(event) {
                    that._keyDown(event);
                }.bind(this));

                handle.attr("tabindex",0);
                $(this).attr({"aria-hidden":true,"tabindex":-1,"hidden":"hidden"});
            }
            
            if (that.options.disabled) {
                handle.attr("aria-disabled",true).removeAttr("tabindex");
            }
        });

        that.$handles = that.$element.find('.handle');
        that.$tooltips = that.$element.find('.tooltip');
    },
    
    _handleClick: function(event) {
        if(this.options.disabled) return false;
        var that = this;

        // Mouse page position
        var mouseX = event.pageX;
        var mouseY = event.pageY;
            
        if (event.type === "touchstart") {
            var touches = (event.originalEvent.touches.length > 0) ? event.originalEvent.touches : event.originalEvent.changedTouches;
            mouseX = touches[0].pageX;
            mouseY = touches[0].pageY;
        }

        if (mouseX === undefined || mouseY === undefined) return; // Do not use undefined values!

        // Find the nearest handle
        var pos = that._findNearestHandle(mouseX, mouseY);
        
        var val = that._getValueFromCoord(mouseX, mouseY, true);

        if (!isNaN(val))
        {
            that._updateValue(pos, val);
            that._moveHandles();
            if(that.options.filled) {
                that._updateFill();
            }
        }
        
        if (!CUI.util.isTouch) {
            if (event.type === "mousedown") {
                that.$handles.eq(pos).data("mousedown", true);
            }
            that.$handles.eq(pos).focus();
        }            
    },
    
    _findNearestHandle: function(mouseX, mouseY) {
        var that = this;

        var closestDistance = 999999; // Incredible large start value

        // Find the nearest handle
        var pos = 0;
        that.$handles.each(function(index) {

            // Handle position
            var handleX = $(this).offset().left;
            var handleY = $(this).offset().top;

            // Handle Dimensions
            var handleWidth = $(this).width();
            var handleHeight = $(this).height();

            // Distance to handle
            var distance = Math.abs(mouseX - (handleX+(handleWidth/2)));
            if(that.options.orientation === "vertical") {
                distance = Math.abs(mouseY - (handleY+(handleHeight/2)));
            }

            if(distance < closestDistance) {
                closestDistance = distance;
                pos = index;
            }
        });

        return pos;
    },
    
    _focus: function(event) {
        if (this.options.disabled) return false;
        var that = this,
            $this = $(event.target),
			$value = $this.closest(".value"),
			$handle = $value.find(".handle");
		if (!$handle.data("mousedown")) {
            that.$element.addClass("focus");
            $value.addClass("focus");
			$handle.addClass("focus");
		}
    },

    _blur: function(event) {
        if (this.options.disabled) return false;
        var that = this,
        $this = $(event.target),
		$value = $this.closest(".value"),
		$handle = $value.find(".handle");
        that.$element.removeClass("focus");
		$value.removeClass("focus");
		$handle.removeClass("focus").removeData("mousedown");
    },
    
    _keyDown: function(event) {
        if (this.options.disabled) return;
        var that = this,
            $this = $(event.target),
            $input = $this.closest(".value").find("input"),
            index = that.$inputs.index($input),
            val = Number($input.val()),
            step = Number(that.options.step),
            minimum = Number(that.options.min),
            maximum = Number(that.options.max),
            page = Math.max(step,Math.round((maximum-minimum)/10));
		
		$this.removeData("mousedown");
		that._focus(event);
		
        switch(event.keyCode) {
            case 40:
            case 37:
                // down/left
                val-=step;
                event.preventDefault();
                break;
            case 38:
            case 39:
                // up/right
                val+=step;
                event.preventDefault();
                break; 
            case 33:
                // page up
                val+=(page-(val%page));
                event.preventDefault();
                break;
            case 34:
                // page down
                val-=(page- (val%page===0 ? 0 : page-val%page));
                event.preventDefault();
                break;
            case 35:
                // end
                val = maximum;
                event.preventDefault();
                break;
            case 36:
                // home
                val = minimum;
                event.preventDefault();
                break;
        }
        if (val !== Number($input.val())) {
            that.setValue(val, index);
            $input.change();
        }
    },

    _mouseDown: function(event) {
        if(this.options.disabled) return false;
        event.preventDefault();
        
        var that = this, $handle;
        
        this.draggingPosition = -1;
        this.$handles.each(function(index, handle) {
            if (handle === event.target) that.draggingPosition = index;
        }.bind(this));
        
        this.$tooltips.each(function(index, tooltip) {
            if (tooltip === event.target) that.draggingPosition = index;
        }.bind(this));
        
        // Did not touch any handle? Emulate click instead!
        if (this.draggingPosition < 0) {
            this._handleClick(event);
            return;
        }
        
        $handle = this.$handles.eq(this.draggingPosition);
        
        $handle.addClass("dragging");
        this.$element.closest("body").addClass("slider-dragging-cursorhack");
        
        $(window).fipo("touchmove.slider", "mousemove.slider", this._handleDragging.bind(this));
        $(window).fipo("touchend.slider", "mouseup.slider", this._mouseUp.bind(this));

        if ($handle !== document.activeElement && !CUI.util.isTouch) {
            if (event.type === "mousedown") {
                $handle.data("mousedown", true);
            }
            $handle.focus();
        }
        //update();
    },
    
    _handleDragging: function(event) {
        var mouseX = event.pageX;
        var mouseY = event.pageY;
        
        // Handle touch events
        if (event.originalEvent.targetTouches) {
            var touch = event.originalEvent.targetTouches.item(0);
            mouseX = touch.pageX;
            mouseY = touch.pageY;            
        }
        
        this._updateValue(this.draggingPosition, this._getValueFromCoord(mouseX, mouseY));      
        this._moveHandles();
        if(this.options.filled) {
            this._updateFill();
        }
        event.preventDefault();
    },

    _mouseUp: function() {
        this.$handles.eq(this.draggingPosition).removeClass("dragging");
        this.$element.closest("body").removeClass("slider-dragging-cursorhack");
        
        this.draggingPosition = -1;
        $(window).unbind("mousemove.slider touchmove.slider");
        $(window).unbind("mouseup.slider touchend.slider");
    },
    
    _clickLabel: function(event) {
        this.$handles.eq(0)[0].focus();
    },

    _updateValue: function(pos, value, doNotTriggerChange) {
        var that = this;
        if (that.$inputs.eq(pos).attr("value") !== value.toString() || (that.values[pos] !== value.toString())) {
            if (value > this.options.max) value = this.options.max;
            if (value < this.options.min) value = this.options.min;

            if(pos === 0 || pos === 1) {
                if (that.$inputs.length===2 && this.options.bound)
                {
                    if (pos===0) {
                        value = Math.min(value, Number(that.$inputs.eq(1).val()));
                        that.$inputs.eq(1).attr({"min":value});
                        that.$inputs.eq(pos).attr({"max":that.$inputs.eq(1).val()});
                        that.$handles.eq(1).attr({"aria-valuemin":value});
                        that.$handles.eq(pos).attr({"aria-valuemax":that.$inputs.eq(1).val()});
                    } else {
                        value = Math.max(value, Number(that.$inputs.eq(0).val()));
                        that.$inputs.eq(0).attr({"max":value});
                        that.$inputs.eq(pos).attr({"min":that.$inputs.eq(0).val()});
                        that.$handles.eq(0).attr({"aria-valuemax":value});
                        that.$handles.eq(pos).attr({"aria-valuemin":that.$inputs.eq(0).val()});
                    }
                }
                that.values[pos] = value.toString();
                that.$inputs.eq(pos).val(value).attr({"value":value,"aria-valuetext":that.options.valuetextFormatter(value)});
                that.$handles.eq(pos).attr({"aria-valuenow":value,"aria-valuetext":that.options.valuetextFormatter(value)});
                if (!doNotTriggerChange) {
                    setTimeout(function() {
                        that.$inputs.eq(pos).change(); // Keep input element value updated too and fire change event for any listeners
                    }, 1); // Not immediatly, but after our own work here
                }
            }
        }
    },

    _moveHandles: function() {
        var that = this;

        // Set the handle position as a percentage based on the stored values
        this.$handles.each(function(index) {
            var percent = (that.values[index] - that.options.min) / (that.options.max - that.options.min) * 100;
            var $input = that.$inputs.eq(index);

            if(that.options.orientation === "vertical") {
                if(that.options.slide) {
                    $(this).stop().animate({bottom: percent + "%"});
                    $input.stop().animate({bottom: percent + "%"});
                } else {
                    $(this).css("bottom", percent + "%");
                    $input.css("bottom", percent + "%");
                }
            } else { // Horizontal
                if(that.options.slide) {
                    $(this).stop().animate({left: percent + "%"});
                    $input.stop().animate({left: percent + "%"});
                } else {
                    $(this).css("left", percent + "%");
                    $input.css("left", percent + "%");
                }
            }

            // Update tooltip value (if required)
            if(that.options.tooltips) {
                that.$tooltips.eq(index).html(that.options.tooltipFormatter(that.values[index]));
            }
        });
    },

    _updateFill: function() {
        var that = this;
        var percent;

        if(that.values.length !== 0) {
            if(that.values.length === 2) { // Double value/handle
                percent = ((that._getLowestValue() - that.options.min) / (that.options.max - that.options.min)) * 100;
                var secondPercent = ((that._getHighestValue() - that.options.min) / (that.options.max - that.options.min)) * 100;
                var percentDiff = secondPercent - percent;
                if(that.options.orientation === "vertical") {
                    if(that.options.slide) {
                        that.$fill.stop().animate({bottom: percent + "%", height: percentDiff + "%"});
                    } else {
                        that.$fill.css("bottom", percent + "%").css("height", percentDiff + "%");
                    }
                } else { // Horizontal
                    if(that.options.slide) {
                        that.$fill.stop().animate({left: percent + "%", width: percentDiff + "%"});
                    } else {
                        that.$fill.css("left", percent + "%").css("width", percentDiff + "%");
                    }
                }
            } else { // Single value/handle
                percent = ((that.values[0] - that.options.min) / (that.options.max - that.options.min)) * 100;
                if(that.options.orientation === "vertical") {
                    if(that.options.slide) {
                        that.$fill.stop().animate({height: percent + "%"});
                    } else {
                        that.$fill.css("height", percent + "%");
                    }
                } else {
                    if(that.options.slide) {
                        that.$fill.stop().animate({width: percent + "%"});
                    } else {
                        that.$fill.css("width", percent + "%");
                    }
                }
            }
        }
        if(that.options.ticks) {
            that._coverTicks();
        }
    },

    _coverTicks: function() {
        var that = this;

        // Ticks covered by the fill are given 'covered' class
        that.$ticks.each(function(index) {
            var value = that._getValueFromCoord($(this).offset().left, $(this).offset().top);
            if(that.values.length === 2) { // TODO add a parameter to indicate multi values/handles
                if((value > that._getLowestValue()) && (value < that._getHighestValue())) {
                    if(!$(this).hasClass('covered')) $(this).addClass('covered');
                } else {
                    $(this).removeClass('covered');
                }
            } else {
                if(value < that._getHighestValue()) {
                    if(!$(this).hasClass('covered')) $(this).addClass('covered');
                } else {
                    $(this).removeClass('covered');
                }
            }
        });
    },

    _getValueFromCoord: function(posX, posY, restrictBounds) {
        var that = this;
        var percent, snappedValue, remainder;
        var elementOffset = that.$element.offset();

        if(that.options.orientation === "vertical") {
            var elementHeight = that.$element.height();
            percent = ((elementOffset.top + elementHeight) - posY) / elementHeight;
        } else {
            var elementWidth = that.$element.width();
            percent = ((posX - elementOffset.left) / elementWidth);
        }
        
        // if the bounds are retricted, as with _handleClick, we souldn't change the value.
        if (restrictBounds && (percent<0 || percent>1)) return NaN;
        
        var rawValue = that.options.min * 1 + ((that.options.max - that.options.min) * percent);

        if(rawValue >= that.options.max) return that.options.max;
        if(rawValue <= that.options.min) return that.options.min;

        // Snap value to nearest step
        remainder = ((rawValue - that.options.min) % that.options.step);
        if(Math.abs(remainder) * 2 >= that.options.step) {
            snappedValue = (rawValue - remainder) + (that.options.step * 1); // *1 for IE bugfix: Interpretes expr. as string!
        } else {
            snappedValue = rawValue - remainder;
        }
        
        return snappedValue;
    },

    _getHighestValue: function() {
      return Math.max.apply(null, this.values);
    },

    _getLowestValue: function() {
      return Math.min.apply(null, this.values);
    }

    /*update: function() {
        // TODO: Single update method
    }*/
  });



  CUI.util.plugClass(CUI.Slider);

  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
        $(".slider[data-init~='slider']", e.target).slider();
    });
  }
}(window.jQuery));


/*


 <!--section id="Slider">
 <h2 class="line">Slider</h2>
 <p>Different types of sliders.</p>

 <div class="componentSample">
 <div class="example">
 <div class="filterSample">
 <div class="sampleTitle left">Standard</div>
 <div class="slider horizontal">
 <div class="range" style="width: 20%"></div>
 <span class="tick" style="left: 20%"></span>
 <div class="handle" style="left: 45%"></div>
 <div class="tooltip info arrow-left">
 <span>45%</span>
 </div>
 </div>

 </div>
 </div>

 </section-->

    */

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




(function($) {
  var datepicker_guid = 0;
  
  CUI.Datepicker = new Class(/** @lends CUI.Datepicker# */{
    toString: 'Datepicker',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc A datepicker widget
         
        <p>
            <div class="datepicker" data-init="datepicker">
                <input type="datetime" value="1987-04-06T20:35Z">
                <button><span class="icon-calendar small">Datetime picker</span></button>
            </div>
        </p>

        @example
        <caption>Instantiate by data API</caption>
        &lt;div class=&quot;datepicker&quot; data-init=&quot;datepicker&quot;&gt;
            &lt;input type=&quot;datetime&quot; value=&quot;1987-04-06T20:35Z&quot;&gt;
            &lt;button&gt;&lt;span class=&quot;icon-calendar small&quot;&gt;Datetime picker&lt;/span&gt;&lt;/button&gt;
        &lt;/div&gt;
        
Currently there are the following data options:
  data-init="datepicker"         Inits the datepicker widget after page load
  data-disabled                  Sets field to "disabled" if given (with any non-empty value)
  data-required                  Sets field to "required" if given (with any non-empty value)
  data-stored-format             Sets the format of the date for transferring it to the server
  data-displayed-format          Sets the format of the date for displaying it to the user
  data-force-html-mode           Force to HTML mode and never use a native Date widget, if given (with any non-empty value)
  data-day-names                 JSON-array-data with the short names of all week days, starting with Sunday
  data-month-names               JSON-array-data with the names of all months, starting with January
  data-head-format               Defines headline format, default is "MMMM YYYY".
  data-start-day                 Defines the start day of the week, 0 = Sunday, 1 = Monday, etc.

Additionally the type (date, time, datetime) is read from the &lt;input&gt; field.

        @example
        <caption>Instantiate with Class</caption>
        var datepicker = new CUI.Datepicker({
          element: '#myOrdinarySelectBox'
        });

        @example
        <caption>Instantiate by jQuery plugin</caption>
        $("div.datepicker").datepicker();
           
       
      @desc Creates a datepicker from a div element
      @constructs

      @param {Object}  options                                                     Component options
      @param {Array}   [options.monthNames=english names]                          Array of strings with the name for each month with January at index 0 and December at index 11
      @param {Array}   [options.dayNames=english names]                            Array of strings with the name for each weekday with Sun at index 0 and Sat at index 6
      @param {String}  [options.type="date"]                                       Type of picker, supports date, datetime, datetime-local and time
      @param {integer} [options.startDay=0]                                        Defines the start day for the week, 0 = Sunday, 1 = Monday etc.
      @param {boolean} [options.disabled=false]                                    Is this widget disabled?
      @param {String}  [options.displayedFormat="YYYY-MM-DD[T]HH:mm[Z]"]           Displayed date (userfriendly), default is 2012-10-20 20:35
      @param {String}  [options.storedFormat="YYYY-MM-DD[T]HH:mmZ"]                Storage Date format, is never shown to the user, but transferred to the server 
      @param {String}  [options.required=false]                                    Is a value required?
      @param {String}  [options.hasError=false]                                    True to display widget as erroneous, regardless if the value is required or not.
      @param {String}  [options.minDate]                                           Defines the start date of selection range. Dates earlier than minDate are not selectable. 
                                                                                   It must be expressed in officialDateFormat (YYYY-MM-DD) or as "today".
      @param {String}  [options.maxDate]                                           Defines the end date of selection range. Dates later than maxDate are not selectable. 
                                                                                   It must be expressed in officialDateFormat (YYYY-MM-DD) or as "today".
      @param {String}  [options.headFormat="MMMM YYYY"]                            Defines calendar headline format, default is "MMMM YYYY"
      @param {boolean} [options.forceHTMLMode=false]                               Force to HTML mode and never use a native Date widget, if given (with any non-empty value)
      @param {String}  [options.selectedDateTime]                                  Defines what date/time will be selected when the calendar is rendered. If nothing is specified it will be 
                                                                                   considerend today or current time.
    */
    
    defaults: {
        monthNames: null,
        dayNames: null,
        format: null,
        type: "date",
        selectedDateTime: null,
        startDay: 0,
        disabled: false,
        displayedFormat: null,
        storedFormat: null,
        headFormat: "MMMM YYYY",
        forceHTMLMode: false,
        required: false,
        hasError: false,
        minDate: null,
        maxDate: null
    },
    
    displayDateTime: null,
    pickerShown: false,
    useNativeControls: false,
    internFormat: 'YYYY-MM-DD[T]HH:mmZ',
    officialDateFormat: 'YYYY-MM-DD',
    officialTimeFormat: 'HH:mm',
    officialDatetimeFormat: 'YYYY-MM-DD[T]HH:mmZ',
    language: 'coralui',

    construct: function(options) {
        
        this.guid = (datepicker_guid += 1);
        
        this._readDataFromMarkup();
        this._adjustMarkup();

        this.options.monthNames = this.options.monthNames || CUI.Datepicker.monthNames;
        this.options.dayNames = this.options.dayNames || CUI.Datepicker.dayNames;

        // Generate our very own language name for this picker to not overwrite any existing
        // moment.js language definition
        this.language = 'coralui_' + new Date().getTime();
        
        moment.lang(this.language, {
           months: this.options.monthNames,
           weekdaysMin: this.options.dayNames,
        });
        
        // Set standard formats
        this.options.storedFormat = this.options.storedFormat || (this.options.type === "time" ? 'HH:mm' : 'YYYY-MM-DD[T]HH:mmZ');
        this.options.displayedFormat = this.options.displayedFormat || (this.options.type === "time" ? 'HH:mm' : 'YYYY-MM-DD HH:mm');
        
        if(this._isSupportedMobileDevice() && this._supportsInputType(this.options.type)) {
            this.useNativeControls = true;
        }

        this.$input = this.$element.find('input').not("[type=hidden]");
        this.$hiddenInput = this.$element.find('input[type=hidden]');
        this.$popover = $('body').find(".popover#"+this.guid);
        this.$openButton = this.$element.find('button');
        
        this._updateState();

        // Enable time buttons in popover
        if(this._isTimeEnabled()) {
            this._renderTime();
        }

        // If HTML5 input is used, then force to use the official format.
        if (this.useNativeControls) {
            if (this.options.type === 'date') {
                this.options.displayedFormat = this.officialDateFormat;
            } else if (this.options.type === 'time') {
                this.options.displayedFormat = this.officialTimeFormat;
            } else {
                this.options.displayedFormat = this.officialDatetimeFormat;
            }
        }
        
        if(this.options.minDate !== null){
            if(this.options.minDate === "today"){
                this.options.minDate = moment().startOf("day");
            }else{
                if(moment(this.options.minDate, this.officialDateFormat).isValid()){
                    this.options.minDate = moment(this.options.minDate, this.officialDateFormat);
                }else{
                    this.options.minDate = null;
                }
            }
        }
        
        if(this.options.maxDate !== null){
            if(this.options.maxDate === "today"){
                this.options.maxDate = moment().startOf("day");
            }else{
                if(moment(this.options.maxDate, this.officialDateFormat).isValid()){
                    this.options.maxDate = moment(this.options.maxDate, this.officialDateFormat);
                }else{
                    this.options.maxDate = null;
                }
            }
        }
        
        if(!this.useNativeControls) {
            this._switchInputTypeToText(this.$input);
        }

        var timeout = null;
        var $input = this.$input;
        var $btn = this.$openButton;

        if (!this.options.disabled) {
            $('body').on('click', function(){
                if (this.keepShown === false) {
                    this._hidePicker();
                }
            }.bind(this));
            
            this.$element.on('click', function(event){
                var $target = $(event.target);
                if ($target.is(".icon-calendar, .icon-clock") || $target.children().is(".icon-calendar, .icon-clock")) {
                    if (!this.pickerShown) {
                        this._openPicker();
                    } else {
                        this._hidePicker();
                    }
                }

                // let the event time to propagate.
                // Do not use stopPropagation, as other Datepickers might want to use this event for closing their own datepicker
                this.keepShown = true;
                setTimeout(function() {
                    this.keepShown = false;
                }.bind(this), 200);

            }.bind(this));
            
            this.$popover.on('click', function(event){
                this.keepShown = true;
                setTimeout(function() {
                    this.keepShown = false;
                }.bind(this), 200);

            }.bind(this));
        }

        // Listen on change and additional on blur for mobile, as iPad does not fire change events for date fields.  
        $input.on("change" + (this._isSupportedMobileDevice() ? " blur" : ""), function() {
            if (this.options.disabled) return;
            var newDate = moment(this.$input.val(), this.options.displayedFormat);
            if(newDate !== null && !this._isDateInRange(newDate, this.options.minDate, this.options.maxDate)){
                this.options.hasError = true;
            }else{
                this.options.hasError = false;
            }
            this._setDateTime(newDate, true); // Set the date, but don't trigger a change event
        }.bind(this));

        function normalizeDate(date) {
            if (!date) return null;
            return moment([date.year(), date.month(), date.date()]);
        }

        // Move around
        this.$popover.find(".calendar").on("swipe", function(event) {
            var d = event.direction;
            if (d === "left") {
                this.displayDateTime = normalizeDate(moment([this.displayDateTime.year(), this.displayDateTime.month() + 1, 1]));
                this._renderCalendar("left");                
            } else if (d === "right") {
                this.displayDateTime = normalizeDate(moment([this.displayDateTime.year(), this.displayDateTime.month() - 1, 1]));
                this._renderCalendar("right");                
            }         
        }.bind(this));

        this.$popover.on("mousedown", ".next-month", function(event) {
            event.preventDefault();
            if (!this.displayDateTime) return;
            this.displayDateTime = normalizeDate(moment([this.displayDateTime.year(), this.displayDateTime.month() + 1, 1]));
            this._renderCalendar("left");
        }.bind(this));

        this.$popover.on("mousedown", ".prev-month", function(event) {
            event.preventDefault();
            if (!this.displayDateTime) return;
            this.displayDateTime = normalizeDate(moment([this.displayDateTime.year(), this.displayDateTime.month() - 1, 1]));
            this._renderCalendar("right");
        }.bind(this));

        if(this._isTimeEnabled()) {
            var dropdownChanged = function () {
                var h = this._getHoursFromDropdown();
                var m = this._getMinutesFromDropdown();
                if (!this.options.selectedDateTime) this.options.selectedDateTime = moment();
                var date = this.options.selectedDateTime.hours(h).minutes(m);

               this._setDateTime(date);
            };

            // for Desktop
            this.$popover.on("dropdown-list:select", ".hour,.minute", dropdownChanged.bind(this));
            // for Mobile
            this.$popover.on("change", ".hour,.minute", dropdownChanged.bind(this));
        }


        if (this.useNativeControls) {
            this.displayDateTime = this.options.selectedDateTime = moment(this.$input.val(), this.options.displayedFormat);
        }
        
        // Reading input value for the first time -> there may be a storage format
        if (!this.options.selectedDateTime) this._readInputVal([this.options.storedFormat, this.options.displayedFormat]);
        this._setDateTime(this.options.selectedDateTime, true);
    },
    
    _readDataFromMarkup: function() {
        
        if (this.$element.data("disabled")) {
            this.options.disabled = true;
        }
        
        if (this.$element.hasClass("error")) {
            this.options.hasError = true;
        }
                
        if (this.$element.data('required')) {
            this.options.required = true;
        }

        var $input = $(this.$element.find("input").filter("[type^=date],[type=time]"));
        if ($input.length !== 0) {
            this.options.type = $input.attr("type");
        }

        var el = this.$element;
        if (el.data('displayed-format') !== undefined) {
            this.options.displayedFormat = el.data('displayed-format');
        }

        if (el.data('stored-format') !== undefined) {
            this.options.storedFormat = el.data('stored-format');
        }

        if (el.data('force-html-mode') !== undefined) {
            this.options.forceHTMLMode = el.data('force-html-mode');
        }
        
        if (el.data('day-names') !== undefined) {
            this.options.dayNames = el.data('day-names') || this.options.dayNames;
        }
        
        if (el.data('month-names') !== undefined) {
            this.options.monthNames = el.data('month-names') || this.options.monthNames;
        }
        
        if (el.data('head-format') !== undefined) {
            this.options.headFormat = el.data('head-format') || this.options.headFormat;
        }
               
        if (el.data('start-day') !== undefined) {
            this.options.startDay = el.data('start-day') * 1; // Force number
        }   
        if (el.data('min-date') !== undefined) {
            this.options.minDate = el.data('min-date');
        }
        if (el.data('max-date') !== undefined) {
            this.options.maxDate = el.data('max-date');
        }
    },

    _readInputVal: function(format) {
        if (!format) format = this.options.displayedFormat;
        var value = this.$input.eq(0).val();
        var date = moment(value, format);
        if (!date || !date.isValid()) date = moment(value); // Fallback: Try automatic guess if none of our formats match
        this.displayDateTime = this.options.selectedDateTime = date;
    },
    
    _updateState: function() {
        if (this.options.disabled) {
            this.$element.find("input,button").attr("disabled", "disabled");
            this._hidePicker();
        } else {
            this.$element.find("input,button").removeAttr("disabled");
        }

        if (this.options.hasError || (!this.options.selectedDateTime && this.options.required) || (this.options.selectedDateTime && !this.options.selectedDateTime.isValid())) {
            this.$element.addClass("error");
        } else {
            this.$element.removeClass("error");
        }
    },

    _switchInputTypeToText: function($input) {
        var convertedInput = $input.detach().attr('type', 'text');
        // readonly to hide the keyboard
        // convertedInput.attr('readonly', 'true'); // Removed, we want to edit dates manually!
        this.$element.prepend(convertedInput);
    },

    _openNativeInput: function() {
        this.$input.trigger("tap");
    },
    
    _keyPress: function() {
        if (!this.pickerShown) return;
        
        // TODO: Keyboard actions
    },

    _openPicker: function() {
        this.$element.addClass("focus");
        this.$input.parents().on('scroll', this._scrollParents.bind(this));

        if(!this.useNativeControls) {
            this._readInputVal();
            this._showPicker();
        } else {
            this._openNativeInput();
        }
    },
    
    _scrollParents: function(event) {
        this._hidePicker();
    },
    
    _showPicker: function() {
        if(this._isDateEnabled()) this._renderCalendar();
        
        var left = this.$openButton.offset().left + this.$openButton.width() / 2 - (this.$popover.width() / 2);
        var top = this.$openButton.offset().top + this.$openButton.outerHeight() + 16;
        //if (left < 0) left = 0;
        this.$popover.css(
                {"position": "absolute",
                 "left": left + "px",
                 "top": top + "px"}).show();
        
        this.pickerShown = true;
    },
    
    _hidePicker: function() {
        this.$element.removeClass("focus");
        this.$input.parents().off('scroll', this._scrollParents);
        if(this.$popover){
            this.$popover.hide();
        }
        this.pickerShown = false;
    },
    
    _adjustMarkup: function() {
        this.$element.addClass("datepicker");
        
        if (!this.useNativeControls) {
            if (this.$element.find("input").not("[type=hidden]").length === 0) {
                this.$element.append("<input type=\"text\">");
            }
            if (this.$element.find("button").length === 0) {
                this.$element.append("<button class=\"icon-calendar small\"><span>Datepicker</span></button>");
            }
            if ( $('body').find(".popover#"+this.guid).length === 0) {
                $('body').append('<div class="datepicker popover arrow-top" style="display:none" id ="' + this.guid + '"><div class="inner"></div></div>');
                var $popover = $('body').find(".popover#"+this.guid);
                if(this._isDateEnabled()) {
                    $popover.find(".inner").append('<div class="calendar"><div class="calendar-header"></div><div class="calendar-body"></div></div>');
                }
            }
        } else {
            // Show native control
        }

        // Always include hidden field
        if (this.$element.find("input[type=hidden]").length === 0) {
            this.$element.append("<input type=\"hidden\">");
        }
        
        if (!this.$element.find("input[type=hidden]").attr("name")) {
            var name = this.$element.find("input").not("[type=hidden]").attr("name");
            this.$element.find("input[type=hidden]").attr("name",name);
            this.$element.find("input").not("[type=hidden]").removeAttr("name");
        }
        
        // Force button to be a button, not a submit thing
        var $button = this.$element.find('>button');
        if ($button.attr('type') === undefined) {
            $button[0].setAttribute('type', 'button');
        }
                    
    },
    
    _renderCalendar: function(slide) {
        if (!this.displayDateTime || !this.displayDateTime.isValid()) this.displayDateTime = moment();
        var displayDateTime = this.displayDateTime;
    
        var displayYear = displayDateTime.year();
        var displayMonth = displayDateTime.month() + 1;

        var table = this._renderOneCalendar(displayMonth, displayYear);
        
        var $calendar = this.$popover.find(".calendar");

        table.on("mousedown", "a", function(event) {
            event.preventDefault();

            var date = moment($(event.target).data("date"), this.internFormat);

            if(this._isTimeEnabled()) {
                var h = this._getHoursFromDropdown();
                var m = this._getMinutesFromDropdown();
                date.hours(h).minutes(m);
            }

            this._setDateTime(date);

            // Only hide, if the picker has no extra time fields
            if(!this._isTimeEnabled()) this._hidePicker();
        }.bind(this));

        if ($calendar.find("table").length > 0 && slide) {
            this._slideCalendar(table, (slide === "left"));
        } else {
            $calendar.find("table").remove();
            $calendar.find(".sliding-container").remove();
            $calendar.find(".calendar-body").append(table);
        }

        this._updateState();
    },

    _getHoursFromDropdown: function() {
        return parseInt(this.$popover.find('.time .hour select').val(), 10);
    },

    _getMinutesFromDropdown: function() {
        return parseInt(this.$popover.find('.time .minute select').val(), 10);
    },

    _renderOneCalendar: function(month, year) {

        var heading = moment([year, month - 1, 1]).lang(this.language).format(this.options.headFormat);  
        var title = $('<div class="calendar-header"><h2>' + heading + '</h2></div>');

        // Month selection
        var nextMonthElement = $("<button class=\"next-month\">›</button>");
        var prevMonthElement = $("<button class=\"prev-month\">‹</button>");

        title.append(nextMonthElement).append(prevMonthElement);

        var $calendar = this.$popover.find(".calendar");
        if ($calendar.find(".calendar-header").length > 0) {
            $calendar.find(".calendar-header").replaceWith(title);
        } else {
            $calendar.prepend(title);
        }

        var day = null;

        var table = $("<table>");
        table.data("date", year + "/" + month);

        var html = "<tr>";
        for(var i = 0; i < 7; i++) {
            day = (i + this.options.startDay) % 7;
            var dayName = this.options.dayNames[day];
            html += "<th><span>" + dayName + "</span></th>";
        }
        html += "</tr>";
        table.append("<thead>" + html + "</thead>");

        var firstDate = moment([year, month - 1, 1]);
        var monthStartsAt = (firstDate.day() - this.options.startDay) % 7;
        if (monthStartsAt < 0) monthStartsAt += 7;

        html = "";
        var today = moment();
        
        function isSameDay(d1, d2) {
            if (!d1) return;
            if (!d2) return;
            return d1.year() === d2.year() && d1.month() === d2.month() && d1.date() === d2.date(); 
        }
        
        for(var w = 0; w < 6; w++) {
            html +="<tr>";
            for(var d = 0; d < 7; d++) {
                day = (w * 7 + d) - monthStartsAt + 1;
                var displayDateTime = moment([year, month - 1, day]);
                var isCurrentMonth = (displayDateTime.month() + 1) === parseFloat(month);
                var cssClass = "";

                if (isSameDay(displayDateTime, today)) cssClass += " today";
                if (isSameDay(displayDateTime, this.options.selectedDateTime)) cssClass += " selected";

                if (isCurrentMonth && this._isDateInRange(displayDateTime, this.options.minDate, this.options.maxDate)) {
                    html += "<td class=\"" + cssClass + "\"><a href=\"#\" data-date=\"" + displayDateTime.lang(this.language).format(this.internFormat) + "\">" + displayDateTime.date() + "</a></td>";
                } else {
                    html += "<td class=\"" + cssClass + "\"><span>" + displayDateTime.date() + "</span></td>";
                }
            }
            html +="</tr>";
        }
        table.append("<tbody>" + html + "</tbody>");

        return table;
    },
      
    _slideCalendar: function(newtable, isLeft) {

        this.$popover.find(".sliding-container table").stop(true, true);
        this.$popover.find(".sliding-container").remove();

        var oldtable = this.$popover.find("table");
        var width = oldtable.width();
        var height = oldtable.height();
        
        var container = $("<div class=\"sliding-container\">");

        container.css({"display" : "block",
                       "position": "relative",
                       "width": width + "px",
                       "height": height + "px",
                       "overflow": "hidden"});
                   
        this.$popover.find(".calendar-body").append(container);
        container.append(oldtable).append(newtable);
        oldtable.css({"position": "absolute", "left": 0, "top": 0});
        oldtable.after(newtable);
        newtable.css({"position": "absolute", "left": (isLeft) ? width : -width, "top": 0});

        var speed = 400;
        
        oldtable.animate({"left": (isLeft) ? -width : width}, speed, function() {
            oldtable.remove();
        });

        newtable.animate({"left": 0}, speed, function() {
            if (container.parents().length === 0) return; // We already were detached!
            newtable.css({"position": "relative", "left": 0, "top": 0});
            newtable.detach();
            this.$popover.find(".calendar-body").append(newtable);
            container.remove();
        }.bind(this));        
    },
    
    /**
    * Sets a new datetime object for this picker
    */
    _setDateTime: function(date, silent) {
        this.options.selectedDateTime = this.displayDateTime = date;
        
        if (!date) {
            this.$input.val(""); // Clear for null values
        } else if (date.isValid()) {
            this.$input.val(date.lang(this.language).format(this.options.displayedFormat)); // Set only valid dates
        }
        
        var storage = (date && date.isValid()) ? date.lang('en').format(this.options.storedFormat) : ""; // Force to english for storage format!    
        this.$hiddenInput.val(storage);
            
        this._updateState();
        
        if(this._isDateEnabled()) this._renderCalendar();
        
        if(this._isTimeEnabled()) this._renderTime();

        // Trigger a change even on the input
        if (!silent)
            this.$input.trigger('change');
        
        // Always trigger a change event on the hidden input, since we're not listening to it internally
        this.$hiddenInput.trigger('change');
    },

    _getTimeFromInput: function() {
        if(this._isTimeEnabled()) {
            var h = parseInt(this.$popover.find('.time .hour button').text(), 10);
            var m = parseInt(this.$popover.find('.time .minute button').text(), 10);
            var time = [h,m];
            return time;
        }
    },

    _getTimeString: function(hour, minute) {
        return this._pad(hour) + ":" + this._pad(minute) + ":" + this._pad(this.options.selectedDateTime.seconds());
    },

    _combineDateTimeStrings: function(dateString, timeString) {
        return dateString + " " + timeString;
    },
    
    _isDateInRange: function(date, startDate, endDate){
        if(startDate === null && endDate === null){
            return true;
        }
        if(startDate === null){
            return date <= endDate;
        }else if(endDate === null){
            return date >= startDate;
        }else{
            return (startDate <= date && date <= endDate);
        }
    },

    _renderTime: function() {
        var html = $("<div class='time'><i class='icon-clock small'></i></div>");

        // Hours
        var hourSelect = $('<select></select>');
        for(var h = 0; h < 24; h++) {
            var hourOption = $('<option>' + this._pad(h) + '</option>');
            if(this.options.selectedDateTime && h === this.options.selectedDateTime.hours()) { hourOption.attr('selected','selected'); }
            hourSelect.append(hourOption);
        }
        var hourDropdown = $('<div class="dropdown hour"><button></button></div>').append(hourSelect);

        // Minutes
        var minuteSelect = $('<select></select>');
        for(var m = 0; m < 60; m++) {
            var minuteOption = $('<option>' + this._pad(m) + '</option>');
            if(this.options.selectedDateTime && m === this.options.selectedDateTime.minutes()) { minuteOption.attr('selected', 'selected'); }
            minuteSelect.append(minuteOption);
        }
        var minuteDropdown = $('<div class="dropdown minute"><button>Single Select</button></div>').append(minuteSelect);

        // Style for mobile, select can't be hidden
        $(hourDropdown).css({
            'position': 'relative'
        });

        $(minuteDropdown).css({
            'position': 'relative'
        });

        $(hourDropdown).find('select').css({
            'position': 'absolute',
            'left': '1.5rem',
            'top': '1rem'
        });

        $(minuteDropdown).find('select').css({
            'position': 'absolute',
            'left': '1.5rem',
            'top': '1rem'
        });

        html.append(hourDropdown, $("<span>:</span>"), minuteDropdown);

        if (this.$popover.find(".time").length === 0) {
            this.$popover.find(".inner").append(html);
        } else {
            this.$popover.find(".time").empty().append(html.children());
        }
        
        // Set up dropdowns
        $(hourDropdown).dropdown();
        $(minuteDropdown).dropdown();
    },

    _isSupportedMobileDevice: function() {
      if( (navigator.userAgent.match(/Android/i) ||
          navigator.userAgent.match(/iPhone|iPad|iPod/i)) &&
          !this.options.forceHTMLMode) {
          return true;
      }
      return false;
    },

    _supportsInputType: function(type) {
      var i = document.createElement("input");
      i.setAttribute("type", type);
      return i.type !== "text";
    },

    _isDateEnabled: function() {
      return (this.options.type === "date") || (this.options.type === "datetime") || (this.options.type === "datetime-local");
    },

    _isTimeEnabled: function() {
      return (this.options.type === "time") || (this.options.type === "datetime") || (this.options.type === "datetime-local");
    },
    
    _pad: function(s) {
        if (s < 10) return "0" + s;
        return s;
    }
    
  });
  
  CUI.Datepicker.monthNames = ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"];
  CUI.Datepicker.dayNames = ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"];

  CUI.util.plugClass(CUI.Datepicker);

  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
        $("[data-init~=datepicker]", e.target).datepicker();
    });
  }

}(window.jQuery));

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

/*************************************************************************
*
* ADOBE CONFIDENTIAL
* ___________________
*
*  Copyright 2012 Adobe Systems Incorporated
*  All Rights Reserved.
*
* NOTICE:  All information contained herein is, and remains
* the property of Adobe Systems Incorporated and its suppliers,
* if any.  The intellectual and technical concepts contained
* herein are proprietary to Adobe Systems Incorporated and its
* suppliers and are protected by trade secret or copyright law.
* Dissemination of this information or reproduction of this material
* is strictly forbidden unless prior written permission is obtained
* from Adobe Systems Incorporated.
**************************************************************************/

(function($) {

    var DISPLAY_GRID = "grid";

    var DISPLAY_LIST = "list";

    var SELECTION_MODE_COUNT_SINGLE = "single";

    var SELECTION_MODE_COUNT_MULTI = "multiple";

    var DEFAULT_SELECTOR_CONFIG = {

        "itemSelector": "article",                      // selector for getting items
        "headerSelector": "header",                     // selector for headers
        "dataContainer": "grid-0",                      // class name of the data container
        "enableImageMultiply": true,                    // flag if images should be multiplied
        "view": {
            "selectedItem": {                           // defines what classes (cls) on what elements (selector; optional) are used to mark a selection
                "list": {
                    "cls": "selected"
                },
                "grid": {
                    "cls": "selected"
                }
            },
            "selectedItems": {                          // defines the selector that is used for determining the current selection; a resolver function may be specified that adjusts the selection (for exmaple by determining a suitable parent element)
                "list": {
                    "selector": "article.selected"
                },
                "grid": {
                    "selector": "article.selected"
                }
            }
        },
        "controller": {
            "selectElement": {                          // defines the selector that is used for installing the tap/click handlers
                "list": "article > i.select",
                /* "listNavElement": "article", */      // may be used to determine the element that is responsible for navigating in list view (required only if different from the Grid's select item)
                "grid": "article"
            },
            "moveHandleElement": {                      // defines the selector that is used to determine the object that is responsible for moving an item in list view
                "list": "article > i.move"
            },
            "targetToItem": {                           // defines methods that are used to resolve the event target of a tap/click event to a card view item
                "list": function($target) {
                    return $target.closest("article");
                },
                "grid": function($target) {
                    return $target.closest("article");
                },
                "header": function($target) {
                    return $target.closest("header");
                }
            },
            "gridSelect": {                             // defines the class that is used to trigger the grid selection mode
                "cls": "selection-mode"
            },
            "selectAll": {                              // defines the "select all" config (list view only)
                "selector": "header > i.select",
                "cls": "selected"
            },
            "sort": {                                   // defines the config for column sort
                "columnSelector": ".label > *"
            }
        }

    };

    var ensureItem = function(item) {
        if (item.jquery) {
            return item.data("cardView-item");
        }
        return item;
    };

    /**
     * @classdesc
     * Internal class that provides utility functionality to the card view.
     */
    var Utils = {

        /**
         * Check two jQuery objects for equality.
         * @param {jQuery} $1 first jQuery object
         * @param {jQuery} $2 second jQuery object
         * @return {boolean} True if both objects are considered equal
         */
        equals: function($1, $2) {
            return ($1.length === $2.length) && ($1.length === $1.filter($2).length);
        },

        /**
         * Gets a CardView widget for the specified jQuery object.
         * @param {jQuery} $el The jQuery object
         * @return {CUI.CardView} The widget
         */
        getWidget: function($el) {
            var widget;
            if ($el.length > 0) {
                widget = $($el[0]).data("cardView");
            }
            return widget;
        },

        /**
         * Mixes two objects so that for every missing property in object1 the properties of object2 are used. This is also done
         * for nested objects.
         * @param {object} Object 1
         * @param {object} Object 2
         * @return {object} The mixed object with all properties
        */
        mixObjects: function(object1, object2) {
            if (object1 === undefined) return object2;

            var result = {};
            for(var i in object2) {
                if (object1[i] === undefined) {
                    result[i] = object2[i];
                    continue;
                }
                var p = object1[i];

                // Go one step deeper in the object hierarchy if we find an object that is not a string.
                // Note: typeof returns "function" for functions, so no special testing for functions needed.
                if (typeof(object1[i]) == "object" && (!(object1[i] instanceof String))) {
                    p = this.mixObjects(object1[i], object2[i]);
                }
                result[i] = p;
            }
            return result;
        },

        /**
         * Resolves each of the DOM elements in the specified jQuery object with a given
         * function into a new jQuery object.
         * @param {jQuery} $el The jQuery object to resolve
         * @param {Function} fn The resolver function
         * @return {jQuery} The resolved jQuery object
         */
        resolve: function($el, fn) {
            var resolved = [ ];
            $el.each(function() {
                resolved.push.apply(resolved, fn($(this)).toArray());
            });
            return $(resolved);
        },

        /**
         * Multiplies the image with the provided color. This will insert a canvas element
         * before the img element.
         * @param {HTMLElement} $images image element to multiply with the color
         * @param {Number[]} color RGB array of values between 0 and 1
         */
        multiplyImages: function($images, color) {
            // Filter out images where the multiply effect has already been inserted to the DOM, or images that aren't visible
            $images = $images.filter(function () {
                var $image = $(this);
                return !$image.is(".multiplied") && !$image.prev().is(".multiplied") && $image.is(":visible");
            });

            var imageMaxCounter = $images.length;
            var imageIteratorCounter = 0;
            
            function multiplyNextImage() {
                if (imageIteratorCounter < imageMaxCounter) {
                    // Not adding the timeout for the first image will make it feel more reactive.
                    multiplyOneImage($images[imageIteratorCounter]);

                    imageIteratorCounter++;

                    // But adding a timeout for the other images will make it non-blocking.
                    setTimeout(multiplyNextImage, 0);
                }
            }

            function multiplyOneImage(image) {
                var width  = image.naturalWidth,
                    height = image.naturalHeight;

                // defer if image is not yet available
                if ((width === 0) && (height === 0)) {
                    window.setTimeout(function() {
                        multiplyOneImage(image);
                    }, 200);
                    return;
                }

                var canvas = $("<canvas width='" + width + "' height='" + height+"'></canvas>")[0];

                var context = canvas.getContext("2d");
                context.drawImage(image, 0, 0, width, height);

                var imageData = context.getImageData(0, 0, width, height);
                var data = imageData.data;

                for (var i = 0, l = data.length; i < l; i += 4) {
                    data[i]   *= color[0];
                    data[i+1] *= color[1];
                    data[i+2] *= color[2];
                }

                context.putImageData(imageData, 0, 0);

                // re-sizing of canvases are handled different in IE and Opera, thus we have to use an image
                $("<img class='" + image.className + " multiplied' " +
                    "width='" + width + "' height='" + height + "' " +
                    "src='" + canvas.toDataURL("image/png") + "'/>").insertBefore(image);
            }

            multiplyNextImage();
        }
    };

    var ListItemAutoScroller = new Class(/** @lends CUI.CardView.ListItemAutoScroller# */{

        /**
         * The item element
         * @type jQuery
         * @private
         */
        $el: null,

        /**
         * The scrolling container element (with overflow auto/visible and position
         * absolute)
         * @type jQuery
         * @private
         */
        $containerEl: null,

        /**
         * Size of a scrolling step (= number of pixels that get scrolled per autoscrolling
         * 'tick'
         * @type Number
         * @private
         */
        stepSize: 0,

        /**
         * Timeout ID
         * @type Number
         * @private
         */
        iid: undefined,

        /**
         * @private
         */
        autoMoveOffset: 0,

        /**
         * The maximum value that is allowed for scrollTop while autoscrolling
         * @type Number
         * @private
         */
        maxScrollTop: 0,


        /**
         * @ignore
         * @name CUI.CardView.ListItemAutoScroller
         * @classdesc
         * Internal class that implements auto scrolling while moving cards in list view.
         *
         * @desc
         * Creates a new auto scroller.
         *
         * @constructs
         *
         * @param {jQuery} $el The jQuery container element of the card item that is moved
         * @param {Number} stepSize The step size (number of pixels scrolled per 'tick')
         * @param {Function} autoMoveFn Function that gets executed on every auto scroll
         *        "event". The function must actually reposition the item element to
         *        the coordinate specified as parameters (first parameter: x; second
         *        parameter: y
         */
        construct: function($el, stepSize, autoMoveFn) {
            this.$el = $el;
            this.stepSize = stepSize;
            this.$containerEl = this._getScrollingContainer($el);
            var cont = this.$containerEl[0];
            this.maxScrollTop = Math.max(cont.scrollHeight - cont.clientHeight, 0);
            this.autoMoveFn = autoMoveFn;
        },

        /**
         * Gets a suitable container element that limits the scrolling area.
         * @param {jQuery} $el The card item's container element
         * @return {jQuery} The container element
         * @private
         */
        _getScrollingContainer: function($el) {
            while (($el.length > 0) && !$el.is("body")) {
                var ovflY =  $el.css("overflowY");
                var pos = $el.css("position");
                if (((ovflY === "auto") || (ovflY === "visible")) && (pos === "absolute")) {
                    return $el;
                }
                $el = $el.parent();
            }
            return $(window);
        },

        /**
         * Checks if scrolling is necessary according to the item element's current position
         * and the scroll container's state and executes a single scrolling step by
         * adjusting the scroll container's scrollTop property if necessary.
         * @return {Boolean} True if scrolling occured; false if no scrolling was necessary
         * @private
         */
        _execute: function() {
            var cont = this.$containerEl[0];
            var clientHeight = cont.clientHeight;
            var scrollTop = cont.scrollTop;
            var itemTop = this.$el.offset().top - this.$containerEl.offset().top;
            var itemBottom = itemTop + this.$el.height();
            var isAutoScroll = false;
            if (itemTop <= 0) {
                // auto scroll upwards
                if (scrollTop > 0) {
                    scrollTop -= this.stepSize;
                    this.autoMoveOffset = -this.stepSize;
                    if (scrollTop < 0) {
                        scrollTop = 0;
                    }
                    cont.scrollTop = scrollTop;
                    isAutoScroll = true;
                }
            } else if (itemBottom >= clientHeight) {
                // auto scroll downwards
                if (scrollTop < this.maxScrollTop) {
                    scrollTop += this.stepSize;
                    this.autoMoveOffset = this.stepSize;
                    if (scrollTop > this.maxScrollTop) {
                        scrollTop = this.maxScrollTop;
                    }
                    cont.scrollTop = scrollTop;
                    isAutoScroll = true;
                }
            }
            return isAutoScroll;
        },

        /**
         * Moves the card item's element by calculating its new position and calling
         * the function that was provided in the constructor as autoMoveFn.
         * @private
         */
        _autoMove: function() {
            if (this.autoMoveOffset && this.autoMoveFn) {
                var itemOffs = this.$el.offset();
                var itemTop = itemOffs.top + this.autoMoveOffset;
                this.autoMoveFn(itemOffs.left, itemTop);
            }
        },

        /**
         * Checks if autoscrolling is currently necessary; if so, the autoscrolling is
         * executed and a timer is started that will check again later if additional
         * auto scrolling is necessary (and execute again if so).
         */
        check: function() {
            var self = this;
            this.stop();
            var isAutoScroll = this._execute();
            if (isAutoScroll) {
                this.iid = window.setTimeout(function() {
                    self.iid = undefined;
                    self._autoMove();
                }, 50);
            }
        },

        /**
         * Stops autoscrolling.
         */
        stop: function() {
            if (this.iid !== undefined) {
                window.clearTimeout(this.iid);
                this.autoMoveOffset = 0;
                this.iid = undefined;
            }
        }

    });

    var ListItemMoveHandler = new Class(/** @lends CUI.CardView.ListItemMoveHandler# */{

        /**
         * The list's jQuery element
         * @type {jQuery}
         * @private
         */
        $listEl: null,

        /**
         * The moved item's jQuery element
         * @type {jQuery}
         * @private
         */
        $itemEl: null,

        /**
         * A jQuery element that contains all items of the list
         * @type {jQuery}
         * @private
         */
        $items: null,

        /**
         * The document
         * @type {jQuery}
         * @private
         */
        $doc: null,

        /**
         * The jQuery object that represents the card that is right before the moved card
         * just before the move; undefined, if the moved card is the first card of the list
         * @type {jQuery}
         * @private
         */
        $oldBefore: null,

        /**
         * The CSS class that is added to the item while it is moved
         * @type String
         * @private
         */
        dragCls: null,

        /**
         * Flag that determines if the horizontal position of an item should be fixed
         * while it is moved
         * @type {Boolean}
         * @private
         */
        fixHorizontalPosition: false,

        /**
         * The autoscroller module
         * @type {ListItemAutoScroller}
         * @private
         */
        autoScroller: null,

        /**
         * @ignore
         * @name CUI.CardView.ListItemMoveHandler
         *
         * @classdesc
         * Internal class that implements the reordering of cards in list view.
         *
         * @constructor
         * @desc
         * Creates a new handler for moving a item around in a list by drag &amp; drop (or
         * its touch counterpart).
         */
        construct: function(config) {
            var self = this;
            this.$listEl = config.$listEl;
            this.$itemEl = config.$itemEl;
            this.$items = config.$items;
            this.dragCls = config.dragCls;
            this.fixHorizontalPosition = (config.fixHorizontalPosition !== false);
            this.autoScroller = (config.autoScrolling ?
                    new ListItemAutoScroller(this.$itemEl, 8, function(x, y) {
                        self._autoMove(x, y);
                    }) : undefined);
        },

        /**
         * Gets the page coordinates of the specified event, regardless if it is a mouse
         * or a touch event.
         * @param {Object} e The event
         * @return {{x: (Number), y: (Number)}} The page coordinates
         * @private
         */
        _getEventCoords: function(e) {
            if (!e.originalEvent.touches) {
                return {
                    x: e.pageX,
                    y: e.pageY
                };
            }
            return (e.originalEvent.touches.length > 0 ? {
                x: e.originalEvent.touches[0].pageX,
                y: e.originalEvent.touches[0].pageY
            } : e.originalEvent.changedTouches.length > 0 ? {
                x: e.originalEvent.changedTouches[0].pageX,
                y: e.originalEvent.changedTouches[0].pageY
            } : {
                x: e.pageX,
                y: e.pageY
            });
        },

        /**
         * Limits the specified coordinates to the list's real estate.
         * @param {Number} top vertical coordinate to limit
         * @param {Number} left horizontal coordinate to limit
         * @return {{top: *, left: *}}
         * @private
         */
        _limit: function(top, left) {
            if (left < this.listOffset.left) {
                left = this.listOffset.left;
            }
            if (top < this.listOffset.top) {
                top = this.listOffset.top;
            }
            var right = left + this.size.width;
            var bottom = top + this.size.height;
            var limitRight = this.listOffset.left + this.listSize.width;
            var limitBottom = this.listOffset - top + this.listSize.height;
            if (right > limitRight) {
                left = limitRight - this.size.width;
            }
            if (bottom > limitBottom) {
                top = limitBottom - this.size.height;
            }
            if (this.fixHorizontalPosition) {
                left = this.listOffset.left;
            }
            return {
                "top": top,
                "left": left
            };
        },

        /**
         * Gets the coordinates of the specified event in a device (mouse, touch) agnostic
         * way.
         * @param {Object} e The event
         * @return {{x: Number, y: Number}} The coordinates
         * @private
         */
        _getEventPos: function(e) {
            var evtPos = this._getEventCoords(e);
            return {
                x: evtPos.x - this.delta.left,
                y: evtPos.y - this.delta.top
            };
        },

        /**
         * Adjust the position of the moved item by limiting it to the containing list
         * and executing autoscrolling.
         * @param {Number} x The original x coordinate
         * @param {Number} y The original y coordinate
         * @private
         */
        _adjustPosition: function(x, y) {
            this.$itemEl.offset(this._limit(y, x));
            if (this.autoScroller) {
                this.autoScroller.check();
            }
        },

        /**
         * Changes the order of the items in the list if necessary.
         * @private
         */
        _changeOrderIfRequired: function() {
            var itemPos = this.$itemEl.offset();
            var hotX = itemPos.left + (this.size.width / 2);
            var hotY = itemPos.top + (this.size.height / 2);
            var $newTarget = null;
            // check if we are overlapping another item at least 50% -> then we will take
            // its position
            var isInsertBefore = false;
            for (var i = 0; i < this.$items.length; i++) {
                var $item = $(this.$items[i]);
                if (!Utils.equals($item, this.$itemEl)) {
                    var offs = $item.offset();
                    var width = $item.width();
                    var height = $item.height();
                    var bottom = offs.top + height;
                    if ((hotX >= offs.left) && (hotX < offs.left + width) &&
                            (hotY >= offs.top) && (hotY < bottom)) {
                        isInsertBefore = ((hotY - offs.top) > (bottom - hotY));
                        $newTarget = $item;
                        break;
                    }
                }
            }
            if ($newTarget) {
                var _offs = this.$itemEl.offset();
                if (isInsertBefore) {
                    $newTarget.before(this.$itemEl);
                } else {
                    $newTarget.after(this.$itemEl);
                }
                this.$itemEl.offset(_offs);
            }
        },

        /**
         * Callback for auto move (called by auto scroller implementation)
         * @param x {Number} The horizontal position
         * @param y {Number} The vertical position
         * @private
         */
        _autoMove: function(x, y) {
            this._adjustPosition(x, y);
            this._changeOrderIfRequired();
        },

        /**
         * Starts moving the list item.
         * @param {Object} e The event that starts the move
         */
        start: function(e) {
            this.$oldPrev = this.$itemEl.prev();
            this.$oldNext = this.$itemEl.next();

            var evtPos = this._getEventCoords(e);
            if (this.dragCls) {
                this.$itemEl.addClass(this.dragCls);
            }
            var self = this;
            this.$doc = $(document);
            this.$doc.fipo("touchmove.listview.drag", "mousemove.listview.drag",
                function(e) {
                    self.move(e);
                });
            this.$doc.fipo("touchend.listview.drag", "mouseup.listview.drag",
                function(e) {
                    self.end(e);
                });
            this.offset = this.$itemEl.offset();
            this.delta = {
                "left": evtPos.x - this.offset.left,
                "top": evtPos.y - this.offset.top
            };
            this.size = {
                "width": this.$itemEl.width(),
                "height": this.$itemEl.height()
            };
            this.listOffset = this.$listEl.offset();
            this.listSize = {
                "width": this.$listEl.width(),
                "height": this.$listEl.height()
            };
            e.stopPropagation();
            e.preventDefault();
            /*
            console.log("offset", this.offset, "delta", this.delta, "size", this.size,
                "listoffs", this.listOffset, "listsize", this.listSize);
            */
        },

        /**
         * Moves the card item.
         * @param {Object} e The event that is responsible for the move
         */
        move: function(e) {
            // console.log("move", e);
            var pos = this._getEventPos(e);
            this._adjustPosition(pos.x, pos.y);
            this._changeOrderIfRequired();
            e.stopPropagation();
            e.preventDefault();
        },

        /**
         * Finishes moving the card item.
         * @param {Object} e The event that is responsible for finishing the move
         */
        end: function(e) {
            var pos = this._getEventPos(e);
            this._adjustPosition(pos.x, pos.y);
            // console.log("end", e);
            if (this.dragCls) {
                this.$itemEl.removeClass(this.dragCls);
            }
            if (this.autoScroller) {
                this.autoScroller.stop();
            }
            this.$itemEl.css("position", "");
            this.$itemEl.css("top", "");
            this.$itemEl.css("left", "");
            this.$doc.off("touchmove.listview.drag");
            this.$doc.off("mousemove.listview.drag");
            this.$doc.off("touchend.listview.drag");
            this.$doc.off("mouseup.listview.drag");
            var $newPrev = this.$itemEl.prev();
            var $newNext = this.$itemEl.next();

            this.$itemEl.trigger($.Event("item-moved", {
                newPrev: $newPrev,
                newNext: $newNext,
                oldPrev: this.$oldPrev,
                oldNext: this.$oldNext,
                hasMoved: !Utils.equals($newPrev, this.$oldPrev)
            }));
            e.stopPropagation();
            e.preventDefault();
        }

    });

    /*
     * This class represents a single item in the list model.
     */
    var Item = new Class(/** @lends CUI.CardView.Item# */{

        /**
         * The jQuery object that represents the card
         * @type {jQuery}
         * @private
         */
        $itemEl: null,

        /**
         * @ignore
         * @name CUI.CardView.Item
         *
         * @classdesc
         * Internal class that represents a single card/list item in a card view's data
         * model.
         *
         * @constructor
         * @desc
         * Create a new card/list item.
         */
        construct: function($itemEl) {
            this.$itemEl = $itemEl;
            this.reference();
        },

        /**
         * Get the card/list item's jQuery object.
         * @return {jQuery} The jQuery object
         */
        getItemEl: function() {
            return this.$itemEl;
        },

        /**
         * References the item's data model in the jQzery object.
         */
        reference: function() {
            this.$itemEl.data("cardView-item", this);
        }

    });

    var Header = new Class(/** @lends CUI.CardView.Header# */{

        /**
         * The jQuery object that represents the header
         * @type {jQuery}
         * @private
         */
        $headerEl: null,

        /**
         * The first item that follows the header
         * @type {CUI.CardView.Item}
         */
        itemRef: null,

        /**
         * @ignore
         * @name CUI.CardView.Header
         *
         * @classdesc
         * This class represents a list header (that is shown in list view only) in the
         * card view's data model.
         *
         * @constructor
         * @desc
         * Create a new list header.
         */
        construct: function($headerEl, itemRef) {
            this.$headerEl = $headerEl;
            this.itemRef = itemRef;
        },

        /**
         * Get the jQuery object that is assosciated with the list header.
         * @return {jQuery} The associated jQuery object
         */
        getHeaderEl: function() {
            return this.$headerEl;
        },

        /**
         * Get the list item that follows the header directly.
         * @return {CUI.CardView.Item} The item
         */
        getItemRef: function() {
            return this.itemRef;
        },

        /**
         * Set the list item that follows the header directly.
         * @param {CUI.CardView.Item} itemRef The item
         */
        setItemRef: function(itemRef) {
            this.itemRef = itemRef;
        }

    });

    /**
        Handles resort according to columns
    */
    var ColumnSortHandler = new Class(/** @lends CUI.CardView.ColumnSortHandler# */{
        construct: function(options) {
            this.model = options.model;
            this.comparators = options.comparators;
            this.selectors = options.selectors;
            this.columnElement = options.columnElement;

            this.headerElement = options.columnElement.closest(this.selectors.headerSelector);                    
            var header = this.model.getHeaderForEl(this.headerElement);
            this.items = this.model.getItemsForHeader(header);

            this.isReverse = this.columnElement.hasClass("sort-asc"); // switch to reverse?
            this.toNatural = this.columnElement.hasClass("sort-desc"); // back to natural order?
            this.fromNatural = !this.headerElement.hasClass("sort-mode");
            
            this.comparator = null;

            // Choose the right comparator
            if (this.comparators) {
                for(var selector in this.comparators) {
                    if (!this.columnElement.is(selector)) continue;
                    this.comparator = this.comparators[selector];
                }
            }

            if (!this.comparator) this.comparator = this._readComparatorFromMarkup();
        },
        _readComparatorFromMarkup: function() {
            var selector = this.columnElement.data("sort-selector");
            var attribute = this.columnElement.data("sort-attribute");
            var sortType = this.columnElement.data("sort-type");
            if (!selector && !attribute) return null;
            return new CUI.CardView.DefaultComparator(selector, attribute, sortType);

        },
        _adjustMarkup: function() {
            // Adjust general mode class
            if (this.fromNatural) this.headerElement.addClass("sort-mode");
            if (this.toNatural) this.headerElement.removeClass("sort-mode");
            
            // Adjust column classes
            this.headerElement.find(this.selectors.controller.sort.columnSelector).removeClass("sort-asc sort-desc");
            this.columnElement.removeClass("sort-desc sort-asc");
            if (!this.toNatural) this.columnElement.addClass(this.isReverse ? "sort-desc" : "sort-asc");

            // Show or hide d&d elements 
            var showMoveHandle = this.toNatural;
            $.each(this.items, function() {this.getItemEl().find(".move").toggle(showMoveHandle);});
        },
        sort: function() {
            if (!this.comparator && !this.toNatural) return;

            this._adjustMarkup();

            // Re-Sort items
            var items = this.items.slice(); // Make a copy before sorting
            // By default items are in their "natural" order, most probably defined by the user with d&d

            // Only sort if we have a comparator
            if (this.comparator) {
                this.comparator.setReverse(this.isReverse);
                var fn = this.comparator.getCompareFn();
                if (!this.toNatural) items.sort(fn);   // Only sort if we do not want to go back to natural order       
            }

            // Adjust DOM
            var prevItem = this.headerElement; // Use header as starting point;
            $.each(this.items, function() {this.getItemEl().detach();}); // First: Detach all items
           
            // Now: reinsert in new order
            for(var i = 0; i < items.length; i++) {
                var item = items[i].getItemEl();
                prevItem.after(item);
                prevItem = item;
            }
        }
    });



    var DirectMarkupModel = new Class(/** @lends CUI.CardView.DirectMarkupModel# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * List of items; original/current sorting order (without UI sorting applied)
         * @type {CUI.CardView.Item[]}
         * @private
         */
        items: null,

        /**
         * List of headers
         * @type {CUI.CardView.Header[]}
         * @private
         */
        headers: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupModel
         *
         * @classdesc
         * This class represents a data model that is created via a selector from an
         * existing DOM.
         *
         * @constructor
         * @desc
         * Create a new data model.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         */
        construct: function($el, selectors) {
            this.$el = $el;
            this.items = [ ];
            this.selectors = selectors;
            var $items = this.$el.find(selectors.itemSelector);
            var itemCnt = $items.length;
            for (var i = 0; i < itemCnt; i++) {
                this.items.push(new Item($($items[i])));
            }
            this.headers = [ ];
            var $headers = this.$el.find(selectors.headerSelector);
            var headerCnt = $headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var $header = $($headers[h]);
                var $itemRef = $header.nextAll(selectors.itemSelector);
                var itemRef = ($itemRef.length > 0 ?
                        this.getItemForEl($($itemRef[0])) : undefined);
                this.headers.push(new Header($header, itemRef));
            }
        },

        /**
         * Initialize the data model.
         */
        initialize: function() {
            var self = this;
            this.$el.on("item-moved", this.selectors.itemSelector, function(e) {
                if (e.hasMoved) {
                    self._reorder(e);
                }
            });
        },

        /**
         * Reorder the cards according to the specified event.
         * @param {Event} e The reordering event
         * @private
         */
        _reorder: function(e) {
            var itemToMove = this.getItemForEl($(e.target));
            var newBefore = this.getItemForEl(e.newPrev);
            var isHeaderInsert = false;
            var header;
            if (!newBefore) {
                header = this.getHeaderForEl(e.newPrev);
                if (header) {
                    isHeaderInsert = true;
                    var refPos = this.getItemIndex(header.getItemRef());
                    if (refPos > 0) {
                        newBefore = this.getItemAt(refPos - 1);
                    }
                }
            }
            var oldPos = this.getItemIndex(itemToMove);
            this.items.splice(oldPos, 1);
            // if the item to move is directly following a header, the header's item ref
            // has to be updated
            var headerRef = this._getHeaderByItemRef(itemToMove);
            if (headerRef) {
                headerRef.setItemRef(this.getItemAt(oldPos));
            }
            var insertPos = (newBefore ? this.getItemIndex(newBefore) + 1 : 0);
            this.items.splice(insertPos, 0, itemToMove);
            if (isHeaderInsert) {
                header.setItemRef(itemToMove);
            }
            // console.log(itemToMove, newBefore, isHeaderInsert);
        },

        /**
         * Get the number of cards/list items.
         * @return {Number} The number of cards/list items
         */
        getItemCount: function() {
            return this.items.length;
        },

        /**
         * Get the card/list item that is at the specified list position.
         * @param {Number} pos The position
         * @return {CUI.CardView.Item} The item at the specified position
         */
        getItemAt: function(pos) {
            return this.items[pos];
        },

        /**
         * Get the list position of the specified card/list item.
         * @param {CUI.CardView.Item} item The item
         * @return {Number} The list position; -1 if the specified item is not a part of
         *         the list
         */
        getItemIndex: function(item) {
            for (var i = 0; i < this.items.length; i++) {
                if (item === this.items[i]) {
                    return i;
                }
            }
            return -1;
        },

        /**
         * Get the card/list item that is associated with the specified jQuery object.
         * @param {jQuery} $el The jQuery object
         * @return {CUI.CardView.Item} The item; undefined if no item is associated with
         *         the specified jQuery object
         */
        getItemForEl: function($el) {
            var itemCnt = this.items.length;
            for (var i = 0; i < itemCnt; i++) {
                var item = this.items[i];
                if (Utils.equals(item.getItemEl(), $el)) {
                    return item;
                }
            }
            return undefined;
        },

        /**
         * <p>Inserts the specified card(s)/list item(s) at the given position.</p>
         * <p>Please note that you can specify multiple items either as an array of jQuery
         * objects or a single jQuery object that contains multiple DOM objects, each
         * representing an item.</p>
         * @param {jQuery|jQuery[]} $items The item(s) to insert
         * @param {Number} pos The position to insert
         * @param {Boolean} beforeHeader True if the items should added before headers (only
         *        applicable if the items are inserted directly at a position where also
         *        a header is present); needs to be false if the list has a single header
         *        that is placed at the top of the list
         */
        insertItemAt: function($items, pos, beforeHeader) {
            if (!$.isArray($items)) {
                $items = $items.toArray();
            }
            for (var i = $items.length - 1; i >= 0; i--) {

                var $item = $items[i];
                if (!$item.jquery) {
                    $item = $($item);
                }

                // adjust model
                var followupItem;
                var item = new Item($item);
                if ((pos === undefined) || (pos === null)) {
                    this.items.push(item);
                    pos = this.items.length - 1;
                } else {
                    followupItem = this.items[pos];
                    this.items.splice(pos, 0, item);
                }
                var insert = {
                    "item": followupItem,
                    "mode": "item"
                };

                // adjust header references if item is inserted directly behind a header
                var headerCnt = this.headers.length;
                for (var h = 0; h < headerCnt; h++) {
                    var header = this.headers[h];
                    if (header.getItemRef() === followupItem) {
                        if (beforeHeader) {
                            insert = {
                                "item": header,
                                "mode": "header"
                            };
                            break;
                        } else {
                            header.setItemRef(item);
                        }
                    }
                }

                // trigger event
                this.$el.trigger($.Event("change:insertitem", {
                    "insertPoint": insert,
                    "followupItem": followupItem,
                    "item": item,
                    "pos": pos,
                    "widget": Utils.getWidget(this.$el),
                    "moreItems": (i > 0)
                }));
            }
        },

        /**
         * Get the number of list headers.
         * @return {Number} The number of headers
         */
        getHeaderCount: function() {
            return this.headers.length;
        },

        /**
         * Get a list header by its position in the list of headers.
         * @param {Number} pos The list header
         * @return {CUI.CardView.Header} The list header at the specified position
         */
        getHeaderAt: function(pos) {
            return this.headers[pos];
        },

        /**
         * Get all list headers.
         * @return {CUI.CardView.Header[]} List headers
         */
        getHeaders: function() {
            var headers = [ ];
            headers.push.apply(headers, this.headers);
            return headers;
        },

        /**
         * Get the list header that is associated with the specified jQuery object.
         * @param {jQuery} $el The jQuery object
         * @return {CUI.CardView.Header} The list header; undefined if no header is
         *         associated with the jQuery object
         */
        getHeaderForEl: function($el) {
            var headerCnt = this.headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var header = this.headers[h];
                if (Utils.equals(header.getHeaderEl(), $el)) {
                    return header;
                }
            }
            return undefined;
        },

        /**
         * Get the header that directly precedes the specified list item.
         * @param {CUI.CardView.Item} itemRef The item
         * @return {CUI.CardView.Header} header The header
         * @private
         */
        _getHeaderByItemRef: function(itemRef) {
            for (var h = 0; h < this.headers.length; h++) {
                if (this.headers[h].getItemRef() === itemRef) {
                    return this.headers[h];
                }
            }
            return undefined;
        },

        /**
         * Get all list items that are preceded by the specified header.
         * @param header {CUI.CardView.Header} The header
         * @return {CUI.CardView.Item[]} The list items
         */
        getItemsForHeader: function(header) {
            // TODO does not handle empty headers yet
            var itemRef = header.getItemRef();
            var headerCnt = this.headers.length;
            var itemCnt = this.items.length;
            var itemsForHeader = [ ];
            var isInRange = false;
            for (var i = 0; i < itemCnt; i++) {
                var item = this.items[i];
                if (isInRange) {
                    for (var h = 0; h < headerCnt; h++) {
                        if (this.headers[h].getItemRef() === item) {
                            isInRange = false;
                            break;
                        }
                    }
                    if (isInRange) {
                        itemsForHeader.push(item);
                    } else {
                        break;
                    }
                } else {
                    if (item === itemRef) {
                        isInRange = true;
                        itemsForHeader.push(itemRef);
                    }
                }
            }
            return itemsForHeader;
        },

        /**
         * Get the list items (data model) from their associated DOM objects.
         * @param {jQuery} $elements The jQuery object that specifies the items' DOM
         *        objects
         * @return {CUI.CardView.Item[]} List items
         */
        fromItemElements: function($elements) {
            var items = [ ];
            $elements.each(function() {
                var item = $(this).data("cardView-item");
                if (item) {
                    items.push(item);
                }
            });
            return items;
        },

        /**
         * Write back references to the data model to the respective DOM objects.
         */
        reference: function() {
            var itemCnt = this.items.length;
            for (var i = 0; i < itemCnt; i++) {
                this.items[i].reference();
            }
        },

        /**
         * Removes all items without triggering respective events.
         */
        removeAllItemsSilently: function() {
            this.items.length = 0;
            for (var h = 0; h < this.headers.length; h++) {
                this.headers[h].setItemRef(undefined);
            }
        }

    });

    var DirectMarkupView = new Class(/** @lends CUI.CardView.DirectMarkupView# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupView
         *
         * @classdesc
         * This class represents a view for data represented by a DirectMarkupModel.
         *
         * @constructor
         * @desc
         * Create a new view.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         */
        construct: function($el, selectors) {
            this.$el = $el;
            this.selectors = selectors;
        },

        /**
         * Initializes the view.
         */
        initialize: function() {
            var self = this;
            this.$el.on("change:displayMode", function(e) {
                var oldMode = e.oldValue;
                var newMode = e.value;
                self.cleanupAfterDisplayMode(oldMode);
                self.prepareDisplayMode(newMode);
            });
            this.$el.on("change:insertitem", function(e) {
                self._onItemInserted(e);
            });
            this.$el.reflow({
                "small": function ($el, size) {
                    return $el.width() > 40*size.rem() && $el.width() < 50*size.rem();
                },
                "xsmall": function ($el, size) {
                    return $el.width() > 30*size.rem() && $el.width() < 40*size.rem();
                },
                "xxsmall": function ($el, size) {
                    return $el.width() < 30*size.rem();
                }
            });
        },

        /**
         * Handler that adjusts the view after a new card/list item has been inserted.
         * @param {Event} e The event
         * @private
         */
        _onItemInserted: function(e) {
            var $dataRoot = this.$el;
            if (this.selectors.dataContainer) {
                $dataRoot = $dataRoot.find("." + this.selectors.dataContainer);
            }
            var $item = e.item.getItemEl();
            var followupItem = e.followupItem;
            switch (this.getDisplayMode()) {
                case DISPLAY_LIST:
                    if (!followupItem) {
                        $dataRoot.append($item);
                    } else {
                        var insert = e.insertPoint;
                        var item = insert.item;
                        var $ref = (insert.mode === "item" ?
                            item.getItemEl() : item.getHeaderEl());
                        $ref.before($item);
                    }
                    break;
                case DISPLAY_GRID:
                    if (!e.moreItems) {
                        var widget = Utils.getWidget(this.$el);
                        widget._restore();
                        widget.layout();
                    }
                    break;
            }
        },

        /**
         * Get the current display mode (grid view/list view)
         * @return {String} The display mode; defined by constants prefixed by DISPLAY_
         */
        getDisplayMode: function() {
            return Utils.getWidget(this.$el).getDisplayMode();
        },

        /**
         * Set the selection state of the specified item.
         * @param {CUI.CardView.Item} item The item
         * @param {String} selectionState The selection state; currently supported:
         *        "selected", "unselected"
         */
        setSelectionState: function(item, selectionState) {
            var displayMode = this.getDisplayMode();
            var selectorDef = this.selectors.view.selectedItem[displayMode];
            var $itemEl = item.getItemEl();
            if (selectorDef.selector) {
                $itemEl = $itemEl.find(selectorDef.selector);
            }
            if (selectionState === "selected") {
                $itemEl.addClass(selectorDef.cls);
                if (displayMode === DISPLAY_GRID) {
                    this._drawSelectedGrid(item);
                }
            } else if (selectionState === "unselected") {
                $itemEl.removeClass(selectorDef.cls);
            }
        },

        /**
         * Get the selection state of the specified item.
         * @param {CUI.CardView.Item} item The item
         * @return {String} The selection state; currently supported: "selected",
         *         "unselected"
         */
        getSelectionState: function(item) {
            var selectorDef = this.selectors.view.selectedItem[this.getDisplayMode()];
            var $itemEl = item.getItemEl();
            if (selectorDef.selector) {
                $itemEl = $itemEl.find(selectorDef.selector);
            }
            var cls = selectorDef.cls.split(" ");
            for (var c = 0; c < cls.length; c++) {
                if (!$itemEl.hasClass(cls[c])) {
                    return "unselected";
                }
            }
            return "selected";
        },

        /**
         * Get a list of currently selected items.
         * @return {jQuery} The list of selected items
         */
        getSelectedItems: function() {
            var selectorDef = this.selectors.view.selectedItems[this.getDisplayMode()];
            var $selectedItems = this.$el.find(selectorDef.selector);
            if (selectorDef.resolver) {
                $selectedItems = selectorDef.resolver($selectedItems);
            }
            return $selectedItems;
        },

        /**
         * <p>Restors the card view.</p>
         * <p>The container is purged and the cards are re-inserted in original order
         * (note that this is necessary, because the item elements get reordered for
         * card view; original order has to be restored for list view),</p>
         * @param {CUI.CardView.DirectMarkupModel} model The data model
         * @param {Boolean} restoreHeaders True if header objects should be restored as
         *        well (for list view)
         */
        restore: function(model, restoreHeaders) {
            var $container = $("<div class='" + this.selectors.dataContainer + "'>");
            this.$el.empty();
            this.$el.append($container);
            var itemCnt = model.getItemCount();
            for (var i = 0; i < itemCnt; i++) {
                $container.append(model.getItemAt(i).getItemEl());
            }
            if (restoreHeaders) {
                var headerCnt = model.getHeaderCount();
                for (var h = 0; h < headerCnt; h++) {
                    var header = model.getHeaderAt(h);
                    var $headerEl = header.getHeaderEl();
                    var itemRef = header.getItemRef();
                    if (itemRef) {
                        itemRef.getItemEl().before($headerEl);
                    } else {
                        $container.append($headerEl);
                    }
                }
            }
        },

        /**
         * Prepares the specified display mode (grid vs. list view).
         * @param {String} displayMode The display mode ({@link CUI.CardView.DISPLAY_GRID},
         *        {@link CUI.CardView.DISPLAY_LIST})
         */
        prepareDisplayMode: function(displayMode) {
            if (displayMode === DISPLAY_GRID) {
                this._drawAllSelectedGrid();
            }
        },

        /**
         * Clean up before the specified display mode is left.
         * @param {String} displayMode The display mode ({@link CUI.CardView.DISPLAY_GRID},
         *        {@link CUI.CardView.DISPLAY_LIST})
         */
        cleanupAfterDisplayMode: function(displayMode) {
            // not yet required; may be overridden
        },

        /**
         * Draw the multiplied version (used for displaying a selection) of the specified
         * image.
         * @param {jQuery} $image The image
         * @private
         */
        _drawImage: function($image) {
            if ($image.length === 0) {
                return;
            }

            if (this._colorFloat === undefined) {
                var color256     = $image.closest("a").css("background-color");     // Let's grab the color form the card background
                this._colorFloat = $.map(color256.match(/(\d+)/g), function (val) { // RGB values between 0 and 1
                    return val/255;
                });
            }

            Utils.multiplyImages($image, this._colorFloat);
        },

        /**
         * Create the multiplied images for selected state (in grid view) for all cards.
         * @private
         */
        _drawAllSelectedGrid: function() {
            if (!this.selectors.enableImageMultiply) {
                return;
            }
            var self = this;
            var selector = this.selectors.view.selectedItems.grid.selector + " img";
            var $selector = $(selector);

            this._drawImage($selector);
            $selector.load(function() {
                self._drawImage($(this));
            });
        },

        /**
         * Create the multiplied image for the selected state of the specified card (in
         * grid view).
         * @param {CUI.CardView.Item} item The card/list item
         * @private
         */
        _drawSelectedGrid: function(item) {
            if (!this.selectors.enableImageMultiply) {
                return;
            }
            var self = this;
            var $img = item.getItemEl().find("img");

            this._drawImage($img);
            $img.load(function() {
                self._drawImage($(this));
            });
        },

        /**
         * Removes all items from the view without triggering respective events.
         */
        removeAllItemsSilently: function() {
            this.$el.find(this.selectors.itemSelector).remove();
        }

    });

    var DirectMarkupController = new Class(/** @lends CUI.CardView.DirectMarkupController# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,
        
        /**
         * comparator config for list sorting
         * @type {Object}
         * @private
         */
        comparators: null,

        /**
         * The selection mode
         * @type {String}
         * @private
         */
        selectionModeCount: null,

        /**
         * Flag that is used for a workaround for touch devices
         * @type {Boolean}
         * @private
         */
        _listSelect: false,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupController
         *
         * @classdesc
         * This class implements the controller for data represented by DirectMarkupModel
         * and displayed by DirectMarkupView.
         *
         * @constructor
         * @desc
         * Create a new controller.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         * @param {Object} comparators The comparator config for column sorting
         */
        construct: function($el, selectors, comparators) {
            this.$el = $el;
            this.selectors = selectors;
            this.comparators = comparators;
            this.selectionModeCount = SELECTION_MODE_COUNT_MULTI;
        },

        /**
         * Initializes the controller
         */
        initialize: function() {
            this.setDisplayMode(this.$el.hasClass("list") ? DISPLAY_LIST : DISPLAY_GRID);
            var self = this;

            // Selection
            this.$el.fipo("tap.cardview.select", "click.cardview.select",
                this.selectors.controller.selectElement.list, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    if (widget.getDisplayMode() === DISPLAY_LIST) {
                        var item = ensureItem(self.getItemElFromEvent(e));
                        if (widget.toggleSelection(item)) {
                            e.stopPropagation();
                            e.preventDefault();
                        }
                        if (e.type === "tap") {
                            self._listSelect = true;
                        }
                    }
                });
            this.$el.fipo("tap.cardview.select", "click.cardview.select",
                this.selectors.controller.selectElement.grid, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    if ((widget.getDisplayMode() === DISPLAY_GRID) &&
                            widget.isGridSelectionMode()) {
                        var item = ensureItem(self.getItemElFromEvent(e));
                        if (widget.toggleSelection(item)) {
                            e.stopPropagation();
                            e.preventDefault();
                        }
                    }
                });
            // list header
            this.$el.fipo("tap.cardview.selectall", "click.cardview.selectall",
                this.selectors.controller.selectAll.selector, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    if (widget.getDisplayMode() === DISPLAY_LIST) {
                        var cls = self.selectors.controller.selectAll.cls;
                        var $header = self.selectors.controller.targetToItem.header(
                                $(e.target));
                        var header = widget.getModel().getHeaderForEl($header);
                        if ($header.hasClass(cls)) {
                            widget.deselectAll(header);
                        } else {
                            widget.selectAll(header);
                        }
                    }
                });
                
            // list sorting
            this.$el.fipo("tap.cardview.sort", "click.cardview.sort",
                this.selectors.headerSelector + " " + this.selectors.controller.sort.columnSelector, function(e) {
                    
                    var widget = Utils.getWidget(self.$el);
                    var model = widget.getModel();

                    // Trigger a sortstart event
                    var event = $.Event("sortstart");
                    $(e.target).trigger(event);
                    if (event.isDefaultPrevented()) return;

                    var sorter = new ColumnSortHandler({
                        model: model,
                        columnElement: $(e.target),
                        comparators: self.comparators,
                        selectors: self.selectors
                    });
                    sorter.sort();

                    // Trigger an sortend event
                    event = $.Event("sortend");
                    $(e.target).trigger(event);
                });

            // Prevent text selection of headers!
            this.$el.on("selectstart.cardview", this.selectors.headerSelector + " " + this.selectors.controller.sort.columnSelector, function(e) {
                e.preventDefault();
            });

            // block click event for cards on touch devices
            this.$el.finger("click.cardview.select",
                this.selectors.controller.selectElement.grid, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    var dispMode = widget.getDisplayMode();
                    if ((dispMode === DISPLAY_GRID) && widget.isGridSelectionMode()) {
                        e.stopPropagation();
                        e.preventDefault();
                    }
                });
            // block click event for list items on touch devices if the click actually
            // represents a change in selection rather than navigating
            var listNavElement = this.selectors.controller.selectElement.listNavElement ||
                    this.selectors.controller.selectElement.grid;
            this.$el.finger("click.cardview.select",
                listNavElement, function(e) {
                    var widget = Utils.getWidget(self.$el);
                    var dispMode = widget.getDisplayMode();
                    if ((dispMode === DISPLAY_LIST) && self._listSelect) {
                        e.stopPropagation();
                        e.preventDefault();
                    }
                    self._listSelect = false;
                });
            // reordering
            this.$el.fipo("touchstart.cardview.reorder", "mousedown.cardview.reorder",
                this.selectors.controller.moveHandleElement.list, function(e) {
                    var $itemEl = self.getItemElFromEvent(e);
                    var handler = new ListItemMoveHandler({
                        $listEl: self.$el,
                        $itemEl: $itemEl,
                        $items: $(self.selectors.itemSelector),
                        dragCls: "dragging",
                        autoScrolling: true
                    });
                    handler.start(e);
                });
            // handle select all state
            this.$el.on("change:selection", function(e) {
                if (e.moreSelectionChanges) {
                    return;
                }
                self._adjustSelectAllState(e.widget);
            });
            this.$el.on("change:insertitem", function(e) {
                if (e.moreItems) {
                    return;
                }
                self._adjustSelectAllState(e.widget);
            });
        },

        /**
         * Adjusts the state of the "select all" element of all list headers.
         * @param {CUI.CardView} widget The card view widget
         * @private
         */
        _adjustSelectAllState: function(widget) {
            var cls = this.selectors.controller.selectAll.cls;
            var selectionState = widget.getHeaderSelectionState();
            var headers = selectionState.headers;
            var headerCnt = headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var header = headers[h];
                var $header = header.header.getHeaderEl();
                if (header.hasUnselected) {
                    $header.removeClass(cls);
                } else {
                    $header.addClass(cls);
                }
            }
        },

        /**
         * Resolves the target of the specified event to a jQuery element that represents
         * a card.
         * @param {Event} e The event
         * @return {jQuery} The jQuery object that represents a card
         */
        getItemElFromEvent: function(e) {
            var $target = $(e.target);
            var resolver = this.selectors.controller.targetToItem[this.getDisplayMode()];
            if ($.isFunction(resolver)) {
                return resolver($target);
            }
            return $target.find(resolver);
        },

        /**
         * Checks if selection mode is enabled for grid view.
         * @return {Boolean} True if selection mode is enabled
         */
        isGridSelect: function() {
            var selectorDef = this.selectors.controller.gridSelect;
            var $el = this.$el;
            if (selectorDef.selector) {
                $el = $el.find(selectorDef.selector);
            }
            return $el.hasClass(selectorDef.cls);
        },

        /**
         * Set selection mode for grid view.
         * @param {Boolean} isGridSelect True to turn selection mode on
         */
        setGridSelect: function(isGridSelect) {
            if (this.isGridSelect() !== isGridSelect) {
                var selectorDef = this.selectors.controller.gridSelect;
                var $el = this.$el;
                if (selectorDef.selector) {
                    $el = $el.find(selectorDef.selector);
                }
                if (isGridSelect) {
                    $el.addClass(selectorDef.cls);
                } else {
                    $el.removeClass(selectorDef.cls);
                    Utils.getWidget($el).clearSelection();
                }
                this.$el.trigger($.Event("change:gridSelect", {
                    "widget": this.$el.data("cardView"),
                    "oldValue": !isGridSelect,
                    "value": isGridSelect
                }));
            }
        },

        /**
         * Get current display mode (grid/list view).
         * @return {String} Display mode ({@link CUI.CardView.DISPLAY_GRID},
         *         {@link CUI.CardView.DISPLAY_LIST})
         */
        getDisplayMode: function() {
            if (this.$el.hasClass("list")) {
                return DISPLAY_LIST;
            }
            if (this.$el.hasClass("grid")) {
                return DISPLAY_GRID;
            }
            return null;
        },

        /**
        * @return {boolean} true if this widget is currently in list mode and has a column sorting on any header applied
        */
        isColumnSorted: function() {
            return (this.getDisplayMode() == "list") && this.$el.find(this.selectors.headerSelector).filter(".sort-mode").length > 0;
        },

        /**
         * Set display mode.
         * @param {String} displayMode Display mode ({@link CUI.CardView.DISPLAY_GRID},
         *        {@link CUI.CardView.DISPLAY_LIST})
         */
        setDisplayMode: function(displayMode) {
            var oldValue = this.getDisplayMode();
            if (oldValue !== displayMode) {
                var widget = Utils.getWidget(this.$el);
                widget._restore(displayMode === DISPLAY_LIST);
                switch (displayMode) {
                    case DISPLAY_GRID:
                        this.$el.removeClass("list");
                        this.$el.addClass("grid");
                        if (oldValue !== null) {
                            var selection = widget.getSelection();
                            this.setGridSelect(selection.length > 0);
                            widget.layout();
                        }
                        break;
                    case DISPLAY_LIST:
                        this.$el.cuigridlayout("destroy");
                        this.$el.removeClass("grid");
                        this.$el.addClass("list");
                        break;
                }
                this.$el.trigger($.Event("change:displayMode", {
                    "widget": this.$el.data("cardView"),
                    "oldValue": oldValue,
                    "value": displayMode
                }));
            }
        },

        /**
         * Get selection mode (single/multiple).
         * @return {String} The selection mode;
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI}
         */
        getSelectionModeCount: function() {
            return this.selectionModeCount;
        },

        /**
         * Set selection mode (single/multiple).
         * @param {String} modeCount The selection mode;
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI}
         */
        setSelectionModeCount: function(modeCount) {
            this.selectionModeCount = modeCount;
        }

    });

    var DirectMarkupAdapter = new Class(/** @lends CUI.CardView.DirectMarkupAdapter# */{

        /**
         * The jQuery object that is the parent of the card view
         * @type {jQuery}
         * @private
         */
        $el: null,

        /**
         * CSS selector config
         * @type {Object}
         * @private
         */
        selectors: null,

        /**
         * comparator config
         * @type {Object}
         * @private
         */
        comparators: null,

        /**
         * The model
         * @type {CUI.CardView.DirectMarkupModel}
         * @private
         */
        model: null,

        /**
         * The view
         * @type {CUI.CardView.DirectMarkupView}
         * @private
         */
        view: null,

        /**
         * The controller
         * @type {CUI.CardView.DirectMarkupController}
         * @private
         */
        controller: null,

        /**
         * @ignore
         * @name CUI.CardView.DirectMarkupAdapter
         *
         * @classdesc
         * Internal class that wires model, controller and view.
         *
         * @constructor
         * @desc
         * Create a new adapter.
         * @param {jQuery} $el The jQuery object that is the parent of the card view
         * @param {Object} selectors The CSS selector config
         */
        construct: function(selectors, comparators) {
            this.selectors = selectors;
            this.comparators = comparators;
        },

        /**
         * Initialize the adapter (and the wrapped model, view & controller).
         * @param {jQuery} $el The card view's parent element
         */
        initialize: function($el) {
            this.$el = $el;
            this.setModel(new DirectMarkupModel($el, this.selectors));
            this.setView(new DirectMarkupView($el, this.selectors));
            this.setController(new DirectMarkupController($el, this.selectors, this.comparators));
            this.model.initialize();
            this.view.initialize();
            this.controller.initialize();
        },

        /**
         * Set the model.
         * @param {CUI.CardView.DirectMarkupModel} model The model
         */
        setModel: function(model) {
            this.model = model;
        },

        /**
         * Get the model.
         * @return {CUI.CardView.DirectMarkupModel} The model
         */
        getModel: function() {
            return this.model;
        },

        /**
         * Set the view.
         * @param {CUI.CardView.DirectMarkupView} view The view
         */
        setView: function(view) {
            this.view = view;
        },

        /**
         * Get the view.
         * @return {CUI.CardView.DirectMarkupView} The view
         */
        getView: function() {
            return this.view;
        },

        /**
         * Set the controller.
         * @param {CUI.CardView.DirectMarkupController} controller The controller
         */
        setController: function(controller) {
            this.controller = controller;
        },

        /**
         * Get the controller.
         * @return {CUI.CardView.DirectMarkupController} The controller
         */
        getController: function() {
            return this.controller;
        },

        /**
         * Check if the specified card/list item is selected.
         * @param {CUI.CardView.Item} item The card/item
         * @return {Boolean} True if it is selected
         */
        isSelected: function(item) {
            var selectionState = this.view.getSelectionState(item);
            return (selectionState === "selected");
        },

        /**
         * Set the selection state of zhe specified card/list item.
         * @param {CUI.CardView.Item} item The card/item
         * @param {Boolean} isSelected True if it is selected
         */
        setSelected: function(item, isSelected) {
            var selectionState = (isSelected ? "selected" : "unselected");
            this.view.setSelectionState(item, selectionState);
        },

        /**
         * Get a list of selected items
         * @param {Boolean} useModel True if {@link CUI.CardView.Item}s should be returned;
         *        false for jQuery objects
         * @return {CUI.CardView.Item[]|jQuery}
         */
        getSelection: function(useModel) {
            var selection = this.view.getSelectedItems();
            if (useModel === true) {
                selection = this.model.fromItemElements(selection);
            }
            return selection;
        },

        /**
         * Get the display mode.
         * @return {String} The display mode ({@link CUI.CardView.DISPLAY_GRID} or
         *         {@link CUI.CardView.DISPLAY_LIST})
         */
        getDisplayMode: function() {
            return this.controller.getDisplayMode();
        },

        /**
        * @return {boolean} true if this widget is currently in list mode and has a column sorting on any header applied
        */
        isColumnSorted: function() {
            return this.controller.isColumnSorted();
        },        

        /**
         * Set the display mode.
         * @param {String} selectionMode The display mode ({@link CUI.CardView.DISPLAY_GRID}
         *        or {@link CUI.CardView.DISPLAY_LIST})
         */
        setDisplayMode: function(selectionMode) {
            this.controller.setDisplayMode(selectionMode);
        },

        /**
         * Check if selection mode is active in grid view.
         * @return {Boolean} True if selection mode is active
         */
        isGridSelectionMode: function() {
            return this.controller.isGridSelect();
        },

        /**
         * Set if selection mode is active in grid view.
         * @param {Boolean} isSelectionMdoe True if selection mode is active
         */
        setGridSelectionMode: function(isSelectionMode) {
            this.controller.setGridSelect(isSelectionMode);
        },

        /**
         * Get the general selection mode (single/multiple items)
         * @return {String} The selection mode
         *         ({@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *         {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI})
         */
        getSelectionModeCount: function() {
            return this.controller.getSelectionModeCount();
        },

        /**
         * Set the general selection mode (single/multiple items)
         * @param {String} modeCount The selection mode
         *        ({@link CUI.CardView.SELECTION_MODE_COUNT_SINGLE},
         *        {@link CUI.CardView.SELECTION_MODE_COUNT_MULTI})
         */
        setSelectionModeCount: function(modeCount) {
            this.controller.setSelectionModeCount(modeCount);
        },

        /**
         * Restores the opriginal DOM structure of the widget.
         * @param {Boolean} restoreHeaders True if list headers should also be restored
         *        (list view)
         * @protected
         */
        _restore: function(restoreHeaders) {
            this.view.restore(this.model, restoreHeaders);
            this.model.reference();
        },

        /**
         * Removes all items from the card view.
         */
        removeAllItems: function() {
            var widget = Utils.getWidget(this.$el);
            widget.clearSelection();
            this.model.removeAllItemsSilently();
            this.view.removeAllItemsSilently();
        }

    });

    CUI.CardView = new Class(/** @lends CUI.CardView# */{

        toString: 'CardView',

        extend: CUI.Widget,

        adapter: null,


        /**
         * @extends CUI.Widget
         * @classdesc
         * <p>A display of cards that can either be viewed as a grid or a list.</p>
         * <p>The display mode - grid or list view - can be changed programmatically
         * whenever required.</p>
         * <p>Grid view has two modes: navigation and selection, which can also be switched
         * programmatically. In navigation mode, the user can use cards to navigate
         * hierarchical structures ("to another stack of cards"). In selection mode, the
         * cards get selected on user interaction instead. List view combines both selection
         * and navigation modes.</p>
         * <p>The card view uses a data model internally that abstracts the cards. This
         * data model is currently not opened as API. Therefore you will often encounter
         * unspecified objects that represent cards in the data model. You can use them
         * interchangibly (for example, if one method returns a card data object, you can
         * pass it to another method that takes a card data object as a parameter), but
         * you shouldn't assume anything about their internals. You may use
         * {@link CUI.CardView#prepend}, {@link CUI.CardView#append} and
         * {@link CUI.CardView#removeAllItems} to manipulate the data model.</p>
         * <p>Please note that the current implementation has some limitiations which are
         * documented if known. Subsequent releases of CoralUI will remove those limitations
         * bit by bit.</p>
         * <p>The following example shows two cards in grid view:</p>
         *
<div class="grid" data-toggle="cardview">
    <div class="grid-0">
        <article class="card-default">
            <i class="select"></i>
            <i class="move"></i>
            <a href="#">
                <span class="image">
                    <img class="show-grid" src="images/preview.png" alt="">
                    <img class="show-list" src="images/preview-small.png" alt="">
                </span>
                <div class="label">
                    <h4>A card</h4>
                    <p>Description</p>
                </div>
            </a>
        </article>
        <article class="card-default">
            <i class="select"></i>
            <i class="move"></i>
            <a href="#">
                <span class="image">
                    <img class="show-grid" src="images/preview.png" alt="">
                    <img class="show-list" src="images/preview-small.png" alt="">
                </span>
                <div class="label">
                    <h4>Another card</h4>
                    <p>See shell example page for more info.</p>
                </div>
            </a>
        </article>
    </div>
</div>
         *
         * @example
<caption>Instantiate with Class</caption>
// Currently unsupported.
         *
         * @example
<caption>Instantiate with jQuery</caption>
// Currently unsupported.
         *
         * @example
<caption>Markup</caption>
&lt;div class="grid" data-toggle="cardview"&gt;
    &lt;div class="grid-0"&gt;
        &lt;article class="card-default"&gt;
            &lt;i class="select"&gt;&lt;/i&gt;
            &lt;i class="move"&gt;&lt;/i&gt;
            &lt;a href="#"&gt;
                &lt;span class="image"&gt;
                    &lt;img class="show-grid" src="images/preview.png" alt=""&gt;
                    &lt;img class="show-list" src="images/preview-small.png" alt=""&gt;
                &lt;/span&gt;
                &lt;div class="label"&gt;
                    &lt;h4&gt;A card&lt;/h4&gt;
                    &lt;p&gt;Description&lt;/p&gt;
                &lt;/div&gt;
            &lt;/a&gt;
        &lt;/article&gt;
    &lt;/div&gt;
&lt;/div&gt;
         * @example
<caption>Defining comparators for column sorting</caption>
//  Define a selector for the column and then a comparator to be used for sorting
// The comparator
var comparatorConfig = {".label .main": new CUI.CardView.DefaultComparator(".label h4", null, false),
                   ".label .published": new CUI.CardView.DefaultComparator(".label .published", "data-timestamp", true)};
new CUI.CardView({comparators: comparatorConfig})

         * @example
<caption>Defining comparators via data API</caption>
&lt;!-- Page header for list view --&gt;
&lt;header class="card-page selectable movable"&gt;
    &lt;i class="select"&gt;&lt;/i&gt;
    &lt;i class="sort"&gt;&lt;/i&gt;
    &lt;div class="label"&gt;
        &lt;div class="main" data-sort-selector=".label h4"&gt;Title&lt;/div&gt;
        &lt;div class="published" data-sort-selector=".label .published .date" data-sort-attribute="data-timestamp" data-sort-type="numeric"&gt;Published&lt;/div&gt;
        &lt;div class="modified" data-sort-selector=".label .modified .date" data-sort-attribute="data-timestamp" data-sort-type="numeric"&gt;Modified&lt;/div&gt;
        &lt;div class="links" data-sort-selector=".label .links-number" data-sort-type="numeric"&gt;&lt;i class="icon-arrowright"&gt;Links&lt;/i&gt;&lt;/div&gt;
    &lt;/div&gt;
&lt;/header&gt;
&lt;!--
    Sorting is started when the user clicks on the corresponding column header.

    data-sort-selector   defines which part of the item to select for sorting
    data-sort-attribute  defines which attribute of the selected item element should be user for sorting. If not given, the inner text is used.
    data-sort-type       if set to "numeric", a numerical comparison is used for sorting, an alphabetical otherwise
--&gt;

         * @example
<caption>Switching to grid selection mode using API</caption>
$cardView.cardView("toggleGridSelectionMode");
         *
         * @example
<caption>Switching to grid selection mode using CSS contract</caption>
$cardView.toggleClass("selection-mode");
$cardView.find("article").removeClass("selected");
         *
         * @desc Creates a new card view.
         * @constructs
         *
         * @param {Object} [options] Component options
         * @param {Object} [options.selectorConfig]
         *        The selector configuration. You can also omit configuration values: Values not given will be used from
         *        the default selector configuration.
         * @param {String} options.selectorConfig.itemSelector
         *        The selector that is used to retrieve the cards from the DOM
         * @param {String} options.selectorConfig.headerSelector
         *        The selector that is used to retrieve the header(s) in list view from the
         *        DOM
         * @param {String} options.selectorConfig.dataContainer
         *        The class of the div that is used internally for laying out the cards
         * @param {Boolean} options.selectorConfig.enableImageMultiply
         *        Flag that determines if the images of cards should use the "multiply
         *        effect" for display in selected state
         * @param {Object} options.selectorConfig.view
         *        Configures the view of the CardView
         * @param {Object} options.selectorConfig.view.selectedItem
         *        Defines what classes on what elements are used to select a card
         * @param {Object} options.selectorConfig.view.selectedItem.list
         *        Defines the selection-related config in list view
         * @param {String} options.selectorConfig.view.selectedItem.list.cls
         *        Defines the CSS class that is used to select a card in list view
         * @param {String} [options.selectorConfig.view.selectedItem.list.selector]
         *        An additioonal selector if the selection class has to be set on a child
         *        element rather than the card's parent element
         * @param {Object} options.selectorConfig.view.selectedItem.grid
         *        Defines the selection-related config in grid view
         * @param {String} options.selectorConfig.view.selectedItem.grid.cls
         *        Defines the CSS class that is used to select a card in grid view
         * @param {String} [options.selectorConfig.view.selectedItem.grid.selector]
         *        An additioonal selector if the selection class has to be set on a child
         *        element rather than the card's parent element
         * @param {Object} options.selectorConfig.view.selectedItems
         *        Defines how to determine the currently selected cards
         * @param {Object} options.selectorConfig.view.selectedItems.list
         *        Defines how to determine the currently selected cards in list view
         * @param {String} options.selectorConfig.view.selectedItems.list.selector
         *        The selector that determines the DOM elements that represent all currently
         *        selected cards
         * @param {Function} [options.selectorConfig.view.selectedItems.list.resolver]
         *        A function that is used to calculate a card's parent element from the
         *        elements that are returned from the selector that is used for determining
         *        selected cards
         * @param {Object} options.selectorConfig.view.selectedItems.grid
         *        Defines how to determine the currently selected cards in grid view
         * @param {String} options.selectorConfig.view.selectedItems.grid.selector
         *        The selector that determines the DOM elements that represent all currently
         *        selected cards
         * @param {Function} [options.selectorConfig.view.selectedItems.grid.resolver]
         *        A function that is used to calculate a card's parent element from the
         *        elements that are returned from the selector that is used for determining
         *        selected cards
         * @param {Object} options.selectorConfig.controller
         *        Configures the controller of the CardView
         * @param {Object} options.selectorConfig.controller.selectElement
         *        The selector that defines the DOM element that is used for selecting
         *        a card (= targets for the respective click/tap handlers)
         * @param {String} options.selectorConfig.controller.selectElement.list
         *        The selector that defines the event targets for selecting a card in list
         *        view
         * @param {String} [options.selectorConfig.controller.selectElement.listNavElement]
         *        An additional selector that may be used to determine the element that is
         *        used for navigating in list view if it is different from the event target
         *        defined by options.selectorConfig.controller.selectElement.grid
         * @param {String} options.selectorConfig.controller.selectElement.grid
         *        The selector that defines the event targets for selecting a card in grid
         *        view
         * @param {Object} options.selectorConfig.controller.moveHandleElement
         *        The selector that defines the DOM elements that are used for moving
         *        cards in list view (= targets for the respective mouse/touch handlers)
         * @param {String} options.selectorConfig.controller.moveHandleElement.list
         *        The selector that defines the event targets for the handles that are used
         *        to move a card in list view
         * @param {Object} options.selectorConfig.controller.targetToItems
         *        Defines the mapping from event targets to cards
         * @param {Function|String} options.selectorConfig.controller.targetToItems.list
         *        A function that takes a jQuery object that represents the event target for
         *        selecting a card in list view and that has to return the jQuery object
         *        that represents the entire card; can optionally be a selector as well
         * @param {Function|String} options.selectorConfig.controller.targetToItems.grid
         *        A function that takes a jQuery object that represents the event target for
         *        selecting a card in grid view and that has to return the jQuery object t
         *        hat represents the entire card; can optionally be a selector as well
         * @param {Function|String} options.selectorConfig.controller.targetToItems.header
         *        A function that takes a jQuery object that represents the event target for
         *        the "select all" button of a header in list view and that has to return
         *        the jQuery object that represents the respective header; can optionally
         *        be a selector as well
         * @param {Object} options.selectorConfig.controller.gridSelect
         *        Defines the selection mode in grid view
         * @param {Object} options.selectorConfig.controller.gridSelect.cls
         *        Defines the class that is used to switch to selection mode in grid view
         * @param {Object} options.selectorConfig.controller.gridSelect.selector
         *        An additional selector that is used to define the child element where the
         *        selection mode class should be applied to/read from
         * @param {Object} options.selectorConfig.controller.selectAll
         *        Defines how to select all cards in list view
         * @param {Object} options.selectorConfig.controller.selectAll.selector
         *        The selector that is used to determine all "select all" buttons in a
         *        CardView
         * @param {Object} options.selectorConfig.controller.sort
         *        Defines selectors for the column sorting mechanism.
         * @param {Object} options.selectorConfig.controller.sort.columnSelector
         *        The selector for all column objects within the header 
         * @param {Object} options.gridSettings
         *        Custom options for jQuery grid layout plugin.
         * @param {Object} options.selectorConfig.controller.selectAll.cls
         *        The class that has to be applied to each card if "select all" is invoked
         * @param {Object} [options.comparators]
         *        An associative array of comparators for column sorting: Every object attribute is a CSS selector
         *        defining one column and its value has to be of type CUI.CardView.DefaultComparator (or your own derived class)      
        */
        construct: function(options) {
            // Mix given selector config with defaults: Use given config and add defaults, where no option is given
            var selectorConfig = Utils.mixObjects(options.selectorConfig, DEFAULT_SELECTOR_CONFIG);
            var comparators = options.comparators || null;

            this.adapter = new DirectMarkupAdapter(selectorConfig, comparators);
            this.adapter.initialize(this.$element);
            this.layout(options.gridSettings);
        },

        /**
         * Get the underlying data model.
         * @return {*} The underlying data model
         * @private
         */
        getModel: function() {
            return this.adapter.getModel();
        },

        /**
         * Set the underlying data model.
         * @param {*} model The underlying data model
         * @private
         */
        setModel: function(model) {
            this.adapter.setModel(model);
        },

        /**
         * Check if the specified item (part of the data model) is currently selected.
         * @param {*} item The item (data mode) to check
         * @return {Boolean} True if the specified item is selected
         * @private
         */
        isSelected: function(item) {
            return this.adapter.isSelected(item);
        },

        /**
         * Get the current display mode (grid or list view).
         * @return {String} The display mode; either {@link CUI.CardView.DISPLAY_GRID} or
         *         {@link CUI.CardView.DISPLAY_LIST}
         */
        getDisplayMode: function() {
            return this.adapter.getDisplayMode();
        },

       /**
        * @return {boolean} true if this widget is currently in list mode and has a column sorting on any header applied
        */
        isColumnSorted: function() {
            return this.adapter.isColumnSorted();
        },

        /**
        * @param {boolean} sortable     Set to true if this list should be sortable by click on column
        */
        setColumnSortable: function(sortable) {
            // TODO implement
        },

        /**
        * @return {boolean} True if this list is column sortable (does not say if it is currently sorted, use isColumnSorted() for this)
        */
        isColumnSortable: function() {
            // TODO implement
        },

        /**
         * Set the display mode (grid or list view).
         * @param {String} displayMode The display mode; either
         *        {@link CUI.CardView.DISPLAY_GRID} or {@link CUI.CardView.DISPLAY_LIST}
         */
        setDisplayMode: function(displayMode) {
            this.adapter.setDisplayMode(displayMode);
        },

        /**
         * Checks if selection mode is currently active in grid view.
         * @return {Boolean} True if selection mode is active
         */
        isGridSelectionMode: function() {
            return this.adapter.isGridSelectionMode();
        },

        /**
         * Set the selection mode in grid view.
         * @param {Boolean} isSelection True to switch grid selection mode on
         */
        setGridSelectionMode: function(isSelection) {
            this.adapter.setGridSelectionMode(isSelection);
        },

        /**
         * Toggle selection mode in grid view.
         */
        toggleGridSelectionMode: function() {
            this.setGridSelectionMode(!this.isGridSelectionMode());
        },

        getSelectionModeCount: function() {
            return this.adapter.getSelectionModeCount();
        },

        setSelectionModeCount: function(modeCount) {
            this.adapter.setSelectionModeCount(modeCount);
        },

        /**
         * <p>Select the specified item.</p>
         * <p>The second parameter should be used if multiple cards are selected/deselected
         * at once. It prevents some time consuming stuff from being executed more than
         * once.</p>
         * @param {jQuery|*} item The item to select; may either be from data model or a
         *        jQuery object
         * @param {Boolean} moreSelectionChanges True if there are more selection changes
         *        following directly
         */
        select: function(item, moreSelectionChanges) {
            // TODO implement beforeselect event
            item = ensureItem(item);
            var isSelected = this.adapter.isSelected(item);
            if (!isSelected) {
                if (this.getSelectionModeCount() === SELECTION_MODE_COUNT_SINGLE &&
                    this.getSelection().length > 0) {
                    this.clearSelection();
                }

                this.adapter.setSelected(item, true);
                this.$element.trigger($.Event("change:selection", {
                    "widget": this,
                    "item": item,
                    "isSelected": true,
                    "moreSelectionChanges": (moreSelectionChanges === true)
                }));
            }
        },

        /**
         * <p>Deselect the specified card.</p>
         * <p>The second parameter should be used if multiple cards are selected/deselected
         * at once. It prevents some time consuming stuff from being executed more than
         * once.</p>
         * @param {jQuery|*} item The item to deselect; may either be from data model or a
         *        jQuery object
         * @param {Boolean} moreSelectionChanges True if there are more selection changes
         *        following directly
         */
        deselect: function(item, moreSelectionChanges) {
            // TODO implement beforeselect event
            item = ensureItem(item);
            var isSelected = this.adapter.isSelected(item);
            if (isSelected) {
                this.adapter.setSelected(item, false);
                this.$element.trigger($.Event("change:selection", {
                    "widget": this,
                    "item": item,
                    "isSelected": false,
                    "moreSelectionChanges": moreSelectionChanges
                }));
            }
        },

        /**
         * <p>Toggle the selection state of the specified item.</p>
         * <p>The second parameter should be used if multiple cards are selected/deselected
         * at once. It prevents some time consuming stuff from being executed more than
         * once.</p>
         * @param {jQuery|*} item The item; may be either from data model or a jQuery object
         * @param {Boolean} moreSelectionChanges True if there are more selection changes
         *        following directly
         * @return {Boolean} True if the toggle requires the originating event (if any)
         *         to be stopped and to prevent browser's default behavior
         */
        toggleSelection: function(item, moreSelectionChanges) {
            item = ensureItem(item);

            // allow to cancel & stop the event
            var beforeEvent = $.Event("beforeselect", {

                selectionCancelled: false,

                stopEvent: false,

                item: item,

                cancelSelection: function(stopEvent) {
                    this.selectionCancelled = true;
                    this.stopEvent = (stopEvent === true);
                }
            });
            this.$element.trigger(beforeEvent);
            if (beforeEvent.selectionCancelled) {
                return beforeEvent.stopEvent;
            }

            var isSelected = this.isSelected(item);
            if (!isSelected &&
                    (this.getSelectionModeCount() === SELECTION_MODE_COUNT_SINGLE) &&
                    (this.getSelection().length > 0)) {
                this.clearSelection();
            }

            this.adapter.setSelected(item, !isSelected);
            this.$element.trigger($.Event("change:selection", {
                "widget": this,
                "item": item,
                "isSelected": !isSelected,
                "moreSelectionChanges": moreSelectionChanges
            }));
            return true;
        },

        /**
         * Gets the currently selected cards.
         * @param {Boolean} useModel True if items from the data model should be retured;
         *        false, if a jQuery object should be returned instead
         * @return {Array|jQuery} The selected items
         */
        getSelection: function(useModel) {
            return this.adapter.getSelection(useModel === true);
        },

        /**
         * Clears the current selection state by deselecting all selected cards.
         */
        clearSelection: function() {
            var selection = this.getSelection(true);
            var itemCnt = selection.length;
            var finalItem = (itemCnt - 1);
            for (var i = 0; i < itemCnt; i++) {
                this.deselect(selection[i], (i < finalItem));
            }
        },

        /**
         * @private
         */
        _headerSel: function(headers, selectFn, lastValidItemFn) {
            var model = this.adapter.getModel();
            if (headers == null) {
                headers = model.getHeaders();
            }
            if (!$.isArray(headers)) {
                headers = [ headers ];
            }
            var headerCnt = headers.length;
            for (var h = 0; h < headerCnt; h++) {
                var header = headers[h];
                if (header.jquery) {
                    header = model.getHeaderForEl(header);
                }
                var itemsToSelect = model.getItemsForHeader(header);
                var itemCnt = itemsToSelect.length;
                for (var i = 0; i < itemCnt; i++) {
                    selectFn.call(this,
                            itemsToSelect[i], !lastValidItemFn(i, itemsToSelect));
                }
            }
        },

        /**
         * <p>Selects all cards.</p>
         * <p>If the headers parameter is specified, all items that are part of one
         * of the specified headers get selected. Items that are not assigned to one of the
         * specified headers are not changed.</p>
         * @param {Array} [headers] Header filter
         */
        selectAll: function(headers) {
            if (this.getSelectionModeCount() !== SELECTION_MODE_COUNT_MULTI) return;

            var self = this;
            this._headerSel(headers, this.select, function(i, items) {
                for (++i; i < items.length; i++) {
                    if (!self.isSelected(items[i])) {
                        return false;
                    }
                }
                return true;
            });
        },

        /**
         * <p>Deselect all cards.</p>
         * <p>If the headers parameter is specified, all items that are part of one
         * of the specified headers get deselected. Items that are not assigned to one of
         * the specified headers are not changed.</p>
         * @param {Array} [headers] Header filter
         */
        deselectAll: function(headers) {
            var self = this;
            this._headerSel(headers, this.deselect, function(i, items) {
                for (++i; i < items.length; i++) {
                    if (self.isSelected(items[i])) {
                        return false;
                    }
                }
                return true;
            });
        },

        /**
         * @private
         */
        getHeaderSelectionState: function() {
            var model = this.getModel();
            var curHeader = null;
            var state = {
                "selected": [ ],
                "hasUnselected": false,
                "headers": [ ]
            };
            var headerCnt = model.getHeaderCount();
            var itemCnt = model.getItemCount();
            for (var i = 0; i < itemCnt; i++) {
                var item = model.getItemAt(i);
                for (var h = 0; h < headerCnt; h++) {
                    var header = model.getHeaderAt(h);
                    if (header.getItemRef() === item) {
                        curHeader = {
                            "header": header,
                            "selected": [ ],
                            "hasUnselected": false
                        };
                        state.headers.push(curHeader);
                        break;
                    }
                }
                if (this.isSelected(item)) {
                    if (curHeader !== null) {
                        curHeader.selected.push(item);
                    } else {
                        state.selected.push(item);
                    }
                } else {
                    if (curHeader !== null) {
                        curHeader.hasUnselected = true;
                    } else {
                        state.hasUnselected = true;
                    }
                }
            }
            return state;
        },

        /**
         * Create and execute a layout of the cards if in grid view.
         */
        layout: function(settings) {
            if (this.getDisplayMode() !== DISPLAY_GRID) {
                return;
            }
            if (this.$element.data('cuigridlayout')) {
                this.$element.cuigridlayout("destroy");
            }
            this.$element.cuigridlayout(settings);
        },

        /**
         * Exexute a relayout of the cards if in grid view.
         */
        relayout: function() {
            if (this.getDisplayMode() !== DISPLAY_GRID) {
                return;
            }
            this.$element.cuigridlayout("layout");
        },

        /**
         * @protected
         */
        _restore: function(restoreHeaders) {
            this.adapter._restore(restoreHeaders);
        },

        /**
         * <p>Append the specified jQuery items as cards.</p>
         * <p>Note that if you are intending to add multiple cards at once, you should
         * either create a single jQuery object that matches the cards to append or an array
         * of jQuery objects, where each array element represents a single card.</p>
         * @param {jQuery|jQuery[]} $items The jQuery item(s) to append
         */
        append: function($items) {
            this.adapter.getModel().insertItemAt($items, null, false);
        },

        /**
         * Prepend the specified jQuery items as cards.
         * @param {jQuery} $items The jQuery item(s) to prepend
         */
        prepend: function($items) {
            this.adapter.getModel().insertItemAt($items, 0, false);
        },

        /**
         * Remove all cards from the view.
         */
        removeAllItems: function() {
            this.adapter.removeAllItems();
            if (this.getDisplayMode() === DISPLAY_GRID) {
                this.relayout();
            }
            this.$element.trigger($.Event("change:removeAll", {
                widget: this
            }));
        }

    });

    /** Comparator class for column sorting */
    CUI.CardView.DefaultComparator = new Class(/** @lends CUI.CardView.DefaultComparator# */{
        /**
        * This comparator can select any text or attribute of a given jQuery element and compares
        * it with a second item either numerical or alpahebtical
        *
        * @param {String}  selector   A CSS selector that matches the part of the item to be compared
        * @param {String}  attribute  The attribute of the item to be compared. If not given, the inner text of the item will be used for comparison.
        * @param {String}  type  "numeric" for numeric comparison, or "string" for alphabetical comparison
        */
        construct: function (selector, attribute, type) {
            this.selector = selector;
            this.attribute = attribute;
            this.isNumeric = (type == "numeric");
            this.reverseMultiplier = 1;
        },
        /**
        * Changes the order of the sort algorithm
        * @param {boolean} True for reverse sorting, false for normal
        */
        setReverse: function(isReverse) {
            this.reverseMultiplier = (isReverse) ? -1 : 1;
        },
        /**
        * Compares two items according to the configuration
        * @return {integer} -1, 0, 1
        */
        compare: function(item1, item2) {
            var $item1 = item1.getItemEl();
            var $item2 = item2.getItemEl();
            var $e1 = (this.selector) ? $item1.find(this.selector) : $item1;
            var $e2 = (this.selector) ? $item2.find(this.selector) : $item2;
            var t1 = "";
            var t2 = "";
            if (!this.attribute) {
                t1 = $e1.text();
                t2 = $e2.text();    
            } else if(this.attribute.substr(0, 5) == "data-") {
                t1 = $e1.data(this.attribute.substr(5));
                t2 = $e2.data(this.attribute.substr(5));
            } else {
                t1 = $e1.attr(this.attribute);
                t2 = $e2.attr(this.attribute);
            }

            if (this.isNumeric) {
                t1 = t1 * 1;
                t2 = t2 * 1;
                if (isNaN(t1)) t1 = 0;
                if (isNaN(t2)) t2 = 0;
            }

            if (t1 > t2) return 1 * this.reverseMultiplier;
            if (t1 < t2) return -1 * this.reverseMultiplier;
            return 0;            
        },
        /**
        * Return the compare function for use in Array.sort()
        * @return {function} The compare function (bound to this object)
        */
        getCompareFn: function() {
            return this.compare.bind(this);
        }
    });


    /**
     * Display mode: grid view; value: "grid"
     * @type {String}
     */
    CUI.CardView.DISPLAY_GRID = DISPLAY_GRID;

    /**
     * Display mode: list view; value: "list"
     * @type {String}
     */
    CUI.CardView.DISPLAY_LIST = DISPLAY_LIST;

    /**
     * Single selection mode; value: "single"
     * @type {String}
     */
    CUI.CardView.SELECTION_MODE_COUNT_SINGLE = "single";

    /**
     * Multi selection mode; value: "multiple"
     * @type {String}
     */
    CUI.CardView.SELECTION_MODE_COUNT_MULTI = "multiple";

    /**
     * Utility method to get a {@link CUI.CardView} for the specified jQuery element.
     * @param {jQuery} $el The jQuery element to get the widget for
     * @return {CUI.CardView} The widget
     */
    CUI.CardView.get = function($el) {
        var cardView = Utils.getWidget($el);
        if (!cardView) {
            cardView = Utils.getWidget($el.cardView());
        }
        return cardView;
    };

    CUI.util.plugClass(CUI.CardView);

    // Data API
    if (CUI.options.dataAPI) {
        $(function() {
            var cardViews = $('body').find('[data-toggle="cardview"]');
            for (var gl = 0; gl < cardViews.length; gl++) {
                var $cardView = $(cardViews[gl]);
                if (!$cardView.data("cardview")) {
                    $cardView.cardView();
                }
            }
        });
    }

    // additional JSdoc

    /**
     * Triggered when a new card has been inserted succesfully.
     * @name CUI.CardView#change:insertitem
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {*} evt.item The inserted item (data model)
     */

    /**
     * Triggered when the grid selection mode changes.
     * @name CUI.CardView#change:gridSelect
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {Boolean} evt.oldValue True if grid select mode was previously active
     * @param {Boolean} evt.value True if grid select mode is now active
     */

    /**
     * Triggered when the display mode (list/grid view) changes. Display modes are
     * defined by their respective String constants, see for example
     * {@link CUI.CardView.DISPLAY_GRID}.
     * @name CUI.CardView#change:displayMode
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {String} evt.oldValue The old display mode
     * @param {String} evt.value The new display mode
     */

    /**
     * Triggered when the selection changes.
     * @name CUI.CardView#change:selection
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     * @param {*} evt.item The card that is (de)selected (data model)
     * @param {Boolean} evt.isSelected True if the item is now selected
     * @param {Boolean} evt.moreSelectionChanges True if there are more selection changes
     *        following (multiple single selection changes can be treated as one big
     *        selection change)
     */

    /**
     * Triggered right before the selection changes if (and only if) the selection is
     * changed using {@link CUI.CardView#toggleSelection}. The selection change can be
     * vetoed by calling cancelSelection on the Event object.
     * @name CUI.CardView#beforeselect
     * @event
     * @param {Object} evt The event
     * @param {*} evt.item The card that is will get (de)selected (data model)
     * @param {Function} evt.changeSelection This function may be called to cancel the
     *        selection; if true is passed as an argument, the originating event (if
     *        applicable; for example if the selection change is triggered by a user
     *        interaction) is cancelled as well (no event propagation; no default browser
     *        behavior)
     */

    /**
     * Triggered after an item has been moved with drag&drop to a new place in the list by the user.
     * @name CUI.CardView#item-moved
     * @event
     * @param {Object} evt          The event
     * @param {Object} evt.oldPrev  The jQuery element that was previous to the item before dragging started, may be empty or a header
     * @param {Object} evt.oldNext  The jQuery element that was next to the item before dragging started, may be empty
     * @param {Object} evt.newPrev  The jQuery element that is now previous to the item, may be empty or a header
     * @param {Object} evt.newNext  The jQuery element that is now next to the item, may be empty
     * @param {boolean} evt.hasMoved  True if the item really moved or false if it has the some position after the drag action as before.
     */

    /**
     * Triggered right before a column sort action on the list is started (when the user clicks on a column). The client side
     * sorting can be vetoed by setting preventDefault() on the event object. The event target is set to the column header the user clicked on.
     * The sortstart event is always triggered, even if the column has no client side sort configuration.
     * @name CUI.CardView#sortstart
     * @event
     * @param {Object} evt The event
     */

    /**
     * Triggered right after a sorting action on the list has been finished (when the user has clicked on a column).
     * The event target is set to the column header the user clicked on. This event is always triggered, even if the column does not have
     * a client side sort configuration.
     * @name CUI.CardView#sortend
     * @event
     * @param {Object} evt The event
     */

    /**
     * Triggered when all cards are removed.
     * @name CUI.CardView#change:removeAll
     * @event
     * @param {Object} evt The event
     * @param {CUI.CardView} evt.widget The widget
     */

}(window.jQuery));

(function($) {
    CUI.PathBrowser = new Class(/** @lends CUI.PathBrowser# */{
        toString: 'PathBrowser',
        extend: CUI.Widget,

        /**
         @extends CUI.Widget
         @classdesc An autocompletable path browser widget

         <p>
            <select data-init="pathbrowser" data-placeholder="Select path">
                <option>/apps</option>
                <option>/content</option>
                <option>/etc</option>
                <option>/libs</option>
                <option>/tmp</option>
                <option>/var</option>
            </select>
         </p>

         @desc Creates a path browser field
         @constructs

         @param {Object}   options                                    Component options
         @param {Array}    [options.options=empty array]              Array of available options (will be read from &lt;select&gt; by default)
         @param {Array}    [options.optionDisplayStrings=empty array] Array of alternate strings for display (will be read from &lt;select&gt; by default)
         @param {Function} [options.optionLoader=use options]         (Optional) Callback to reload options list. Can be synch or asynch. In case of asynch handling, use second parameter as callback function: optionLoader(string currentPath, function callback) with callback(array resultArray)
         @param {String}   [options.optionLoaderRoot=use options]     (Optional) Nested key to use as root to retrieve options from the option loader result
         @param {Function} [options.optionValueReader=use options]    (Optional) Custom function to call to retrieve the value from the option loader result
         @param {Function} [options.optionTitleReader=use options]    (Optional) Custom function to call to retrieve the title from the option loader result
         @param {boolean}  [options.showTitles=true]                  Should option titles be shown?
         @param {String}   [options.rootPath='/content']              The root path where completion and browsing starts.
                                                                      Use the empty string for the repository root (defaults to '/content').
         @param {String}   [options.placeholder=null]                 Define a placeholder for the input field
         @param {int}      [options.delay=200]                        Delay before starting autocomplete when typing
         @param {int}      [options.disabled=false]                   Is this component disabled?
         @param {String}   [options.name=null]                        (Optional) name for an underlying form field.
         @param {Function} [options.autocompleteCallback=use options] Callback for autocompletion
         @param {Function} [options.optionRenderer=default renderer]  (Optional) Renderer for the autocompleter and the tag badges
         @param {String}   [options.position="below"]                 Where to position the dropdown list. "above" or "below"
         @param {boolean}  [options.autoPosition=true]                Should the dropdown auto position itself if there's not enough space for the default position in the window?
         */
        construct: function(options) {
            // Set callback to default if there is none
            if (!this.options.autocompleteCallback) {
                this.options.autocompleteCallback = this._defaultAutocompleteCallback.bind(this);
            }
            if (!this.options.optionRenderer) {
                this.options.optionRenderer = CUI.PathBrowser.defaultOptionRenderer;
            }

            // Adjust DOM to our needs
            this._render();

            // Populate alternative display strings if necessary
            while (this.options.optionDisplayStrings.length < this.options.options.length) {
                this.options.optionDisplayStrings.push(this.options.options[this.options.optionDisplayStrings.length]);
            }

            // Generate Dropdown List widget
            this.dropdownList = new CUI.DropdownList({
                element: this.inputElement,
                positioningElement: this.inputElement,
                position: this.options.position,
                autoPosition: this.options.autoPosition,
                cssClass: "autocomplete-results"
            });

            // Listen to property changes
            this.$element.on('change:disabled', this._update.bind(this));
            this.$element.on('change:placeholder', this._update.bind(this));
            this.$element.on('change:options', this._changeOptions.bind(this));

            // Listen to events
            this.$element.on("input", "input", function() {
                if (this.options.disabled) {
                    return;
                }
                if (this.typeTimeout) {
                    clearTimeout(this.typeTimeout);
                }
                this.typeTimeout = setTimeout(this._inputChanged.bind(this), this.options.delay);
            }.bind(this));

            this.$element.on("blur", "input", function() {
                if (this.options.disabled) {
                    return;
                }
                if (this.typeTimeout) {
                    clearTimeout(this.typeTimeout);
                }
                this.typeTimeout = null;
                // Set to existing selection for single term use
                if (this.selectedIndex >= 0) {
                    if (this.inputElement.attr("value") === "") {
                        this.setSelectedIndex(-1);
                    } else {
                        this._update();
                    }
                }
            }.bind(this));

            this.$element.on("keydown", "input", this._keyPressed.bind(this));
            this.$element.on("keyup", "input", this._keyUp.bind(this));

            this.dropdownList.on("dropdown-list:select", "", function(event) {
                this.dropdownList.hide(200);
                this.setSelectedIndex(event.selectedValue * 1);
            }.bind(this));

            // focus setting confuses mobile safari
            if (!CUI.util.isTouch) {
                this.$element.on("focus", "input", function(event) {
                    if (this.options.disabled) {
                        return;
                    }
                    this.$element.addClass("focus");
                }.bind(this));

                this.$element.on("blur", "input", function() {
                    if (this.options.disabled) {
                        return;
                    }
                    this.$element.removeClass("focus");
                }.bind(this));

                this.$element.on("click touchend", "input", function(event) {
                    if (this.options.disabled) {
                        return;
                    }
                    
                    this.inputElement.focus();
                    this._inputChanged();
                }.bind(this));    
            }
        },

        defaults: {
            autocompleteCallback: null,
            options: [],
            optionDisplayStrings: [],
            optionLoader: null,
            optionLoaderRoot: null,
            optionValueReader: null,
            optionTitleReader: null,
            showTitles: true,
            rootPath: "/content",
            delay: 200,
            placeholder: null,
            optionRenderer: null,
            position: "below",
            autoPosition: true
        },

        dropdownList: null, // Reference to instance of CUI.DropdownList
        syncSelectElement: null,
        inputElement: null,
        typeTimeout: null,
        selectedIndex: -1,
        triggeredBackspace: false,

        /**
         * @param {int} index Sets the currently selected option by its index.
         *                    -1 removes any selected index.
         */
        setSelectedIndex: function(index) {
            if (index < -1 || index >= this.options.options.length) {
                return;
            }
            this.selectedIndex = index;
            this._update();
        },

        /**
         * @return {int} The currently selected options by index or -1 if none is selected
         */
        getSelectedIndex: function() {
            return this.selectedIndex;
        },

        /** @ignore */
        _changeOptions: function(event) {
            if (event.widget !== this) {
                return;
            }
            this.selectedIndex = -1;
            this._update();
        },

        /** @ignore */
        _render: function() {
            this._readDataFromMarkup();

            var div;
            // if current element is select field -> turn into input field, but hold reference to select to update it on change
            if (this.$element.get(0).tagName === "SELECT") {
                div = $("<div></div>");
                this.$element.after(div);
                this.$element.detach();
                div.append(this.$element);
                this.$element = div;
            }

            // if current element is input field -> wrap it into DIV
            if (this.$element.get(0).tagName === "INPUT") {
                div = $("<div></div>");
                this.$element.after(div);
                this.$element.detach();
                div.prepend(this.$element);
                this.$element = div;
            }

            // If there was an select in markup: use it for generating options
            if (this.$element.find("select option").length > 0 && this.options.options.length === 0) {
                this.options.options = [];
                this.options.optionDisplayStrings = [];
                this.$element.find("select option").each(function(i, e) {
                    this.options.options.push($(e).val());
                    this.options.optionDisplayStrings.push($.trim($(e).text()));

                    // Save selected state
                    if ($(e).attr("selected")) {
                        this.selectedIndex = i;
                    }

                }.bind(this));
            }

            this._createMissingElements();

            this.syncSelectElement = this.$element.find("select");
            this.inputElement = this.$element.find("input");

            this.$element.addClass("pathbrowser");
            this.$element.removeClass("focus");

            if (!this.options.placeholder) {
                this.options.placeholder = this.inputElement.attr("placeholder");
            }
            if (this.options.name) {
                this.syncSelectElement.attr("name", this.options.name);
            }

            this._update();
        },

        _createMissingElements: function() {
            if (this.$element.find("select").length === 0) {
                this.$element.append($("<select></select>"));
            }
            if (this.$element.find("input").length === 0) {
                this.$element
                    .prepend($("<input/>", {
                        type: "text"
                    })
                );
            }
        },

        /** @ignore */
        _readDataFromMarkup: function() {
            if (this.$element.attr("placeholder")) {
                this.options.placeholder = this.$element.attr("placeholder");
            }
            if (this.$element.attr("data-placeholder")) {
                this.options.placeholder = this.$element.attr("data-placeholder");
            }
            if (this.$element.attr("disabled") || this.$element.attr("data-disabled")) {
                this.options.disabled = true;
            }
            if (this.$element.attr("data-option-renderer")) {
                // Allow to choose from default option renderers
                this.options.optionRenderer = CUI.PathBrowser[this.$element.attr("data-option-renderer") + "OptionRenderer"];
            }

            if (this.$element.attr("data-root-path")) {
                this.options.rootPath = this.$element.attr("data-root-path");
            }

            // Register a callback function for option loader if defined
            var optionLoader = CUI.util.buildFunction(this.$element.attr("data-option-loader"), ["path", "callback"]);
            if (optionLoader) {
                this.options.optionLoader = optionLoader.bind(this);
            }
            // Root to use from the result object
            if (this.$element.attr("data-option-loader-root")) {
                this.options.optionLoaderRoot = this.$element.attr("data-option-loader-root");
            }
            // Custom value and title readers
            var optionValueReader = CUI.util.buildFunction(this.$element.attr("data-option-value-reader"), ["object"]);
            if (optionValueReader) {
                this.options.optionValueReader = optionValueReader.bind(this);
            }
            var optionTitleReader = CUI.util.buildFunction(this.$element.attr("data-option-title-reader"), ["object"]);
            if (optionTitleReader) {
                this.options.optionTitleReader = optionTitleReader.bind(this);
            }
        },

        /** @ignore */
        _update: function() {
            if (this.options.placeholder) {
                this.inputElement.attr("placeholder", this.options.placeholder);
            }

            if (this.options.disabled) {
                this.$element.addClass("disabled");
                this.inputElement.attr("disabled", "disabled");
            } else {
                this.$element.removeClass("disabled");
                this.inputElement.removeAttr("disabled");
            }

            if (this.syncSelectElement) {
                this.syncSelectElement.find("option:selected").removeAttr("selected");
            }
            if (this.selectedIndex >= 0) {
                if (this.syncSelectElement) {
                    $(this.syncSelectElement.find("option").get(this.selectedIndex)).attr("selected", "selected");
                }
                // Value to set is what is currently in the input field until the last slash + the option value
                var option = this.options.options[this.selectedIndex];
                if (option && option.indexOf("/") !== 0) {
                    // Option contains a relative path
                    var parentPath = "";
                    var iLastSlash = this.inputElement.attr("value").lastIndexOf("/");
                    if (iLastSlash >= 0) {
                        parentPath = this.inputElement.attr("value").substring(0, iLastSlash + 1);
                    }
                    option = parentPath + option;
                }
                this._setInputValue(option, true);
            } else {
                this._setInputValue("");
            }
        },

        /** @ignore */
        _setInputValue: function(newValue, moveCursor) {
            // Using select text util to select starting from last character to last character
            // This way, the cursor is placed at the end of the input text element
            if (newValue) {
                this.inputElement.attr("value", newValue);
                if (moveCursor && this.inputElement.is(":focus")) {
                    CUI.util.selectText(this.inputElement, newValue.length);
                }
            }
        },

        /** @ignore */
        _keyUp: function(event) {
            var key = event.keyCode;
            if (key === 8) {
                this.triggeredBackspace = false; // Release the key event
            }
        },

        /** @ignore */
        _keyPressed: function(event) {
            var key = event.keyCode;
            if (!this.dropdownList.isVisible()) {
                if (key === 40) {
                    this._inputChanged(); // Show box now!
                    event.preventDefault();
                }
            }
        },

        /** @ignore */
        _inputChanged: function() {
            var self = this;

            var searchFor = this.inputElement.attr("value");
            if (searchFor.length > 0) {
                this.options.autocompleteCallback(searchFor)
                    .done(
                        function(results) {
                            self._showAutocompleter(results);
                        }
                    )
                    .fail(
                        function() {
                            // TODO: implement
                        }
                    )
                ;
            } else {
                this.dropdownList.hide();
            }
        },

        /** @ignore */
        _showAutocompleter: function(results) {
            this.dropdownList.hide();

            if (results.length === 0) {
                return;
            }

            var optionRenderer = function(iterator, value) {
                return (this.options.optionRenderer.bind(this))(iterator, value);
            };

            this.dropdownList.set("optionRenderer", optionRenderer.bind(this));
            this.dropdownList.set("options", results);

            this.dropdownList.show();
        },

        /** @ignore */
        _defaultAutocompleteCallback: function(path) {
            var self = this;
            var def = $.Deferred();

            // Check if the input value starts and ends with a slash
            // If so, the options loader will be called if it exists, in order
            // to refresh the available options list.
            // Otherwise, it will just filter the options to only show the
            // matching ones in the auto completer div.
            if (/^\//.test(path) && /\/$/.test(path) && self.options.optionLoader) {
                var isCustomRoot = false;
                if (path === "/") {
                    // Use configured root path
                    if (self.options.rootPath) {
                        path = self.options.rootPath.replace(/\/$/, "");
                        if (path !== "") {
                            isCustomRoot = true;
                        } else {
                            path = "/";
                        }
                    }
                } else {
                    // Remove final slash
                    path = path.replace(/\/$/, "");
                }

                // Replace field value if the entered path was a custom root path
                if (isCustomRoot) {
                    self._setInputValue(path + "/");
                }

                // Make the option loader a promise to guarantee that the callback is
                // executed at the right rime
                var loader = {
                    loadOptions: self.options.optionLoader
                };
                var loaderDef = $.Deferred();
                loaderDef.promise(loader);
                loader.done(
                    function(object) {
                        if ($.isFunction(object.promise)) {
                            // Original function was already returning a promise
                            // Bind the rebuild options on that object's 'done' method
                            object.done(
                                function(object) {
                                    self._rebuildOptions(def, path, object);
                                }
                            );
                        } else {
                            // Original function was not returning a promise
                            self._rebuildOptions(def, path, object);
                        }
                    }
                );
                
                // Asynch optionLoader
                var results = loader.loadOptions(path, function(data) {
                    loaderDef.resolve(data);
                });
                
                //  Synch optionLoader
                if (results) loaderDef.resolve(results);

            } else {
                def.resolve(self._filterOptions(path));
            }

            return def.promise();
        },

        _rebuildOptions: function(def, path, object) {
            var self = this;

            var root = CUI.util.getNested(object, self.options.optionLoaderRoot);
            if (root) {
                var newOptions = [];
                var newOptionDisplayStrings = [];
                $.each(root, function(i, v) {
                    // Read the title and the value either from provided custom reader
                    // or using default expected object structure
                    var value;
                    if (self.options.optionValueReader) {
                        value = self.options.optionValueReader(v);
                    } else {
                        value = typeof v === "object" ? v.path : v;
                    }
                    newOptions.push(value);

                    var title = "";
                    if (self.options.optionTitleReader) {
                        title = self.options.optionTitleReader(v);
                    } else if (typeof v === "object") {
                        title = v.title;
                    }
                    newOptionDisplayStrings.push(title);
                }.bind(self));

                self.options.options = newOptions;
                self.options.optionDisplayStrings = newOptionDisplayStrings;

                var filtered = self._filterOptions(path);
                def.resolve(filtered);
            } else {
                def.reject();
            }
        },

        _filterOptions: function(searchFor) {
            var result = [];

            $.each(this.options.options, function(key, value) {
//                if (value.toLowerCase().indexOf(searchFor.toLowerCase(), 0) >= 0) {
                    result.push(key);
//                }
            }.bind(this));

            return result;
        }

    });

    CUI.util.plugClass(CUI.PathBrowser);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            $("[data-init~='pathbrowser']", e.target).pathBrowser();
        });
    }

    CUI.PathBrowser.defaultOptionRenderer = function(iterator, index) {
        var value = this.options.options[index];
        if (value.indexOf("/") === 0) {
            // Option contains an absolute path
            var iLastSlash = value.lastIndexOf("/");
            value = value.substring(iLastSlash + 1);
        }

        // Use alternate display strings if possible
        var valueCls = "pathbrowser-autocomplete-item-value";
        var titleMarkup = "";
        if (this.options.showTitles && this.options.optionDisplayStrings[index] && this.options.optionDisplayStrings[index].length > 0) {
            valueCls += " pathbrowser-autocomplete-item-value-with-title";
            titleMarkup += "<div class=\"pathbrowser-autocomplete-item-title\">" + this.options.optionDisplayStrings[index] + "</div>";
        }

        return $("<div class=\"" + valueCls + "\">" + value + "</div>" + titleMarkup);
    };


}(window.jQuery));
(function($) {
  CUI.Wizard = new Class(/** @lends CUI.Wizard# */{
    toString: 'Wizard',

    extend: CUI.Widget,
    /**
     * @extends CUI.Widget
     * @classdesc A wizard widget to navigate throught a form.
     *    
     *  <div class="wizard" data-init="wizard">
     *      <nav>
     *          <button class="back">Back</button>
     *          <ol>
     *              <li>First step</li>
     *              <li>Second step</li>
     *              <li>Third step</li>
     *              <li>Last step</li>
     *          </ol>
     *          <button class="next" disabled>Next</button>
     *      </nav>
     *      <section data-next-disabled="false" data-back-label="Cancel">
     *          The first step is optional.
     *      </section>
     *      <section data-next-disabled="false">
     *          The second step is optional.
     *      </section>
     *      <section data-next-disabled="false">
     *          The third step is optional.
     *      </section>
     *      <section data-next-label="Create">
     *          Name is required.
     *      </section>
     *  </div>
     *     
     *  <h2>Data Attributes</h2>
     *  <h4>Currently there are the following data options:</h4>
     *  <pre>
     *    data-hide-steps               If true, step will be hidden (useful for one step wizard).
     *    data-init="wizard"
     *  </pre>
     *
     *  <h4>Currently there are the following data options on section pages:</h4>
     *  <pre>
     *    data-wizard-page-callback     Callback identifier if the pageChanged options contains several callbacks
     *    data-next-label               Specify the label of the `next button`
     *    data-back-label               Specify the label of the `back button`
     *    data-next-disabled            Speficy if the `next button` should be disabled
     *    data-back-disabled            Speficy if the `back button` should be disabled
     *  </pre>
     *
     *  @example
     *  <caption>Instantiate by data API</caption>
     *  &lt;div class=&quot;wizard&quot; data-init=&quot;wizard&quot;&gt;
     *      &lt;input type=&quot;datetime&quot; value=&quot;2012-10-20 11:10&quot;&gt;
     *      &lt;button&gt;&lt;span class=&quot;icon-calendar small&quot;&gt;Datetime picker&lt;/span&gt;&lt;/button&gt;
     *  &lt;/div&gt;
     *     
     *  @example
     *  <caption>Instantiate with Class</caption>
     *  var wizard = new CUI.Wizard({
     *    element: '#myOrdinarySelectBox'
     *  });
     *     
     *  @example
     *  <caption>Instantiate by jQuery plugin</caption>
     *  $("div.wizard").wizard();
     *
     * @desc Creates a new wizard widget 
     * @constructs
     *
     * @param {Object} options Component options
     * @param {Mixed} options.element jQuery selector or DOM element to use for panel
     * @param {Function|Object} options.onPageChanged Callback called each time the page change (with arguments: `page`). An Collection of functions can be given. When a page is displayed if his data-wizard-page-callback attribute can be found in the collection, then the corresponding callback will be executed (examples is given in guide/wizard.html).
     * @param {Function} options.onFinish Callback called when the user is on the last page and clicks on the `next button` (without arguments)
     * @param {Function} options.onLeaving Callback called when the user is on the first page and clicks on the `back button` (without arguments)
     * @param {Function} options.onNextButtonClick Callback called after a click the on `next button` before the page change (without arguments) The page won't change if the callback return false.
     * @param {Function} options.onBackButtonClick Callback called after a click the on `back button` before the page change (without arguments) The page won't change if the callback return false.
     */
    construct: function(options) {
      this.$nav = this.$element.find('nav').first();
      this.$back = this.$nav.find('button').first();
      this.$next = this.$nav.find('button').last();
      this.$pageOverview = this.$nav.find('ol').last();

      if (this.$element.data("hide-steps") === true) {
        this.$pageOverview.addClass("hidden");
      }

      if (this.$back.attr('type') === undefined) {
          this.$back[0].setAttribute('type', 'button');
      }

      if (this.$next.attr('type') === undefined) {
          this.$next[0].setAttribute('type', 'button');
      }

      // Set toolbar classes
      this.$nav.addClass('toolbar');
      this.$back.addClass('left');
      this.$next.addClass('right');
      this.$pageOverview.addClass('center');

      // Add div to render leading fill for first list item
      this.$nav.find('li').first().append('<div class="lead-fill"></div>');

      this.$next.click(this._onNextClick.bind(this));
      this.$back.click(this._onBackClick.bind(this));

      this._updateDefault();

      // Start with first page
      // Asynchronous to make the wizard object available in the option callback (onPageChanged)
      setTimeout(function() { 
        this.changePage(1);
      }.bind(this), 1);
      
    },

    defaults: {
      nextDisabled: false,
      backDisabled: false,
      nextLabel: 'next',
      backLabel: 'back',
      onPageChanged: null,
      onFinish: null,
      onLeaving: null,
      onNextButtonClick: null,
      onBackButtonClick: null
    },

    /**
     * Change the page.
     *
     * Page number start with 1 and not 0.
     * Page number should be between 1 and number of sections.
     *
     * @param {Integer} pageNumber The page number
     */
    changePage: function(pageNumber) {
      if (pageNumber < 1 || pageNumber > this.$nav.find('li').length) return ;

      this.pageNumber = pageNumber;
      var page = this.pageNumber - 1;
      var $newPage = this.getCurrentPage();

      this.$nav.find('li').removeClass('stepped');
      this.$nav.find('li:lt(' + page + ')').addClass('stepped');

      this.$nav.find('li.active').removeClass('active');
      this.$nav.find('li:eq(' + page + ')').addClass('active');

      this.$element.find('>section.active').removeClass('active');
      this.$element.find('>section:eq(' + page + ')').addClass('active');

      this._updateButtons();

      // Accept a callback or a collection of callbacks
      this._fireCallback('onPageChanged');
      if (typeof this.options.onPageChanged === 'object' &&
              this._dataExists($newPage, 'wizardPageCallback') &&
              typeof this.options.onPageChanged[$newPage.data('wizardPageCallback')] === 'function') {
        this.options.onPageChanged[$newPage.data('wizardPageCallback')]($newPage);
      }
    },

    /**
     * Return the number of the current page
     *
     * Page number start with 1 and not 0.
     *
     * @return {Integer} The page number
     */
    getCurrentPageNumber: function() {
      return this.pageNumber;
    },

    /**
     * Return the current page
     *
     * Page number start with 1 and not 0.
     *
     * @return {Integer} The page number
     */
    getCurrentPage: function() {
      return this.getPage(this.pageNumber);
    },

    /**
     * Returns the page specifed by page number
     *
     * @return {Object} The page
     */
    getPage: function(pageNumber) {
      return this.$element.find('>section:eq('+ (parseFloat(pageNumber)-1) +')');
    },

    /**
     * Returns the page specifed by page number
     *
     * @return {Object} The page
     */
    getPageNav: function(pageNumber) {
      return this.$element.find('>nav li:eq('+ (parseFloat(pageNumber)-1) +')');
    },

    /**
     * Set the label of the `next` button 
     *
     * @return {String} The label
     */
    setNextButtonLabel: function(label) {
      this.$next.text(label);
    },

    /**
     * Set the label of the `back` button
     *
     * @return {String} The label
     */
    setBackButtonLabel: function(label) {
      this.$back.text(label);
    },

    /**
     * Set or remove the disabled attribe of the next button
     *
     * @param {Boolean} If true the button will be disabled, if not it will be enabled
     */
    setNextButtonDisabled: function(disabled) {
      this.$next.attr('disabled', disabled);
    },

    /**
     * Set or remove the disabled attribe of the back button
     *
     * @param {Boolean} If true the button will be disabled, if not it will be enabled
     */
    setBackButtonDisabled: function(disabled) {
      this.$back.attr('disabled', disabled);
    },

    activatePage: function(pageNumber) {
      this.getPage(pageNumber).removeClass('wizard-hidden-step');
      this.getPageNav(pageNumber).removeClass('wizard-hidden-step');
    },

    deactivatePage: function(pageNumber) {
      this.getPage(pageNumber).addClass('wizard-hidden-step');
      this.getPageNav(pageNumber).addClass('wizard-hidden-step');
    },

    /** @ignore */
    _onNextClick: function(e) {
      var callbackResult = this._fireCallback('onNextButtonClick');
      
      if (callbackResult === false) {
        return ;
      }
      
      var pageNumber = this._getNextPageNumber();

      if (pageNumber != null) {
        this.changePage(pageNumber);
      } else {
        this._fireCallback('onFinish');
      }
    },

    /** @ignore */
    _onBackClick: function(e) {
      var callbackResult = this._fireCallback('onBackButtonClick');
      
      if (callbackResult === false) {
        return ;
      }

      var pageNumber = this._getPreviousPageNumber();

      if (pageNumber != null) {
        this.changePage(pageNumber);
      } else {
        this._fireCallback('onLeaving');
      }
    },

    /**
     * returns the next page to display.
     * retruns null if the current page is the last one.
     *
     * @return {Integer} the page number
     *
     * @ignore
     */
    _getNextPageNumber: function() {
      var pageNumber = this.getCurrentPageNumber();
      return this._getRelativeNextPageNumber(pageNumber);
    },

    /**
     * return the next page to display from a page number
     * retruns null if the current page is the last one.
     *
     * @param {Integer} pageNumber page number
     * @return {Integer} the page number
     *
     * @ignore
     */
    _getRelativeNextPageNumber: function(pageNumber) {
      if (pageNumber < this.$nav.find('li').length) {
        var newPageNumber = pageNumber + 1;
        var page = this.getPage(newPageNumber);

        if ($(page).hasClass('wizard-hidden-step')) {
          return this._getRelativeNextPageNumber(newPageNumber);
        } else {
          return newPageNumber;
        }

      } else {
        return null;
      }
    },

    /**
     * return the previous page to display
     * retruns null if the current page is the first one.
     *
     * @return {Integer} the page number
     *
     * @ignore
     */
    _getPreviousPageNumber: function() {
      var pageNumber = this.getCurrentPageNumber();
      return this._getRelativePreviousPageNumber(pageNumber);
    },

    /**
     * return the previous page to display from a page number
     * retruns null if the current page is the first one.
     *
     * @param {Integer} pageNumber page number
     * @return {Integer} the page number
     *
     * @ignore
     */
    _getRelativePreviousPageNumber: function(pageNumber) {
      if (pageNumber > 1) {
        var newPageNumber = pageNumber - 1;
        var page = this.getPage(newPageNumber);

        if ($(page).hasClass('wizard-hidden-step')) {
          return this._getRelativePreviousPageNumber(newPageNumber);
        } else {
          return newPageNumber;
        }

        return pageNumber-1;
      } else {
        return null;
      }
    },

    /** @ignore */
    _updateButtons: function() {
      var page = this.getCurrentPage();

      this.setNextButtonLabel((this._dataExists(page, 'nextLabel')) ? page.data('nextLabel') 
        : this.options.nextLabel);
      this.setBackButtonLabel((this._dataExists(page, 'backLabel')) ? page.data('backLabel') 
        : this.options.backLabel);
      this.setNextButtonDisabled((this._dataExists(page, 'nextDisabled')) ? page.data('nextDisabled') 
        : this.options.nextDisabled);
      this.setBackButtonDisabled((this._dataExists(page, 'backDisabled')) ? page.data('backDisabled') 
        : this.options.backDisabled);
    },

    /** @ignore */
    _fireCallback: function(callback) {
        if (typeof this.options[callback] === 'function') {
          return this.options[callback]();
        }
        return undefined;
    },

    /** @ignore */
    /* jQuery doesn't have any method to check if a data exists */
    _dataExists: function($element, index) {
      return $element.data(index) !== undefined;
    },

    /** @ignore */
    _updateDefault: function() {
        this.options.nextDisabled = this.$next.is('[disabled]');
        this.options.backDisabled = this.$back.is('[disabled]');
        this.options.nextLabel = this.$next.text();
        this.options.backLabel = this.$back.text();
    }
  });

  CUI.util.plugClass(CUI.Wizard);

  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
      $("[data-init~=wizard]", e.target).wizard();
    });
  }
}(window.jQuery));

/*
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
*/
(function($) {
    function cloneLeft(buttons) {
        return buttons.filter("[data-action=prev], [data-action=cancel]").first().addClass("hidden")
            .clone().addClass("back left").each(processButton);
    }

    function cloneRight(buttons) {
        return buttons.filter("[data-action=next]").first().addClass("hidden")
            .clone().addClass("primary right").each(processButton);
    }

    function processButton(i, el) {
        $(el).removeClass("hidden").not("button").toggleClass("button", true);
    }

    function buildNav(wizard) {
        var sections = wizard.children(".step");
        var nav;

        if (wizard.children(".toolbar").length === 0) {
            wizard.prepend(function() {
                nav = $('<nav class="toolbar"><ol class="center"></ol></nav>');
                var ol = nav.children("ol");

                sections.map(function() {
                    return $("<li />").text(this.title);
                }).appendTo(ol);

                return nav;
            });
        } else {
            nav = wizard.children("nav");
        }

        nav.find("> ol > li").first().addClass("active").append("<div class='lead-fill' />");

        var buttons = sections.first().find(".flexwizard-control");

        nav.prepend(function() {
            return cloneLeft(buttons);
        }).append(function() {
            return cloneRight(buttons);
        });
    }

    function showNav(to) {
        if (to.length === 0) return;

        to.addClass("active").removeClass("stepped");
        to.prevAll("li").addClass("stepped").removeClass("active");
        to.nextAll("li").removeClass("active stepped");
    }

    function showStep(wizard, to, from) {
        if (to.length === 0) return;

        if (from) {
            from.removeClass("active");
        }

        to.toggleClass("active", true);

        wizard.trigger("flexwizard-stepchange", [to, from]);
    }

    function controlWizard(wizard, action) {
        var nav = wizard.children("nav");
        var from = wizard.children(".step.active");
        var fromNav = nav.children("ol").children("li.active");

        var to, toNav;
        switch (action) {
            case "prev":
                to = from.prev(".step");
                toNav = fromNav.prev("li");
                break;
            case "next":
                to = from.next(".step");
                toNav = fromNav.next("li");
                break;
            case "cancel":
                return;
        }

        if (to.length === 0) return;

        var buttons = to.find(".flexwizard-control");

        cloneLeft(buttons).replaceAll(nav.children(".left"));
        cloneRight(buttons).replaceAll(nav.children(".right"));

        showNav(toNav);
        showStep(wizard, to, from);
    }

    CUI.FlexWizard = new Class({
        toString: "FlexWizard",

        extend: CUI.Widget,

        construct: function(options) {
            var wizard = this.$element;

            buildNav(wizard);

            wizard.on("click", ".flexwizard-control", function(e) {
                controlWizard(wizard, $(this).data("action"));
            });

            showStep(wizard, wizard.children(".step").first());
        }
    });

    CUI.util.plugClass(CUI.FlexWizard);

    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            $("[data-init~=flexwizard]", e.target).flexWizard();
        });
    }
}(window.jQuery));

(function($) {
    CUI.FileUpload = new Class(/** @lends CUI.FileUpload# */{
        toString: 'FileUpload',
        extend: CUI.Widget,

        /**
         Triggered when a file is selected and accepted into the queue

         @name CUI.FileUpload#fileselected
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when a selected file is rejected before upload

         @name CUI.FileUpload#filerejected
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {String} evt.message            The reason why the file has been rejected
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when the internal upload queue changes (file added, file uploaded, etc.)

         @name CUI.FileUpload#queuechanged
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {String} evt.operation          The operation on the queue (ADD or REMOVE)
         @param {int} evt.queueLength           The number of items in the queue
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when selected files list is processed

         @name CUI.FileUpload#filelistprocessed
         @event

         @param {Object} evt                    Event object
         @param {int} evt.addedCount            The number of files that have been added to the processing list
         @param {int} evt.rejectedCount         The number of files that have been rejected
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file starts

         @name CUI.FileUpload#fileuploadstart
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file progresses

         @name CUI.FileUpload#fileuploadprogress
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event (from which the upload ratio can be calculated)
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file is completed (for non-HTML5 uploads only, regardless of success status)

         @name CUI.FileUpload#fileuploadload
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {String} evt.content            The server response to the upload request, which needs to be analyzed to determine if upload was successful
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file succeeded

         @name CUI.FileUpload#fileuploadsuccess
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file failed

         @name CUI.FileUpload#fileuploaderror
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {String} evt.message            The reason why the file upload failed
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when upload of a file has been cancelled

         @name CUI.FileUpload#fileuploadcanceled
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.item               Object representing a file item
         @param {Object} evt.originalEvent      The original upload event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when dragging over a drop zone

         @name CUI.FileUpload#dropzonedragover
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.originalEvent      The original mouse drag event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when dragging out of a drop zone

         @name CUI.FileUpload#dropzonedragleave
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.originalEvent      The original mouse drag event
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         Triggered when dropping files in a drop zone

         @name CUI.FileUpload#dropzonedrop
         @event

         @param {Object} evt                    Event object
         @param {Object} evt.originalEvent      The original mouse drop event
         @param {FileList} evt.files            The list of dropped files
         @param {Object} evt.fileUpload         The file upload widget
         */

        /**
         @extends CUI.Widget
         @classdesc A file upload widget

         <p>
         <span class="fileupload button icon-upload" data-init="fileupload"><input type="file" data-placeholder="Select file(s)"></span>
         </p>

         @desc Creates a file upload field
         @constructs

         @param {Object}   options                                    Component options
         @param {String}   [options.name="file"]                      (Optional) name for an underlying form field.
         @param {String}   [options.placeholder=null]                 Define a placeholder for the input field
         @param {String}   [options.uploadUrl=null]                   URL where to upload the file
         @param {String}   [options.uploadUrlBuilder=null]            Upload URL builder
         @param {boolean}  [options.disabled=false]                   Is this component disabled?
         @param {boolean}  [options.multiple=false]                   Can the user upload more than one file?
         @param {int}      [options.sizeLimit=null]                   File size limit
         @param {boolean}  [options.autoStart=false]                  Should upload start automatically once the file is selected?
         @param {String}   [options.fileNameParameter=null]           Name of File name's parameter
         @param {boolean}  [options.useHTML5=true]                    (Optional) Prefer HTML5 to upload files (if browser allows it)
         @param {boolean}  [options.dropZone=null]                    (Optional) Drop zone to upload files from file system directly (if browser allows it)
         @param {Object}   [options.events={}]                        (Optional) Event handlers
         */
        construct: function(options) {
            // Adjust DOM to our needs
            this._render();

            this.inputElement.on("change", function(event) {
                if (this.options.disabled) {
                    return;
                }
                this._onFileSelectionChange(event);
            }.bind(this));
        },

        defaults: {
            name: "file",
            placeholder: null,
            uploadUrl: null,
            uploadUrlBuilder: null,
            disabled: false,
            multiple: false,
            mimeTypes: null,
            sizeLimit: null,
            autoStart: false,
            fileNameParameter: null,
            useHTML5: true,
            dropZone: null,
            events: {}
        },

        inputElement: null,
        $spanElement: null,
        fileNameElement: null,
        uploadQueue: [],

        /** @ignore */
        _render: function() {
            var self = this;

            // If current element is input field -> wrap it into SPAN
            if (this.$element.get(0).tagName === "INPUT") {
                var clazz = this.$element.attr("class");

                var span = $("<span/>", {
                    "class": clazz
                });
                this.$element.removeAttr("class");
                this.$element.after(span);
                this.$element.detach();
                span.prepend(this.$element);
                this.$element = span;
            }

            // Get the span element
            this.$spanElement = this.$element.is("span") ? this.$element : this.$element.find("span");

            // Get the input element
            this.inputElement = this.$element.find("input[type='file']");

            // Read configuration from markup
            this._readDataFromMarkup();

            if (!CUI.util.HTTP.html5UploadSupported()) {
                this.options.useHTML5 = false;
            }

            this._createMissingElements();

            this.$element.addClass("fileupload");
            this.$element.removeClass("focus");

            if (this.inputElement.attr("title")) {
                this.$element.prepend($("<label/>", {
                    "for": self.options.name
                }).html(this.inputElement.attr("title")));
            }

            // Register event handlers
            if (this.options.events) {
                if (typeof this.options.events === "object") {
                    for (var name in this.options.events) {
                        this._registerEventHandler(name, this.options.events[name]);
                    }
                }
            }

            // Register drop zone
            if (this.options.useHTML5) {
                this.options.dropZone = this._registerDropZone();
            } else {
                this.options.dropZone = null;
            }

            if (!this.options.placeholder) {
                this.options.placeholder = this.inputElement.attr("placeholder");
            }

            if (this.options.autoStart) {
                this._registerEventHandler("fileselected", function(event) {
                    event.fileUpload.uploadFile(event.item);
                });
            }

            // URL built via JavaScript function
            if (this.options.uploadUrlBuilder) {
                this.options.uploadUrl = this.options.uploadUrlBuilder(this);
            }

            if (!this.options.uploadUrl || /\$\{.+\}/.test(this.options.uploadUrl)) {
                this.options.disabled = true;
            }

            this._update();
        },

        _registerDropZone: function() {
            var self = this;
            if (self.options.dropZone) {
                // TODO: provide an additional way to get the drop zone via a function

                // Try to get the drop zone via a jQuery selector
                try {
                    self.options.dropZone = $(self.options.dropZone);
                } catch (e) {
                    delete self.options.dropZone;
                }

                if (self.options.dropZone) {
                    self.options.dropZone
                        .on("dragover", function(e) {
                            if (self._isActive()) {
                                self.isDragOver = true;

                                if (e.stopPropagation) {
                                    e.stopPropagation();
                                }
                                if (e.preventDefault) {
                                    e.preventDefault();
                                }

                                self.$element.trigger({
                                    type: "dropzonedragover",
                                    originalEvent: e,
                                    fileUpload: self
                                });
                            }

                            return false;
                        })
                        .on("dragleave", function(e) {
                            if (self._isActive()) {
                                if (e.stopPropagation) {
                                    e.stopPropagation();
                                }
                                if (e.preventDefault) {
                                    e.preventDefault();
                                }

                                self.isDragOver = false;
                                window.setTimeout(function() {
                                    if (!self.isDragOver) {
                                        self.$element.trigger({
                                            type: "dropzonedragleave",
                                            originalEvent: e,
                                            fileUpload: self
                                        });
                                    }
                                }, 1);
                            }

                            return false;
                        })
                        .on("drop", function(e) {
                            if (self._isActive()) {
                                if (e.stopPropagation) {
                                    e.stopPropagation();
                                }
                                if (e.preventDefault) {
                                    e.preventDefault();
                                }

                                var files = e.originalEvent.dataTransfer.files;

                                self.$element.trigger({
                                    type: "dropzonedrop",
                                    originalEvent: e,
                                    files: files,
                                    fileUpload: self
                                });

                                self._onFileSelectionChange(e, files);
                            }

                            return false;
                        })
                    ;
                }
            }
        },

        _registerEventHandler: function(name, handler) {
            this.$element.on(name, handler);
        },

        _createMissingElements: function() {
            var self = this;

            var multiple = self.options.useHTML5 && self.options.multiple;
            if (self.inputElement.length === 0) {
                self.inputElement = $("<input/>", {
                    type: "file",
                    name: self.options.name,
                    multiple: multiple
                });
                self.$element.prepend(self.inputElement);
            } else {
                self.inputElement.attr("multiple", multiple);
            }
        },

        /** @ignore */
        _readDataFromMarkup: function() {
            var self = this;
            if (this.inputElement.attr("name")) {
                this.options.name = this.inputElement.attr("name");
            }
            if (this.inputElement.attr("placeholder")) {
                this.options.placeholder = this.inputElement.attr("placeholder");
            }
            if (this.inputElement.attr("data-placeholder")) {
                this.options.placeholder = this.inputElement.attr("data-placeholder");
            }
            if (this.inputElement.attr("disabled") || this.inputElement.attr("data-disabled")) {
                this.options.disabled = true;
            }
            if (this.inputElement.attr("multiple") || this.inputElement.attr("data-multiple")) {
                this.options.multiple = true;
            }
            if (this.inputElement.attr("data-upload-url")) {
                this.options.uploadUrl = this.inputElement.attr("data-upload-url");
            }
            if (this.inputElement.attr("data-upload-url-builder")) {
                this.options.uploadUrlBuilder = CUI.util.buildFunction(this.inputElement.attr("data-upload-url-builder"), ["fileUpload"]);
            }
            if (this.inputElement.attr("data-size-limit")) {
                this.options.sizeLimit = this.inputElement.attr("data-size-limit");
            }
            if (this.inputElement.attr("data-auto-start")) {
                this.options.autoStart = true;
            }
            if (this.inputElement.attr("data-usehtml5")) {
                this.options.useHTML5 = this.inputElement.attr("data-usehtml5") === "true";
            }
            if (this.inputElement.attr("data-dropzone")) {
                this.options.dropZone = this.inputElement.attr("data-dropzone");
            }
            if (this.inputElement.attr("data-file-name-parameter")) {
                this.options.fileNameParameter = this.inputElement.attr("data-file-name-parameter");
            }
            $.each(this.inputElement.get(0).attributes, function(i, attribute) {
                var match = /^data-event-(.*)$/.exec(attribute.name);
                if (match && match.length > 1) {
                    var eventHandler = CUI.util.buildFunction(attribute.value, ["event"]);
                    if (eventHandler) {
                        self.options.events[match[1]] = eventHandler.bind(self);
                    }
                }
            });
        },

        /** @ignore */
        _update: function() {
            if (this.options.placeholder) {
                this.inputElement.attr("placeholder", this.options.placeholder);
            }

            if (this.options.disabled) {
                this.$element.addClass("disabled");
                this.inputElement.attr("disabled", "disabled");
            } else {
                this.$element.removeClass("disabled");
                this.inputElement.removeAttr("disabled");
            }
        },

        /** @ignore */
        _onFileSelectionChange: function(event, files) {
            var addedCount = 0, rejectedCount = 0;
            if (this.options.useHTML5) {
                files = files || event.target.files;
                for (var i = 0; i < files.length; i++) {
                    if (this._addFile(files[i])) {
                        addedCount++;
                    } else {
                        rejectedCount++;
                    }
                }
            } else {
                if (this._addFile(event.target)) {
                    addedCount++;
                } else {
                    rejectedCount++;
                }
            }

            this.$element.trigger({
                type: "filelistprocessed",
                addedCount: addedCount,
                rejectedCount: rejectedCount,
                fileUpload: this
            });
        },

        /** @ignore */
        _addFile: function(file) {
            var self = this;

            var fileName;
            if (this.options.useHTML5) {
                fileName = file.name;
            } else {
                fileName = $(file).attr("value");
            }
            if (fileName.lastIndexOf("\\") !== -1) {
                fileName = fileName.substring(fileName.lastIndexOf("\\") + 1);
            }

            if (!self._getQueueItemByFileName(fileName)) {
                var item = {
                    fileName: fileName
                };
                if (this.options.useHTML5) {
                    item.file = file;
                    item.fileSize = file.size;

                    // Check file size
                    if (self.options.sizeLimit && file.size > self.options.sizeLimit) {
                        self.$element.trigger({
                            type: "filerejected",
                            item: item,
                            message: "File is too big",
                            fileUpload: self
                        });
                        return false;
                    }
                }

                // Add item to queue
                self.uploadQueue.push(item);
                self.$element.trigger({
                    type: "queuechanged",
                    item: item,
                    operation: "ADD",
                    queueLength: self.uploadQueue.length,
                    fileUpload: self
                });

                self.$element.trigger({
                    type: "fileselected",
                    item: item,
                    fileUpload: self
                });

                return true;
            }

            return false;
        },

        /** @ignore */
        _getQueueIndex: function(fileName) {
            var index = -1;
            $.each(this.uploadQueue, function(i, item) {
                if (item.fileName === fileName) {
                    index = i;
                    return false;
                }
            });
            return index;
        },

        /** @ignore */
        _getQueueItem: function(index) {
            return index > -1 ? this.uploadQueue[index] : null;
        },

        /** @ignore */
        _getQueueItemByFileName: function(fileName) {
            return this._getQueueItem(this._getQueueIndex(fileName));
        },

        /**
         Upload a file item

         @param {Object} item                   Object representing a file item
         */
        uploadFile: function(item) {
            var self = this;

            if (self.options.useHTML5) {
                item.xhr = new XMLHttpRequest();
                item.xhr.addEventListener("loadstart", function(e) { self._onUploadStart(e, item); }, false);
                item.xhr.addEventListener("load", function(e) { self._onUploadLoad(e, item); }, false);
                item.xhr.addEventListener("error", function(e) { self._onUploadError(e, item); }, false);
                item.xhr.addEventListener("abort", function(e) { self._onUploadCanceled(e, item); }, false);

                var upload = item.xhr.upload;
                upload.addEventListener("progress", function(e) { self._onUploadProgress(e, item); }, false);

                // TODO: encoding of special characters in file names
                var file = item.file;
                var fileName = item.fileName;
                if (window.FormData) {
                    var f = new FormData();
                    if (self.options.fileNameParameter) {
                        // Custom file and file name parameter
                        f.append(self.inputElement.attr("name"), file);
                        f.append(self.options.fileNameParameter || "fileName", fileName);
                    } else {
                        f.append(fileName, file);
                    }
                    f.append("_charset_", "utf-8");

                    item.xhr.open("POST", self.options.uploadUrl + "?:ck=" + new Date().getTime(), true);
                    item.xhr.send(f);
                } else {
                    item.xhr.open("PUT", self.options.uploadUrl + "/" + fileName, true);
                    item.xhr.send(file);
                }

            } else {
                var $body = $(document.body);

                // Build an iframe
                var iframeName = "upload-" + new Date().getTime();
                var $iframe = $("<iframe/>", {
                    name: iframeName
                });
                $iframe.addClass("fileupload").appendTo($body);

                // Build a form
                var $form = $("<form/>", {
                    method: "post",
                    enctype: "multipart/form-data",
                    action: self.options.uploadUrl,
                    target: iframeName
                });
                $form.addClass("fileupload").appendTo($body);

                var $charset = $("<input/>", {
                    type: "hidden",
                    name: "_charset_",
                    value: "utf-8"
                });
                $form.prepend($charset);

                // Define value of the file name element
                if (this.options.fileNameParameter) {
                    this.fileNameElement = $("<input/>", {
                        type: "hidden",
                        name: this.options.fileNameParameter,
                        value: item.fileName
                    });
                    $form.prepend(this.fileNameElement);
                }

                $iframe.one("load", function() {
                    var content = this.contentWindow.document.body.innerHTML;
                    self.inputElement.prependTo(self.$spanElement);
                    $form.remove();
                    $iframe.remove();

                    self.$element.trigger({
                        type: "fileuploadload",
                        item: item,
                        content: content,
                        fileUpload: self
                    });
                });

                self.inputElement.prependTo($form);
                $form.submit();
            }
        },

        /**
         Cancel upload of a file item

         @param {Object} item                   Object representing a file item
         */
        cancelUpload: function(item) {
            item.xhr.abort();
        },

        /** @ignore */
        _onUploadStart: function(e, item) {
            this.$element.trigger({
                type: "fileuploadstart",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadProgress: function(e, item) {
            // Update progress bar
            this.$element.trigger({
                type: "fileuploadprogress",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadLoad: function(e, item) {
            var request = e.target;
            if (request.readyState === 4) {
                this._internalOnUploadLoad(e, item, request.status, request.responseText);
            }
        },

        /** @ignore */
        _internalOnUploadLoad: function(e, item, requestStatus, responseText) {
            if (CUI.util.HTTP.isOkStatus(requestStatus)) {
                this.$element.trigger({
                    type: "fileuploadsuccess",
                    item: item,
                    originalEvent: e,
                    fileUpload: this
                });
            } else {
                this.$element.trigger({
                    type: "fileuploaderror",
                    item: item,
                    originalEvent: e,
                    message: responseText,
                    fileUpload: this
                });
            }

            // Remove file name element if needed
            if (this.fileNameElement) {
                this.fileNameElement.remove();
            }

            // Remove queue item
            this.uploadQueue.splice(this._getQueueIndex(item.fileName), 1);
            this.$element.trigger({
                type: "queuechanged",
                item: item,
                operation: "REMOVE",
                queueLength: this.uploadQueue.length,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadError: function(e, item) {
            this.$element.trigger({
                type: "fileuploaderror",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _onUploadCanceled: function(e, item) {
            this.$element.trigger({
                type: "fileuploadcanceled",
                item: item,
                originalEvent: e,
                fileUpload: this
            });
        },

        /** @ignore */
        _isActive: function() {
            return !this.inputElement.is(':disabled');
        }

    });

    CUI.util.plugClass(CUI.FileUpload);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            $("[data-init~='fileupload']", e.target).fileUpload();
        });
    }

}(window.jQuery));

(function($) {
  CUI.Toolbar = new Class(/** @lends CUI.Toolbar# */{
    toString: 'Toolbar',

    extend: CUI.Widget,
    /**
     * @extends CUI.Widget
     * @classdesc 
     *    
     *  
     *     
     *  <h2>Data Attributes</h2>
     *  <h4>Currently there are the following data options:</h4>
     *  <pre>
     *    data-init="toolbar"
     *  </pre>
     */
    construct: function(options) {
      var $toolbar  = this.$element,
          $header   = $toolbar.closest(".content-header"),
          $icons    = $toolbar.find(".left"),
          hasCenter = $toolbar.find(".center").length !== 0,
          iconWidth = $icons.width();

      $toolbar.reflow({
        "break-lines": function ($toolbar, size) {
          return hasCenter && $toolbar.width()-2*iconWidth < 16*size.rem();
        },
        "long-title":  function ($toolbar, size) {
          return hasCenter && $toolbar.width()-2*iconWidth > 40*size.rem();
        }
      }, {
        "applyClassToElement": $header
      });
    }
  });

  CUI.util.plugClass(CUI.Toolbar);

  // Data API
  if (CUI.options.dataAPI) {
    $(document).on("cui-contentloaded.data-api", function(e) {
      $("[data-init~=toolbar]", e.target).toolbar();
    });
  }
}(window.jQuery));

(function($) {
    CUI.Tooltip = new Class(/** @lends CUI.Tooltip# */{
        toString: 'Tooltip',

        extend: CUI.Widget,
    /**
      @extends CUI.Widget
      @classdesc <p>A tooltip that can be attached to any other element and may be displayed immediately, on mouse over or only on API call.</p>
      <p>Please always have in mind that there are two elements to deal with: The tooltip HTML element itself, and a "target" element.
      The tooltip is bound to this "target" element. Only on mouseover (or touch) of the "target" element the tooltip is shown (in interactive mode).</p>
      <h3>Quicktip</h3>
      <p>There is also a special "quicktip" mode that gives you a quick way to define tooltips on any element: This element will turn into the target element
      of a newly and dynamically created tooltip (see example below). This "quicktip" even works for dynamically inserted elements.</p>
      <p>
      <button data-init="quicktip" data-quicktip-content="Maybe an error?">Quicktip</button>
      </p>
      @example
      <caption>Instantiate by data API</caption>
&lt;button id="my-button"&gt;My Button&lt;/button&gt;
&lt;span class="tooltip arrow-left" data-interactive="true" data-init="tooltip" data-target="#my-button"&gt;Tooltip content&lt;/span&gt;

Currently there are the following data options:
  data-init="tooltip"         Inits the tooltip widget after page load
  data-interactive            Set to "true" for interactive show/hide on mouseover/touch.
  data-target                 Give an CSS-Selector for defining the target element this tooltips targets at.
  arrow-* CSS classes         One of arrow-left, arrow-right, arrow-top, arrow-bottom to define the position of the tooltip
  type CSS classes            One of success, error, info, notice to define the type of the tooltip

      @example
      <caption>Instantiate by special "quicktip" data option</caption>
      <description>The "quicktip" options are a quick and convenient way of defining tooltips for your elements, even if they are dynamically injected into your page.</description>
&lt;button id="dynamic-button" data-init="quicktip" data-quicktip-content="This is a quicktip" data-quicktip-type="success" data-quicktip-arrow="bottom"&gt;
    Dynamic quicktip creation
&lt;/button&gt;

The quicktip data options are applied to the target element of the tooltip (see above for the behaviour of "target" elements). These are the options:
  data-init="quicktip"          Creates a new tooltip on mouseover/touch (="quicktip").
  data-quicktip-content         Defines a content for the dynamic tooltip. If this is not given, the html() of the element itself is used.
  data-quicktip-arrow           Defines the direction of the arrow of the new tooltip (and therefore the position)
  data-quicktip-type            One of "info", "success", "error", "notice"

      @example
      <caption>Instantiate by Class</caption>
      &lt;button id="dynamic-button"&gt;Dynamic tooltip creation&lt;/button&gt;
      &lt;script type="text/javascript"&gt;
          new CUI.Tooltip({target: "#dynamic-button",
                           content: "Dynamic tooltip",
                           interactive: true});
          // Note: No need for a "element" option here, just define the targeted element
      &lt;/script&gt;
     
      
      @desc Creates a new tooltip     
      @constructs

      @param {Object} options                       Component options
      @param {Mixed} [options.element]              jQuery selector or DOM element to use for tooltip.
      @param {Mixed} options.target                 jQuery selector or DOM element the tooltip is attached to
      @param {String} [options.content]             Content of the tooltip (HTML)
      @param {String} [options.type=info]           Type of dialog to display. One of info, error, notice or success
      @param {String} [options.arrow=left]          Where to place the arrow? One of left, right, top or bottom.
      @param {Integer} [options.delay=500]          Delay before an interactive tooltip is shown.
      @param {Integer} [options.distance=5]         Additional distance of tooltip from element.
      @param {Boolean} [options.visible=true]       True to display immediately, False to defer display until show() called
      @param {Boolean} [options.interactive=false]  True to display tooltip on mouse over, False to only show/hide it when show()/hide() is called manually
      @param {Boolean} [options.autoDestroy=false]  Automatically destroy tooltip on hide?
     */
        construct: function(options) {
            // Ensure we have an object, not only a selector
            if (this.options.target) this.options.target = $(this.options.target);

            if (this.$element.length === 0 && this.options.target) {
                // Special case: We do not have a element yet, but a target
                // -> let us create our own element
                this.$element = $("<div>");
                this.$element.insertAfter(this.options.target);
            }

            // Add tooltip class to give styling
            this.$element.addClass('tooltip');

            if (this.$element.data("interactive")) {
                this.options.interactive = true;
                if (!this.options.target) this.options.target = this.$element.parent();
            }
     
            if (this.$element.data("target")) {
                this.options.target = $(this.$element.data("target"));
            }

            if (!this.options.arrow) {
                this.options.arrow = "left"; // set some default
                if (this.$element.hasClass("arrow-left")) this.options.arrow = "left";
                if (this.$element.hasClass("arrow-right")) this.options.arrow = "right";
                if (this.$element.hasClass("arrow-top")) this.options.arrow = "top";
                if (this.$element.hasClass("arrow-bottom")) this.options.arrow = "bottom";
            }

            // Interactive Tooltips are never visible by default!
            if (this.options.interactive) this.options.visible = false;

            this.$element.toggleClass("hidden", !this.options.visible);

            // Listen to changes to configuration
            this.$element.on('change:content', this._setContent.bind(this));
            this.$element.on('change:type', this._setType.bind(this));
            this.$element.on('change:arrow', this._setArrow.bind(this));

            this.applyOptions();
            this.reposition();

            // Save this object also in the target element
            if (this.options.target) this.options.target.data("tooltip", this);

            if (this.options.interactive && this.options.target) {
                var hto = null;
                // Special behaviour on mobile: show tooltip on every touchstart
                $(this.options.target).finger("touchstart.cui-tooltip",function(event) {
                    if (hto) clearTimeout(hto);
                    this.show();
                    hto = setTimeout(function() {
                        this.hide();
                    }.bind(this), 3000); // Hide after 3 seconds
                }.bind(this));

                var showTimeout = false;
                $(this.options.target).pointer("mouseover.cui-tooltip", function(event) {
                    if (showTimeout) clearTimeout(showTimeout);
                    showTimeout = setTimeout(function() {
                        this.show();
                    }.bind(this), this.options.delay);
                }.bind(this));        

                $(this.options.target).pointer("mouseout.cui-tooltip", function(event) {
                    if (showTimeout) clearTimeout(showTimeout);
                    this.hide();
                }.bind(this));        
            }
          
        },

        defaults: {
          target: null,
          visible: true,
          type: 'default',
          interactive: false,
          arrow: null,
          delay: 500,
          distance: 5
        },

        _types: [
          'info',
          'error',
          'notice',
          'success'
        ],
        
        _arrows: [
          'arrow-left',
          'arrow-right',
          'arrow-top',
          'arrow-bottom'
        ],
        
        applyOptions: function() {
          this._setContent();
          this._setType();
          this._setArrow();
        },

        /** @ignore */
        _setType: function() {
            if (typeof this.options.type !== 'string' || this._types.indexOf(this.options.type) === -1) return;

            // Remove old type
            this.$element.removeClass(this._types.join(' '));

            // Add new type
            this.$element.addClass(this.options.type);

            // Re-positioning
            this.reposition();
        },
        
        /** @ignore */
        _setArrow: function() {
            if (typeof this.options.arrow !== 'string' || this._arrows.indexOf("arrow-" + this.options.arrow) === -1) return;

            // Remove old type
            this.$element.removeClass(this._arrows.join(' '));

            // Add new type
            this.$element.addClass("arrow-" + this.options.arrow);

            // Re-positioning
            this.reposition();
        },    

        /** @ignore */
        _setContent: function() {
            if (typeof this.options.content !== 'string') return;

            this.$element.html(this.options.content);

            // Re-positioning
            this.reposition();
        },

        
        /** @ignore */
        _show: function() {
            if (this.$element.hasClass("hidden")) {
                this.$element.removeClass('hidden');
                this.$element.css("display", "none");
            }
            this.$element.fadeIn();
            //this.reposition();
        },

        /** @ignore */
        _hide: function() {
            this.$element.fadeOut(400, function() {
              if (this.options.autoDestroy) {
                this.$element.remove();
                $(this.options.target).off(".cui-tooltip");
                $(this.options.target).data("tooltip", null);
              }         
            }.bind(this));
            return this;
        },

        /**
          Place tooltip on page

          @returns {CUI.Tooltip} this, chainable
         */
        reposition: function(withoutWorkaround) {
            if (!this.options.target) return;
            
            // Reposition a second time due to rendering errors with Chrome and IE
            if (!withoutWorkaround) setTimeout(function() {this.reposition(true);}.bind(this), 50);
            
            this.$element.detach().insertAfter(this.options.target);

            this.$element.css("position", "absolute");

            var el = $(this.options.target);
            var eWidth = el.outerWidth(true);
            var eHeight = el.outerHeight(true);
            var eLeft = el.position().left;
            var eTop = el.position().top;


            var width = this.$element.outerWidth(true);
            var height = this.$element.outerHeight(true);

            var left = 0;
            var top = 0;

            if (this.options.arrow === "left") {
                left =  eLeft + eWidth + this.options.distance;
                top = eTop + (eHeight - height) / 2;
            }
            if (this.options.arrow === "right") {
                left =  eLeft - width - this.options.distance;
                top = eTop + (eHeight - height) / 2;
            }
            if (this.options.arrow === "bottom") {
                left =  eLeft + (eWidth - width) / 2;
                top = eTop - height - this.options.distance;
            }
            if (this.options.arrow === "top") {
                left =  eLeft + (eWidth - width) / 2;
                top = eTop + eHeight + this.options.distance;
            }                  

            this.$element.css('left', left);
            this.$element.css('top', top);

            return this;
        }
    });

    CUI.util.plugClass(CUI.Tooltip);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            // Only initialize non-interactive tooltips this way!
            $("[data-init~=tooltip]", e.target).tooltip();
        });
        
        $(document).fipo("touchstart", "mouseover", "[data-init~=quicktip]", function(event) {
          var el = $(event.target);
          var tooltip = el.data("tooltip");
          if (!tooltip) {
            new CUI.Tooltip({
              target: el,
              content: el.data("quicktip-content") || el.html(),
              type: el.data("quicktip-type"),
              arrow: el.data("quicktip-arrow"),
              interactive: true,
              autoDestroy: true
            });
            el.trigger(event);
          }
        }.bind(this));  

    }
}(window.jQuery));

(function($) {
  var ns = "cui-draggable-list";
  var dropZoneClassName = "dropzone";
  
  /* Define some private helpers */
  function boundingBox(element) {
    return {l: element.offset().left,
            t: element.offset().top,
            w: element.outerWidth(),
            h: element.outerHeight()};
  }
  function within(x, y, element) {
    var bb = boundingBox(element);
    return (x >= bb.l && y >= bb.t && x < bb.l + bb.w && y < bb.t + bb.h);
  }
  function currentPagePosition(event) {
      var touch = {};
      if (event.originalEvent) {
        var o = event.originalEvent;
        if (o.changedTouches && o.changedTouches.length > 0) touch = event.originalEvent.changedTouches[0];        
        if (o.touches && o.touches.length > 0) touch = event.originalEvent.touches[0];
      }
      var x = touch.pageX || event.pageX;
      var y = touch.pageY || event.pageY;
      return {x: x, y: y};   
  }
  
  /**
     Internal helper class to perform the drag action.
  */
  var DragAction = new Class({
    /**
      Construct a new Drag Action. This class is internal for now.
      
      After the initialization the drag ist performed immediatly.
      
      @param {Event} event        The event that triggered the drag
      @param {jQuery} source      The element that is the source of this drag
      @param {jQuery} dragElement The element that will be dragged
      @param {Array} dropZones    An Array of elements that can be destinations for this drag 
    */
    construct: function(event, source, dragElement, dropZones, restrictAxis) {
      this.sourceElement = source;
      this.dragElement = dragElement;
      this.container = this._getViewContainer(dragElement);
      this.containerHeight = this.container.get(0).scrollHeight; // Save current container height before we start dragging
      this.dropZones = dropZones;
      this.axis = restrictAxis;
      this.scrollZone = 20; // Use 20px as scrolling zone, static for now
      this.dragStart(event);
    },
    currentDragOver: null,
    
    _getViewContainer: function(element) {
      // Search for the first parent that has a hidden/scrolling overflow
      while (true) {
        var p = element.parent();
        if (p.length === 0) return p;
        if (p.is("body")) return p;
        var flow = p.css("overflow");
        if (flow == "hidden" || flow == "auto" || flow == "scroll") return p;
        element = p;
      }
    },
    dragStart: function(event) {
      event.preventDefault();
      // Starting the drag
      var p = this.dragElement.position();

      this.dragElement.css({
          "left": p.left,
          "top": p.top,
          "width": this.dragElement.width() + "px"}
      );
      this.dragElement.addClass("dragging"); 
           
      var pp = currentPagePosition(event);
      var x = pp.x;
      var y = pp.y;
              
      this.dragStart = {x: x - p.left, y: y - p.top};
     
      // Bind event listeners
      $(document).fipo("touchmove." + ns, "mousemove." + ns, this.drag.bind(this));
      $(document).fipo("touchend." + ns, "mouseup." + ns, this.dragEnd.bind(this));  
       
      this.sourceElement.trigger(this._createEvent("dragstart", event));
      
      // Perform a first drag
      this.drag(event);
      
    },
    drag: function(event) {
      event.preventDefault();
      
      // Performing the drag
      var p = currentPagePosition(event);
      var x = p.x;
      var y = p.y;

      // Need to scroll?      
      if (this.container.is("body")) {
        if ((y - this.container.scrollTop()) < this.scrollZone) this.container.scrollTop(y - this.scrollZone);
        if ((y - this.container.scrollTop()) > (this.container.height() - this.scrollZone)) this.container.scrollTop(y - (this.container.height() - this.scrollZone));
      } else {
        var oldTop = this.container.scrollTop();
        var t = this.container.offset().top + this.scrollZone;
        if (y < t) {
          this.container.scrollTop(this.container.scrollTop() - (t - y));
        }
        var h = this.container.offset().top + this.container.height() - this.scrollZone;
        if (y > h) {
          var s = this.container.scrollTop() + (y - h);
          if (s > (this.containerHeight - this.container.height())) {
            s = Math.max(this.containerHeight - this.container.height(), 0);
          }
          this.container.scrollTop(s);
        }
        var newTop = this.container.scrollTop();
        this.dragStart.y += oldTop - newTop; // Correct drag start position after element scrolling
      }
      
    
      var newCss = {};
      if (this.axis != "horizontal") newCss["top"] = y - this.dragStart.y;
      if (this.axis != "vertical") newCss["left"] = x - this.dragStart.x;

      this.dragElement.css(newCss);
      
      this.triggerDrag(event);
    },
    dragEnd: function(event) {
      event.preventDefault();
      // Finishing a drag
      this.dragElement.removeClass("dragging");
      this.dragElement.css({top: "", left: "", width: ""});
              
      // Remove event handlers
      $(document).off("." + ns);
      
      // Trigger drop
      this.triggerDrop(event);
      
      // Trigger end events
      if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragleave", event));
      this.sourceElement.trigger(this._createEvent("dragend", event));     
    },
    triggerDrag: function(event) {
      var dropElement = this._getCurrentDropZone(event);
      if (dropElement != this.currentDragOver) {
        if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragleave", event));
        this.currentDragOver = dropElement;
        if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragenter", event));
      } else {
        if (this.currentDragOver != null) $(this.currentDragOver).trigger(this._createEvent("dragover", event));
      }      
    },
    triggerDrop: function(event) {
      var dropElement = this._getCurrentDropZone(event);
      if (dropElement == null) return;
      var dropEvent = this._createEvent("drop", event);
      dropElement.trigger(dropEvent);
    },
    _getCurrentDropZone: function(event) {
      var p = currentPagePosition(event);
      var dropElement = null;
      
      jQuery.each(this.dropZones, function(index, value) {
        if (!within(p.x, p.y, value)) return;
        dropElement = value;
      }.bind(this));
      
      return dropElement;     
    },
    _createEvent: function(name, fromEvent) {
      var p = currentPagePosition(fromEvent);
      var event = jQuery.Event(name);
      event.pageX = p.x;
      event.pageY = p.y;
      event.sourceElement = this.sourceElement;
      event.item = this.dragElement;
      return event;
    }
    
  });
  
  /* Global array of drop zones */
  var dropZones = [];
      
  CUI.DraggableList = new Class(/** @lends CUI.DraggableList# */{
    toString: 'DraggableList',

    extend: CUI.Widget,
    
    /**
     Triggered when a the position of an item in the list has been changed due to user sorting

     @name CUI.Sortable#reordered
     @event

     @param {Object} evt                    Event object
     @param {Object} evt.sourceElement      The DOM list element this event occured on
     @param {Object} evt.item               Object representing the moved item
     @param {Object} evt.oldIndex           The old position of the item in the list
     @param {Object} evt.newIndex           The new position of the item in the list
    */
    /**
     Triggered when an item has ben inserted from another list

     @name CUI.Sortable#inserted
     @event

     @param {Object} evt                    Event object
     @param {Object} evt.sourceElement      The DOM list element where the item came from
     @param {Object} evt.item               Object representing the inserted item
     @param {Object} evt.oldIndex           The old position of the item in the former list
     @param {Object} evt.newIndex           The new position of the item in the current list
    */
    /**
     Triggered when an items has been removed from the list by the user

     @name CUI.Sortable#removed
     @event

     @param {Object} evt                    Event object
     @param {Object} evt.item               Object representing the removed item
     @param {Object} evt.index              The former position of the item
    */              
    /**
      @extends CUI.Widget
      @classdesc Makes a list draggable. This also implies re-ordering of the list items on behalf of the user. This widget also
      defines some high-level events for inserting, reordering and removing items. The underlying drag/drop events should not be used outside this widget,
      as they are subject to change in the future.

      <h2 class="line">Examples</h2>

      @example
      <caption>A fully configured list</caption>
      &lt;div class="dropzone"&gt;
        &lt;ul class="draggable" data-init="draggable-list" data-allow="drag drop reorder" data-closeable="true"&gt;
        &lt;li&gt;Item 1&lt;/li&gt;
        &lt;li&gt;Item 2&lt;/li&gt;
        &lt;/ul&gt;
        &lt;p&gt;Drop here!&lt;/p&gt;
      &lt;/div&gt;
          
      
      @desc Creates a draggable/sortable list
      @constructs

      @param {Object} options                          Component options
      @param {Mixed} options.element                   jQuery selector or DOM element to use for sortable list. Has to be an <ul>
      @param {boolean} [options.allowReorder=false]    May the user reorder the list by drag&drop?     
      @param {boolean} [options.allowDrag=false]       May the user drag elements of the list to other lists?
      @param {boolean} [options.allowDrop=false]       May the user drop elements into this list?
      @param {boolean} [options.closable=false]        Can the user remove items from this list?                             
    */
    construct: function(options) {
      this.$element.addClass("draggable");
      
      if (this.$element.data("allow")) {
        var allow = this.$element.data("allow").split(" ");
        if (jQuery.inArray("reorder", allow) >= 0) this.options.allowReorder = true;
        if (jQuery.inArray("drag", allow) >= 0) this.options.allowDrag = true;
        if (jQuery.inArray("drop", allow) >= 0) this.options.allowDrop = true;
      }
      if (this.$element.data("closable")) this.options.closable = true;
                  
      this.$element.on("click", ".close", this.close.bind(this));      
      this.$element.fipo("taphold", "mousedown", "li", this.dragStart.bind(this));           

      this.dropZone = (this.$element.parent().is("." + dropZoneClassName)) ? this.$element.parent() : this.$element;

      this.$element.on("dragend", this.dragEnd.bind(this));

      // Register drop event handlers if dropping is allowed or reordering is allowed
      if (this.options.allowDrop || this.options.allowReorder) {
          this.dropZone.on("dragenter", this.dragEnter.bind(this));
          this.dropZone.on("dragover", this.dragOver.bind(this));
          this.dropZone.on("dragleave", this.dragLeave.bind(this));
          this.dropZone.on("drop", this.drop.bind(this));
      }
      
      // But out dropZone into the global array ONLY if dropping is allowed!    
      if (this.options.allowDrop) {
          dropZones.push(this.dropZone);
      }
      
      // Prevent browser from starting his own drag&drop chain
      this.$element.on("dragstart", function(event) {
        event.preventDefault();
      });
    },

    defaults: {
      allowReorder: false,
      allowDrag: false,
      allowDrop: false,
      closable: false
    },
    
    dropZone: null,
    
    dragStart: function(event) {
      if ($(event.target).hasClass("close")) return; // Don't drag on close button!
      event.preventDefault();

      //this.dragging = $(event.target).closest("li");
      
      var el = $(event.target).closest("li");
      el.prevAll().addClass("drag-before");
      el.nextAll().addClass("drag-after");

      // Fix height of list element to avoid flickering of page
      this.$element.css({height: this.$element.height() + $(event.item).height() + "px"});
      if (this.options.allowDrag) {
        new DragAction(event, this.$element, el, dropZones);
      } else {
        new DragAction(event, this.$element, el, [this.dropZone], "vertical");
      }

    },
    dragEnd: function(event) {
      this.$element.css({height: ""});
    },
    dragEnter: function(event) {
      this.dropZone.addClass("drag-over");
      if (this.options.allowReorder) {
        this.reorderPreview(event);
      }  
    },
    dragOver: function(event) {
      if (this.options.allowReorder) {
        this.reorderPreview(event);
      }
    },
    dragLeave: function(event) {
      this.dropZone.removeClass("drag-over");
      this.$element.children().removeClass("drag-before drag-after");      
    },
    drop: function(event) {
      this.$element.css({height: ""});
      if (this.$element.is(event.sourceElement) && this.options.allowReorder) {
        this.reorder(event, false); 
      }
      if (!this.$element.is(event.sourceElement) && this.options.allowDrop) {
        var e = $(event.item);
        
        if (this.options.closable && e.find(".close").length === 0) {
          e.append("<button class=\"close\">&times;</button>");
        } else if (!this.options.closable) {
          e.find(".close").remove();
        }
        
        if (this.options.allowReorder) {
          this.reorder(event, e);
        } else {
          this.$element.append(e);
        }
      }
      this.$element.children().removeClass("drag-before drag-after");
    },
    
    reorderPreview: function(event) {
      var p = currentPagePosition(event);
      var x = p.x;
      var y = p.y;
      var bb = boundingBox(this.$element);
      var that = this;
            
      if (x < bb.l || y < bb.t || x > bb.l + bb.w || y > bb.t + bb.h) {
        this.$element.children().removeClass("drag-after drag-before");
      } else {
        this.$element.children().each(function() {
          if ($(this).is(".dragging")) return;
          var bb = boundingBox($(this));
          var isAfter = (y < (bb.t + bb.h / 2));
          $(this).toggleClass("drag-after", isAfter);
          $(this).toggleClass("drag-before", !isAfter);        
        });
      }
                  
    },
    
    reorder: function(event, newItem) {
        var from = (newItem) ? newItem : $(event.item);
        var before = this.$element.children(".drag-after:first");
        var after = this.$element.children(".drag-before:last");

        var oldPosition = from.index();
        if (before.length > 0) from.insertBefore(before);
        if (after.length > 0) from.insertAfter(after);
        if (before.length === 0 && after.length === 0 && newItem) {
          this.$element.append(from);
        }
        var newPosition = from.index();

        if (oldPosition != newPosition || newItem) {
          var e = jQuery.Event((newItem) ? "inserted" : "reordered");
          e.sourceElement = event.sourceElement;
          e.oldIndex = oldPosition;
          e.newIndex = newPosition;
          e.item = from.get(0);
          this.$element.trigger(e);
          return true;
        }
        return false;
    },
    close: function(event) {
      if (!this.options.closable) return;
      event.preventDefault();
      var e = $(event.target).closest("li");
      var index = e.index();
      e.remove();
      var ev = jQuery.Event("removed");
      ev.sourceElement = this.$element.get(0);
      ev.index = index;
      ev.item = e.get(0);
      this.$element.trigger(ev);
    }
           
  });

  // jQuery plugin
  CUI.util.plugClass(CUI.DraggableList);

  if (CUI.options.dataAPI) {
      $(document).on('cui-contentloaded.data-api', function() {
        $("[data-init~=draggable-list]").draggableList();
      });
  }
}(window.jQuery));

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



(function($) {
  CUI.Accordion = new Class(/** @lends CUI.Accordion# */{
    toString: 'Accordion',
    extend: CUI.Widget,
    
    /**
      @extends CUI.Widget
      @classdesc A widget for both accordions and collapsibles
      
            <ul data-init="accordion">
              <li>
                <h3>
                  <em>Tab 1</em> of the accordion
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
              </li>
              <li class="active">
                <h3>
                  <em>Tab 2</em> of the accordion
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
              </li>
              <li>
                <h3>
                  <em>Tab 3</em> of the accordion
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
              </li>                             
            </ul>
            
            <div data-init="collapsible">
                <h3>
                  <em>Headline</em> of the collapsible
                </h3>
                Chocolate cake jelly cupcake dessert. Chocolate bar lemon drops croissant candy canes caramels jelly beans lemon drops pie wypas. Faworki donut macaroon jujubes brownie cupcake topping macaroon gummies. Halvah gummi bears brownie chupa chups. Fruitcake lemon drops gingerbread cake apple pie. Pastry carrot cake pie. Brownie oat cake gummies. Bonbon soufflé jujubes soufflé biscuit. Chocolate cake halvah chocolate cake candy canes powder chocolate croissant. Lollipop fruitcake muffin chocolate cake apple pie bear claw cookie tootsie roll. Chupa chups dessert icing gummies cake jelly pie sesame snaps. Wafer halvah cake sweet.
            </div>
                  
      @example
      <caption>Instantiate Accordion with data API</caption>
      <description>This is the most convenient way to instantiate an accordion, as an accordion in any case needs some markup to be initialized. Give the "active" class to the element that should be opened by default.</description>
&lt;ul data-init="accordion"&gt;
  &lt;li&gt;
    &lt;h3&gt;
      &lt;em&gt;Tab 1&lt;/em&gt; of the accordion
    &lt;/h3&gt;
    ...
  &lt;/li&gt;
  &lt;li class="active"&gt;
    &lt;h3&gt;
      &lt;em&gt;Tab 2&lt;/em&gt; of the accordion
    &lt;/h3&gt;
    ...
  &lt;/li&gt;
&lt;/ul&gt;

      @example
      <caption>Instantiate Collapsible with data API</caption>
      <description>Add the "active" class to open the collapsible by default.</description>
&lt;div data-init="collapsible"&gt;
  &lt;h3&gt;
    &lt;em&gt;Headline&lt;/em&gt; of the collapsible
  &lt;/h3&gt;
  ...
&lt;/div&gt;

      @example
      <caption>Instantiate Accordion with jQuery plugin</caption>
      <description>The widget will try to guess if it is a collapsible instead of accordion by testing if the CSS class "collapsible" is present.</description>
  $("#accordion").accordion({active: 2}); // Set active tab
  $("#collapsible").accordion({active: true}); // Set collapsible to "opened" (=active)
          
      @desc Creates a new accordion or collapsible
      @constructs

      @param {Object} options                               Widget options.
      @param {Mixed}  [options.active=false]                Index of the initial active tab of the accordion or one of true/false for collapsibles
            
    */
    construct: function(options) {
      this.isAccordion = (!this.$element.hasClass("collapsible")) && (this.$element.data("init") !== "collapsible");
    
      if (this.isAccordion) this.$element.addClass("accordion");
    
      if (this.isAccordion) {
        var activeIndex = this.$element.children(".active").index();
        if (this.options.active !== false) activeIndex = this.options.active;
        this.$element.children().each(function(index, element) {
          this._initElement(element, index != activeIndex);
        }.bind(this));
      } else {
        this._initElement(this.$element, !(this.options.active || this.$element.hasClass("active")));
      }
      
      this.$element.on("click", "h3", this._toggle.bind(this));
      
      this.$element.on("change:active", this._changeActive.bind(this));
      
      // Prevent text selection on header
      this.$element.on("selectstart", "h3", function(event) {
        event.preventDefault();
      });
      
      this._makeAccessible();
    },
    
    defaults: {
      active: false
    },
    
    isAccordion: false,
    
    _toggle: function(event) {
      var el = $(event.target).closest(".collapsible"),
          isCurrentlyActive = el.hasClass("active"),
          active = (isCurrentlyActive) ? false : ((this.isAccordion) ? el.index() : true); 
      this.setActive(active);
    },
    _changeActive: function() {
      if (this.isAccordion) {
        this._collapse(this.$element.children(".active"));
        if (this.options.active !== false) {
          var activeElement = this.$element.children().eq(this.options.active);
          this._expand(activeElement);
        }
      } else {
        if (this.options.active) {
          this._expand(this.$element);
        } else {
          this._collapse(this.$element);
        }
      }      
    },
    setActive: function(active) {
      this.options.active = active;
      this._changeActive();    
    },
    _initElement: function(element, collapse) {
        // Add correct header
        if ($(element).find("h3").length === 0) $(element).prepend("<h3>&nbsp;</h3>");
        if ($(element).find("h3 i").length === 0) $(element).find("h3").prepend("<i></i>&nbsp;");
        
        $(element).addClass("collapsible");
        
        var head = $(element).find("h3"),
            fold = $("<div></div>").toggle(!collapse),
            icon = head.find("i");
        
        // wrap the collapsible content in a div so that we can identify 
        // the relationship between the heading and the content it controls
        $(element).wrapInner(fold);
        
        // move the heading before the collapsible content
        head.prependTo(element);
        
        // Set correct initial state
        if (collapse) {
          $(element).removeClass("active");
          $(element).height(head.height());
          icon.removeClass("icon-accordiondown").addClass("icon-accordionup");
        } else {
          $(element).addClass("active");
          $(element).css("height", "auto");
          icon.removeClass("icon-accordionup").addClass("icon-accordiondown");
        }    
    },
    _collapse: function(el) {
         el.find("h3 i").removeClass("icon-accordiondown").addClass("icon-accordionup");
         el.animate({height: el.find("h3").height()}, "fast", function() {
            el.removeClass("active"); // remove the active class after animation so that background color doesn't change during animation
            el.find("div[aria-expanded]").hide(); // After animation we want to hide the collapsed content so that it cannot be focused
         });
         
         // update WAI-ARIA accessibility properties
         var head = el.find("h3"),
             fold = head.next("div[aria-expanded]");
         if (this.isAccordion) {
             head.attr({
                 "tabindex": head.is(document.activeElement) ? 0 : -1,
                 "aria-selected": false 
             });
         } else {
            head.attr({
                 "aria-expanded": false 
             });
         }
         fold.attr({
             "aria-hidden": true,
             "aria-expanded": false 
         });
    },
    _expand: function(el) {
         el.addClass("active");
         el.find("h3 i").removeClass("icon-accordionup").addClass("icon-accordiondown");
         var h = this._calcHeight(el);
            
         el.animate({height: h}, "fast", function() {
           el.css("height", "auto"); // After animation we want the element to adjust its height automatically
         });
         
         // update WAI-ARIA accessibility properties
         var head = el.find("h3"),
             fold = head.next("div[aria-expanded]");
         if (this.isAccordion) {
             head.attr({
                 "tabindex": 0,
                 "aria-selected": true 
             });
         } else {
            head.attr({
                 "aria-expanded": true 
             });
         }
         fold.attr({
             "aria-hidden": false,
             "aria-expanded": true 
         }).show();
    },
    /** @ignore */
    _calcHeight: function(el) {
      // Dimension calculation of invisible elements is not trivial.
      // "Best practice": Clone it, put it somwhere on the page, but not in the viewport,
      // and make it visible. 
      var el2 = $(el).clone(),
          fold2 = el2.find('div[aria-expanded]');
      fold2.show();
      el2.css({display: "block",
               position: "absolute",
               top: "-10000px",
               width: el.width(), // Ensure we calculate with the same width as before
               height: "auto"});
      $("body").append(el2);
      var h = el2.height();
      el2.remove();
      return h;
    },
    /**
     * adds accessibility attributes and features
     * per the WAI-ARIA Accordion widget design pattern: 
     * http://www.w3.org/WAI/PF/aria-practices/#accordion 
     * @private
     */
    _makeAccessible: function() {
        var idPrefix = 'accordion-' + new Date().getTime() + '-',
            section, head, fold, isActive, panelId;
        if (this.isAccordion) {
            
            this.$element.attr({
                "role": "tablist" // accordion container has the role="tablist"
            });
            
            this.$element.children(".collapsible").each(function (i, e) {
                var section =  $(e),
                    head = section.find("h3:first"),
                    isActive = section.hasClass("active"),
                    panelId = idPrefix + 'panel-' + i,
                    fold = head.next("div");
                
                section.attr({
                    "role": "presentation" // collapsible containers have the role="presentation" 
                });
                
                head.attr({
                    "role": "tab", // accordion headers should have the role="tab"
                    "id": head.attr("id") || idPrefix + "tab-" + i, // each tab needs an id
                    "aria-controls": panelId, // the id for the content wrapper this header controls
                    "aria-selected": isActive, // an indication of the current state
                    "tabindex": (isActive ? 0 : -1)
                });

                fold.attr({
                    "role": "tabpanel", // the content wrapper should have the role="tabpanel"
                    "id": panelId, // each content wrapper needs a unique id
                    "aria-labelledby": head.attr("id"), // the content wrapper is labelled by its header
                    "aria-expanded": isActive, // an indication of the current state
                    "aria-hidden": !isActive // hide/show content to assistive technology
                });
            });
            
        } else {
            idPrefix = 'collapsible-' + new Date().getTime() + '-';
            section =  this.$element;
            head = section.find("h3:first");
            isActive = section.hasClass("active");
            panelId = idPrefix + 'panel';
            fold = head.next("div");
                
            head.attr({
                "role": "button", // the header should have the role="button"
                "id": head.attr("id") || idPrefix + "heading", // each header needs an id
                "aria-controls": panelId, // the id for the content wrapper this header controls
                "aria-expanded": isActive, // an indication of the current state
                "tabindex": 0
            });

            fold.attr({
                "id": panelId, // each content wrapper needs a unique id
                "aria-labelledby": head.attr("id"), // the content wrapper is labelled by its header
                "aria-expanded": isActive, // an indication of the current state
                "aria-hidden": !isActive // hide/show content to assistive technology
            });
        }
        
        // handle keydown events from focusable descendants
        this.$element.on('keydown', ':focusable', this._onKeyDown.bind(this));
        
        // handle focusin/focusout events from focusable descendants
        this.$element.on('focusin.accordion', ':focusable', this._onFocusIn.bind(this));
        this.$element.on('focusout.accordion', 'h3:focusable', this._onFocusOut.bind(this));
        
        this.$element.on('touchstart.accordion, mousedown.accordion', 'h3:focusable', this._onMouseDown.bind(this));
    },
    /**
     * keydown event handler, which defines the keyboard behavior of the accordion control
     * per the WAI-ARIA Accordion widget design pattern: 
     * http://www.w3.org/WAI/PF/aria-practices/#accordion
     * @private
     */   
    _onKeyDown: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head),
            keymatch = true;
                    
        switch (event.which) {
            case 13: //enter
            case 32: //space
                if (isHead) {
                    head.trigger('click');
                } else {
                    keymatch = false;
                }
                break;
            case 33: //page up
            case 37: //left arrow
            case 38: //up arrow
                if ((isHead && this.isAccordion) || (event.which === 33 && (event.metaKey || event.ctrlKey))) {
                    // If the event.target is an accordion heading, or the key command is CTRL + PAGE_UP,
                    // focus the previous accordion heading, or if none exists, focus the last accordion heading.
                    if (el.prev().find("h3:first").focus().length === 0) {
                        this.$element.find(".collapsible:last h3:first").focus();
                    } 
                } else if (!isHead && (event.metaKey || event.ctrlKey)) {
                    // If the event.target is not a collapsible heading, 
                    // and the key command is CTRL + UP or CTRL + LEFT, focus the collapsible heading.
                    head.focus();
                } else {
                   keymatch = false; 
                }
                break;
            case 34: //page down
            case 39: //right arrow 
            case 40: //down arrow
                if (isHead && this.isAccordion) {
                    // If the event.target is an accordion heading,
                    // focus the next accordion heading, or if none exists, focus the first accordion heading.
                    if (el.next().find("h3:first").focus().length === 0) {
                        this.$element.find(".collapsible:first h3:first").focus();
                    } 
                } else if (!isHead && event.which === 34 && (event.metaKey || event.ctrlKey)) {
                    // If the event.target is not a collapsible heading, 
                    // and the key command is CTRL + PAGE_DOWN, focus the collapsible heading.
                    head.focus();
                } else {
                   keymatch = false; 
                }
                break;
            case 36: //home
                if (isHead && this.isAccordion) {
                    this.$element.find(".collapsible:first h3:first").focus();
                } else {
                   keymatch = false; 
                }
                break;
            case 35: //end
                if (isHead && this.isAccordion) {
                    this.$element.find(".collapsible:last h3:first").focus();
                } else {
                   keymatch = false; 
                }
                break;
            default:
                keymatch = false;
                break;
        }
        
        if (keymatch === true) {
            event.preventDefault();
        }
    },
    /**
     * focusin event handler, used to update tabindex properties on accordion headers
     * and to display focus style on headers.
     * @private
     */
    _onFocusIn: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head);            
        if (isHead) {
            if (this.isAccordion) {
                this.$element.find(".collapsible h3[role=tab]").attr('tabindex', -1);
            }
            if (!head.data('collapsible-mousedown')) {
                el.addClass('focus');
            } else {
                head.removeData('collapsible-mousedown');
            }
        }
        head.attr('tabindex', 0);
    },
    /**
     * focusout event handler, used to clear the focus style on headers.
     * @private
     */
    _onFocusOut: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head);            
        if (isHead) {
            el.removeClass('focus').removeData('collapsible-mousedown');
        }
    },
    /**
     * mousedown event handler, used flag 
     * @private
     */
    _onMouseDown: function(event) {
        var el = $(event.currentTarget).closest(".collapsible"),
            head = el.find('h3:first'),
            isHead = $(event.currentTarget).is(head);            
        if (isHead) {
            head.data('collapsible-mousedown', true);
        }
    }
  });

  CUI.util.plugClass(CUI.Accordion);
  
  // Data API
  $(document).on("cui-contentloaded.data-api", function(e) {
    $("[data-init~=accordion],[data-init~=collapsible]").accordion();
  });
}(window.jQuery));



(function($) {
  CUI.Tour = new Class(/** @lends CUI.Tour# */{
    toString: 'Tour',

    extend: CUI.Widget,
    /**
      @extends CUI.Widget
      @classdesc A tour which allows to explain the application

      <h2 class="line">Example</h2>
      <button onclick="$('#myTour').tour('show')" class="primary">Test the tour</button>
      
      <div id="myTour" class="tour" data-init="tour">
        <div class="tour-slide active">
            <section>
                <h1>Technology Preview</h1>
                <p>
                    Welcome to the new Touch optimized page editor !<br/>
                    Your feedback is important
                </p>
            </section>
            <img src="http://lorempixel.com/852/396/?img=1">
        </div>
        <div class="tour-slide">
            <section>
                <h1>Technology Preview</h1>
                <p>
                    Welcome to the new Touch optimized page editor !<br/>
                    Your feedback is important
                </p>
            </section>
            <img src="http://lorempixel.com/852/396/?img=2">
        </div>
        <div class="tour-slide">
            <section>
                <h1>Technology Preview</h1>
                <p>
                    Welcome to the new Touch optimized page editor !<br/>
                    Your feedback is important
                </p>
            </section>
            <img src="http://lorempixel.com/852/396/?img=3">
        </div>
        <nav>
            <button class="skip">Skip into</button>
            <button class="done primary">OK</button>
            <a href="#" class="prev icon-chevronleft medium">Previous</a>
            <a href="#" class="next icon-chevronright medium">Next</a> 
            <div class="control">
                <a href="#"></a>
                <a href="#"></a>
                <a href="#"></a>
            </div>
        </nav>
      </div>

      @example
      <caption>Instantiate with Class</caption>
      var modal = new CUI.Tour({
        element: '#myTour'
      });

      @example
      <caption>Instantiate with jQuery</caption>
      $('#myTour').tour();

      // open the tour
      $('#myTour').tour('show');

      @desc Creates a new tour
      @constructs

      @param {Object} options Component options
      @param {Boolean} [options.autoshow=false]   True to open the dialog immediately after initialization
      @param {Mixed} [options.backdrop=static]    False to not display transparent underlay, True to display and close when clicked, 'static' to display and not close when clicked
     */
    construct: function (options) {
      var idx;

      this.$slides = this.$element.find('.tour-slide');
      this.$navigation = this.$element.find('nav');
      this.$skip = this.$navigation.find('button.skip');
      this.$done = this.$navigation.find('button.done');
      this.$prev = this.$navigation.find('a.prev');
      this.$next = this.$navigation.find('a.next');
      this.$control = this.$navigation.find('.control');

      // set current slide
      this.$current = this.$element.find('.tour-slide.active');
      this.$current = this.$current.length > 0 ? this.$current : this.$element.find('.tour-slide:eq(0)').addClass('active'); // if no slide is selected set first

      // set current state in nav
      this._setCircleNav(this.$current);
      this._toggleButtons(this.$current);

      // bind the handlers
      this._bindControls();

      if (this.$element.hasClass('show')) {
        this.options.autoshow = true;
      }

      if (this.options.autoshow) {
        this.show();
      }
    },

    defaults: {
      autoshow: false,
      backdrop: 'static'
    },

    /** @ignore */
    _toggleButtons: function ($slide) {
      //reset
      this.$skip.removeClass('show');
      this.$done.removeClass('show');
      this.$prev.removeClass('hide');
      this.$next.removeClass('hide');

      if ($slide.hasClass('first')) {
        this.$skip.addClass('show');
        this.$prev.addClass('hide');

      } else if ($slide.hasClass('last') || $slide.hasClass('skipper')) {
        this.$done.addClass('show');
        this.$next.addClass('hide');
      }
    },

    /** @ignore */
    _setCircleNav: function ($slide) {
      var idx = $slide.index();

      if ($slide.hasClass('skipper')) {
        this.$control
         .removeClass('show')
         .addClass('hide');
      } else {
        this.$control
          .removeClass('hide')
          .addClass('show')
          .find('a')
          .removeClass('active')
          .filter(':eq('+ idx +')')
          .addClass('active');
      }
    },

    /** @ignore */
    _slideTo: function (sl) {
      var $slide = $(sl);

      if ($slide.length > 0) {
        this.$current.find('.fadable').addClass('faded');
        this.$current.removeClass('active');
        this.$current = $slide.addClass('active');

        this._setCircleNav($slide);
        this._toggleButtons($slide);

        this._stopLastAnimation();
        this._startNewAnimation();
      }
    },

    /**
     * slides to the next slide
     * @return {this}
     */
    slideToNext: function () {
      var next = this.$current.next('.tour-slide');
      if (! this.$current.hasClass('last')) {
        this._slideTo(next);
      }

      return this;
    },

    /**
     * slides to the previous slide
     * @return {this}
     */
    slideToPrev: function () {
      var prev = this.$current.hasClass('skipper') ?
          this.$element.find('.first') :
          this.$current.prev('.tour-slide');

      this._slideTo(prev);

      return this;
    },

    /**
     * slides to the skipper (hidden) slide
     * @return {this}
     */
    slideToSkipper: function () {
      var skipper = this.$element.find('.skipper');
      this._slideTo(skipper);

      return this;
    },

    /**
     * slides to the first slide
     * @return {this}
     */
    slideToFirst: function () {
      var first = this.$element.find('.first');
      this._slideTo(first);

      return this;
    },

    /**
     * slides to the given index of a slide
     * @param  {integer} no
     * @return {this}
     */
    slideTo: function (no) {
      this._slideTo(this.$slides[no]);

      return this;
    },

    /** @ignore */
    _bindControls: function () {
      // disable all anchors
      this.$navigation.on('click', 'a', function (event) {
        event.preventDefault();
      });

      this.$skip.fipo('tap', 'click', this.slideToSkipper.bind(this));
      this.$done.fipo('tap', 'click', this.hide.bind(this));
      this.$prev.fipo('tap', 'click', this.slideToPrev.bind(this));
      this.$next.fipo('tap', 'click', this.slideToNext.bind(this));
      this.$control.fipo('tap', 'click', 'a', function (event) {
        this.slideTo($(event.currentTarget).index());
      }.bind(this));

      this.$element.finger('swipe', '.tour-slide', function (event) {
        if (event.finger === 1) {
          if (event.direction === 'left') {
            this.slideToNext();
          } else if (event.direction === 'right') {
            this.slideToPrev();
          }
        }
      }.bind(this));
    },

    /** @ignore */
    _startNewAnimation: function () {
      var $imageToFade = this.$current.find('.fadable');
      if ($imageToFade) {
        this._animationTimer = setInterval(function () {
          $imageToFade.toggleClass('faded');
        }, 5000);
      }
    },

    /** @ignore */
    _stopLastAnimation: function () {
     clearInterval(this._animationTimer);
    },

      /** @ignore */
    _show: function () {
      this.$element.addClass('show');
      this.slideToFirst();
      this._toggleBackdrop(true);
      this._startNewAnimation();
    },

    /** @ignore */
    _hide: function () {
      this.$element.removeClass('show');
      this._toggleBackdrop();
      this._stopLastAnimation();
    },

    /** @ignore */
    _removeBackdrop: function () {
        if (this.$backdrop && !this.get('visible')) {
          // Remove from the DOM
          this.$backdrop.remove();
          this.$backdrop = null;
        }
    },

    /** @ignore */
    _toggleBackdrop: function (show) {
      if (show && this.options.backdrop) {
        if (this.$backdrop)
          this.$backdrop.fadeIn();
        else {
          this.$backdrop = $('<div/>', {
            "class": 'tour-backdrop',
            "css": {
              display: 'none'
            }
          }).appendTo(document.body);

          // start async to have a fadein effect
          setTimeout(function () {
            this.$backdrop.fadeIn();
          }.bind(this), 50);

          // Note: If this option is changed before the fade completes, it won't apply
          if (this.options.backdrop !== 'static') {
            this.$backdrop.click(this.hide);
          }
        }
      }
      else if (!show && this.$backdrop) {
        this.$backdrop.fadeOut(function () {
          this._removeBackdrop();
        }.bind(this));
      }
    }

  });

  CUI.util.plugClass(CUI.Tour);

  // Data API
  if (CUI.options.dataAPI) {

    $(document).on("cui-contentloaded.data-api", function (event) {
      $("[data-init=tour]", event.target).tour();
    });

  }
}(window.jQuery));

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
(function($) {
    
    var COLOPICKER_FOOTER_TEMPLATE = "<div class=\"button-group navigation-bar\"></div>";
    var CLASSIC_PALETTE_BUTTON = "<button id='classicButton'><i class=\"icon-viewgrid\"></i></button>";
    var EDIT_BUTTON = "<button id='editButton'><i class=\"icon-edit\"></i></button>";
    var COLOR_SLIDER_BUTTON = "<button><i class=\"icon-properties\"></i></button>";
    var EDIT_MODE = "editMode";
    var CLASSIC_MODE = "classicMode";

    CUI.Colorpicker = new Class(
            /** @lends CUI.Colorpicker# */
            {
                toString : 'Colorpicker',

                extend : CUI.Widget,

                defaults : {
                    config : {
                        colors : {},
                        displayModes : {}
                    },
                    disabled : false,
                    name : null,
                    title : ""
                },

                palettePageSize : 3,
                colorShadeNo : 6,
                lowerLimit : 0,
                upperLimit : 0,
                colorNames : [],
                currentPage : 0,
                pages : 1,

                /**
                 * @extends CUI.Widget
                 * @classdesc Colorpicker will create markup after the template.
                 * 
                 * @desc Creates a new colorpicker
                 * @constructs
                 */
                construct : function(options) {
                    this._readDataFromMarkup();
                    this._adjustMarkup();

                    if (this.options.config === null ||
                            this.options.config.colors.length === 0) {
                        this.options.disabled = true;
                    }
                    if (!this.options.disabled &&
                            (this.options.config.displayModes.classicPalette && this.options.config.displayModes.freestylePalette)) {
                        this.options.disabled = true;
                    }
                    if (!this.options.disabled &&
                            (this.options.config.displayModes.length === 0 || (!this.options.config.displayModes.classicPalette && !this.options.config.displayModes.freestylePalette))) {
                        this.options.config.displayModes.classicPalette = true;
                    }

                    this.$openButton = this.$element
                            .find('input.colorpicker-launcher');
                    this.$hiddenInput = this.$element.find("input[name='" +
                            this.options.name + "']");
                    
                    if (this.$element.attr("value")) {
                        var initialVal = this.$element.attr("value");
                        if(CUI.util.color.isValid("rgba", initialVal) || CUI.util.color.isValid("rgb", initialVal)){
                            this._setColor(initialVal);
                        }else{
                            this.$element.removeAttr("value");
                        }
                        
                    }

                    if (this.options.disabled) {
                        this.$element.find(">input").attr("disabled",
                                "disabled");
                    } else {
                        this.colorNames = [];
                        $.each(this.options.config.colors,
                                function(key, value) {
                                    this.colorNames.push(key);
                                }.bind(this));
                        $('body').off(
                                "tap." + this.options.name + " click." +
                                        this.options.name).fipo(
                                "tap." + this.options.name,
                                "click." + this.options.name, function(event) {
                                    if (!this.keepShown) {
                                        if (this.$element.find(".popover").has(event.target).length === 0)
                                        {
                                            this._hidePicker();
                                        }
                                    }
                                }.bind(this));

                        this.$openButton.on("click", function(event) {
                            try {
                                if (!this.pickerShown) {
                                    this._openPicker();
                                } else {
                                    this._hidePicker();
                                }
                                this.keepShown = true;
                                setTimeout(function() {
                                    this.keepShown = false;
                                }.bind(this), 200);
                            } catch (e) {
//                                console.log(e.message);
                            }

                        }.bind(this));
                    }

                },

                _readDataFromMarkup : function() {

                    if (this.$element.data("disabled")) {
                        this.options.disabled = true;
                    }

                    if (this.$element.data("name")) {
                        this.options.name = this.$element.data("name");
                    }
                    
                    if (this.$element.attr("title")) {
                        this.options.title = this.$element.attr("title");
                    }

                    var el = this.$element;
                    if (el.data('config') !== undefined) {
                        this.options.config = {};
                        this.options.config.colors = {};
                        this.options.config.displayModes = {};
                        if (el.data('config').colors) {
                            this.options.config.colors = el.data('config').colors;
                        } else {
                            this.options.disabled = true;
                        }
                        if (el.data('config').pickerModes) {
                            this.options.config.displayModes = el.data('config').pickerModes;
                        }
                    }
                },

                _adjustMarkup : function() {
                    this.$element.addClass("colorpicker");

                    if (this.$element.find(".popover").length === 0) {
                        this.$element
                                .append('<div class="popover arrow-top" style="display:none"><div class="inner"></div></div>');
                        this.$element
                                .find(".inner")
                                .append(
                                        '<div class="colorpicker-holder"><div class="palette-header"></div><div class="colorpicker-body"></div><div class="colorpicker-footer"></div></div>');
                    }

                    if (this.$element.find("input[type=hidden]").length === 0) {
                        this.$element.append("<input type=\"hidden\" name=\"" +
                                this.options.name + "\">");
                    }

                    var $button = this.$element
                            .find('input.colorpicker-launcher');
                    if ($button.attr('type') === undefined) {
                        $button.attr('type', 'button');
                    }
                },

                _openPicker : function() {

                    this._renderPicker(CLASSIC_MODE);
                    //take into consideration the popover border width
                    var left = this.$openButton.position().left +
                            this.$openButton.width() / 2 -
                            (this.$element.find(".popover").width() / 2 + 9);
                    if (left < 0) {
                        left = 0;
                    }
                    var top = this.$openButton.position().top +
                            this.$openButton.outerHeight() + 14;

                    this._renderPickerFooter();

                    this.$element.find(".popover").css({
                        "position" : "absolute",
                        "left" : left + "px",
                        "top" : top + "px"
                    }).show();
                    this.pickerShown = true;
                },

                _hidePicker : function() {
                    this.lowerLimit = 0;
                    this.upperLimit = 0;
                    this.currentPage = 0;
                    this.$element.removeClass("focus");
                    this.$element.find(".popover").hide();
                    this.pickerShown = false;
                },

                //render color picker based on the palette mode
                _renderPicker : function(mode, slide, pageNo) {

                    if (mode === CLASSIC_MODE && !this._calculatePaletteBoundaries(slide, pageNo)) {
                        return;
                    }

                    var table = null;
                    if (mode === CLASSIC_MODE){
                        table = this._renderPalette();
                    }else{
                        table = this._renderEditPalette();
                    }

                    var $picker = this.$element.find(".colorpicker-holder");
                    var $palette_nav = $picker.find(".palette-navigator");
                    var $picker_body = $picker.find(".colorpicker-body");

                    if (slide && $picker.find("table").length > 0) {
                        this._slidePicker(table, (slide === "left"));
                    } else {
                      //display selected color if any and selected page
                        $picker.find("table").remove();
                        $picker.find(".sliding-container").remove();
                        if (mode === EDIT_MODE) {
                            $picker_body.append(table);
                            $palette_nav.remove();
                            if (this.$hiddenInput.val() !== undefined && this.$hiddenInput.val().length > 0){
                                table.find("div.color").css("background", this.$hiddenInput.val());
                                var hex = CUI.util.color.RGBAToHex(this.$hiddenInput.val());
                                table.find("input[name=':hex']").val(hex);
                                var rgb = CUI.util.color.HexToRGB(hex);
                                this._fillRGBFields(rgb);
                                var cmyk = CUI.util.color.RGBtoCMYK(rgb);
                                this._fillCMYKFields(cmyk);
                            }
                        } else {
                            if ($palette_nav.length > 0){
                                $palette_nav.before(table);
                            }else{
                                $picker_body.append(table);
                                this._renderPaletteNavigation();
                            }
                            
                        }
                           
                    }

                },
                //display navigation mode buttons and select the one corresponding to the current display mode
                _renderPickerFooter : function() {
                    this.$element.find(".colorpicker-footer").html(
                            COLOPICKER_FOOTER_TEMPLATE);
                    if (this.options.config.displayModes !== undefined) {
                        if (this.options.config.displayModes.classicPalette ||
                                this.options.config.displayModes.freestylePalette) {
                            var paletteButton = $(CLASSIC_PALETTE_BUTTON);
                            paletteButton.addClass("selected");
                            this.$element.find(".navigation-bar").append(
                                    paletteButton);
                        }
                        if (this.options.config.displayModes.edit) {
                            this.$element.find(".navigation-bar").append(
                                    EDIT_BUTTON);
                        } else {
                            this.$element.find(".colorpicker-footer").remove();
                            return;
                        }
                    }
                    
                    this.$element.find(".colorpicker-footer button").off("tap.button click.button").fipo( "tap.button", "click.button", function(event) {
                                event.stopPropagation();
                                event.preventDefault();
                                var $target = $(event.target);
                                var $button = null;
                                this.$element.find(
                                        ".navigation-bar > .selected")
                                        .removeClass("selected");
                                if (event.target.nodeName === "BUTTON") {
                                    $target.addClass("selected");
                                    $button = $(event.target);
                                } else {
                                    $target.parent().addClass(
                                            "selected");
                                    $button = $target.parent();
                                }
                                if ($button.attr("id") === "editButton"){
                                    this._renderPicker(EDIT_MODE);
                                }else{
                                    this._renderPicker(CLASSIC_MODE, false, this.currentPage);
                                }

                            }.bind(this));
                },
                //function for palette navigation
                _calculatePaletteBoundaries : function(slide, pageNo) {
                    var colorsPerPage = 0;
                    if (this.options.config.displayModes.freestylePalette) {
                        colorsPerPage = this.palettePageSize *
                                this.colorShadeNo;
                    } else {
                        colorsPerPage = this.palettePageSize;
                    }
                    if (!slide) {
                        if (pageNo !== undefined) {
                            this.lowerLimit = colorsPerPage * pageNo;
                            this.upperLimit = this.lowerLimit + colorsPerPage -
                                    1;
                            this.currentPage = pageNo;
                        } else {
                            this.upperLimit += colorsPerPage - 1;
                            this.lowerLimit = 0;
                            this.currentPage = 0;
                        }
                    } else if (slide === "left") {
                        pageNo = this.currentPage + 1;
                        if (pageNo + 1 > this.pages) {
                            return false;
                        }
                        this.lowerLimit = colorsPerPage * pageNo;
                        this.upperLimit = this.lowerLimit + colorsPerPage - 1;
                        this.currentPage = pageNo;
                    } else {
                        pageNo = this.currentPage - 1;
                        if (pageNo < 0) {
                            return false;
                        }
                        this.lowerLimit = colorsPerPage * pageNo;
                        this.upperLimit = this.lowerLimit + colorsPerPage - 1;
                        this.currentPage = pageNo;
                    }
                    return true;
                },
                //display navigation bullets
                _renderPaletteNavigation : function() {
                    this.$element.find(".palette-navigator").remove();
                    var navigator = $("<div>");
                    navigator.addClass("palette-navigator");
                    if (this.options.config.displayModes.classicPalette) {
                        this.pages = Math.ceil(this.colorNames.length /
                                this.palettePageSize);
                    } else {
                        this.pages = Math.ceil(this.colorNames.length /
                                (this.palettePageSize * this.colorShadeNo));
                    }
                    if (this.pages > 1) {
                        for ( var i = 0; i < this.pages; i++) {
                            navigator.append("<i class='dot' page='" + i +
                                    "'></i>");
                        }
                    }
                    this.$element.find(".colorpicker-body").append(navigator);
                    this.$element.find("i[page='" + this.currentPage + "']")
                            .addClass("active");

                    // Move around
                    this.$element.find(".colorpicker-body").on("swipe",
                            function(event) {
                                this._renderPicker(CLASSIC_MODE, event.direction === "left" ? "left" : "right");
                            }.bind(this));
                    this.$element.find(".dot").off("tap.dot click.dot").fipo("tap.dot", "click.dot", function(event) {
                        event.stopPropagation();

                        if (this.currentPage === parseInt($(event.target).attr("page"), 10)) {
                            return;
                        }

                        this._renderPicker(CLASSIC_MODE, false, parseInt($(event.target).attr("page"), 10));
                    }.bind(this));
                },

                _slidePicker : function(newtable, isLeft) {
                    this.$element.find(".sliding-container table").stop(true,
                            true);
                    this.$element.find(".sliding-container").remove();

                    var oldtable = this.$element.find("table");
                    var width = oldtable.width();
                    var height = oldtable.height();

                    var container = $("<div class=\"sliding-container\">");

                    container.css({
                        "display" : "block",
                        "position" : "relative",
                        "width" : width + "px",
                        "height" : height + "px",
                        "overflow" : "hidden"
                    });

                    this.$element.find(".palette-navigator").before(container);
                    container.append(oldtable).append(newtable);
                    oldtable.css({
                        "position" : "absolute",
                        "left" : 0,
                        "top" : 0
                    });
                    oldtable.after(newtable);
                    newtable.css({
                        "position" : "absolute",
                        "left" : (isLeft) ? width : -width,
                        "top" : 0
                    });

                    var speed = 400;

                    oldtable.animate({
                        "left" : (isLeft) ? -width : width
                    }, speed, function() {
                        oldtable.remove();
                    });

                    newtable.animate({
                        "left" : 0
                    }, speed, function() {
                        if (container.parents().length === 0)
                            return; // We already were detached!
                        newtable.css({
                            "position" : "relative",
                            "left" : 0,
                            "top" : 0
                        });
                        newtable.detach();
                        this.$element.find(".palette-navigator").before(
                                newtable);
                        container.remove();
                    }.bind(this));
                },
                //render the selected color name and hex code
                _renderPaletteHeader : function() {
                    var title = $('<div class="palette-header"><div class="title"></div><div class="selection"></div></div>');
                    var $picker = this.$element.find(".colorpicker-holder");
                    if ($picker.find(".palette-header").length > 0) {
                        $picker.find(".palette-header").replaceWith(title);
                    } else {
                        $picker.prepend(title);
                    }
                    $picker.find(".title").html(
                            "<span>" + this.options.title + "</span>");
                },

                _renderPalette : function() {
                    this._renderPaletteHeader();

                    var table = $("<table>");
                    var html = "";

                    for ( var i = 0; i < this.palettePageSize; i++) {
                        html += "<tr>";
                        var opacity = 0;
                        var rgb = "";
                        var cssClass = "";
                        var shade = "";
                        for ( var sh = 0; sh < this.colorShadeNo; sh++) {
                            if (this.options.config.displayModes.classicPalette) {
                              //display colors with shades
                                if (this.colorNames.length - 1 < i +
                                        this.lowerLimit) {
                                    html += "<td><a></a></td>";
                                } else {
                                    rgb = CUI.util.color.HexToRGB(this.options.config.colors[this.colorNames[i +
                                                    this.lowerLimit]]);
                                    shade = "rgba(" + rgb.r + "," + rgb.g +
                                            "," + rgb.b + "," +
                                            (1 - opacity).toFixed(2) + ")";
                                    opacity += 0.16;
                                    if (CUI.util.color.isSameColor(shade,
                                            this.$hiddenInput.val())) {
                                        cssClass = "selected";
                                        this._fillSelectedColor(this.colorNames[i + this.lowerLimit], CUI.util.color.RGBAToHex(shade));
                                    } else {
                                        cssClass = "";
                                    }
                                    html += "<td class='filled'><a style='background-color:" +
                                            shade +
                                            "' color='" +
                                            shade +
                                            "' colorName='" +
                                            this.colorNames[i + this.lowerLimit] +
                                            "' class='" +
                                            cssClass +
                                            "'>" +
                                            "</a></td>";
                                }
                            } else {
                              //display colors without shades (freestyle)
                                if (this.colorNames.length - 1 < i *
                                        this.colorShadeNo + sh) {
                                    html += "<td><a></a></td>";
                                } else {
                                    rgb = CUI.util.color.HexToRGB(this.options.config.colors[this.colorNames[i *
                                                    this.colorShadeNo + sh]]);
                                    shade = "rgba(" + rgb.r + "," + rgb.g + "," + rgb.b + "," + 1 + ")";
                                    if (CUI.util.color.isSameColor(shade,
                                            this.$hiddenInput.val())) {
                                        cssClass = "selected";
                                    } else {
                                        cssClass = "";
                                    }
                                    html += "<td class='filled'><a style='background-color:" +
                                            shade +
                                            "' color='" +
                                            shade +
                                            "' colorName='" +
                                            this.colorNames[i *
                                                    this.colorShadeNo + sh] +
                                            "' class='" +
                                            cssClass +
                                            "'>" +
                                            "</a></td>";
                                }
                            }
                        }
                        html += "</tr>";
                    }
                    table.append("<tbody>" + html + "</tbody>");
                    //click on a color box
                    table.find("a").off("tap.a click.a").fipo("tap.a", "click.a", function(event) {
                                        event.stopPropagation();
                                        event.preventDefault();

                                        if (CUI.util.color.isSameColor(this.$hiddenInput
                                                .val(), $(event.target).attr(
                                                "color"))) {
                                            return;
                                        }

                                        this.$element.find("table").find(
                                                ".selected").removeClass(
                                                "selected");
                                        $(event.target).addClass("selected");

                                        var colorName = $(event.target).attr("colorName") !== undefined ? $(event.target).attr("colorName"): "";
                                        this._fillSelectedColor(colorName, CUI.util.color.RGBAToHex($(event.target).attr("color")));

                                        this._setColor($(event.target).attr("color"));
                                    }.bind(this));
                    var $navigator = this.$element.find(".palette-navigator");
                    $navigator.find(".active").removeClass("active");
                    $navigator.find("i[page='" + this.currentPage + "']").addClass("active");

                    return table;
                },
                
                _fillSelectedColor : function(colorName, hexVal){
                    this.$element.find(".colorpicker-holder").find(".selection")
                    .html(
                            "<div><span>" +
                            colorName +
                            "</span><span>" +
                            hexVal +
                                    "</span></div>");  
                },
                //render edit mode screen
                _renderEditPalette : function(){
                    var table = $("<table>");
                    var html = "<tr>" + 
                                //hex color representation
                                    "<td colspan='2' rowspan='2'>" +
                                        "<div class='color'></div>" + 
                                     "</td>" +
                                     "<td class='label'>HEX</td>" + 
                                     "<td colspan='2'>" +
                                         "<input type='text' name=':hex'/>" +
                                     "</td>" + 
                                     "<td colspan='2'>&nbsp;</td>" + 
                                "</tr>" + 
                                //RGB color representation in 3 input fields(r, g,b)
                                "<tr>" + 
                                    "<td class='label'>RGB</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':rgb_r'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':rgb_g'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':rgb_b'/>" +
                                    "</td>" + 
                                    "<td>&nbsp;</td>" + 
                                "</tr>" +
                              //CMYK color representation in 4 input fields(c,m,y,k)
                                "<tr>" + 
                                    "<td colspan='2'>&nbsp;</td>" + 
                                    "<td class='label'>CMYK</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_c'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_m'/>" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_y'/" +
                                    "</td>" + 
                                    "<td>" +
                                        "<input type='text' name=':cmyk_k'/>" +
                                    "</td>" + 
                                "</tr>" +
                                //save button to store the color on the launcher
                                "<tr>" + 
                                    "<td colspan='3'>&nbsp;</td>" + 
                                    "<td colspan='4'>" +
                                        "<button class='primary'>Save Color</button>" +
                                    "</td>" + 
                                "</tr>";

                    table.append("<tbody>" + html + "</tbody>");
                    
                    this.$element.find(".palette-header").remove();
                    //input validations for change events
                    table.find("input[name^=':rgb_']").each(function(index, element){
                        $(element).attr("maxlength", "3");
                        $(element).on("blur", function(event){
                            var rgbRegex = /^([0]|[1-9]\d?|[1]\d{2}|2([0-4]\d|5[0-5]))$/;
                            if (!rgbRegex.test($(event.target).val().trim()) || $("input:text[value=''][name^='rgb_']").length > 0){
                                $(event.target).val("");
                                this._clearCMYKFields();
                                this.$element.find("input[name=':hex']").val("");
                                this.$element.find("div.color").removeAttr("style");
                                return;
                            }
                            var rgb = {r:this.$element.find("input[name=':rgb_r']").val(), g:this.$element.find("input[name=':rgb_g']").val(), b:this.$element.find("input[name=':rgb_b']").val()};
                            var cmyk = CUI.util.color.RGBtoCMYK(rgb);
                            var hex = CUI.util.color.RGBToHex(rgb);
                            this._fillCMYKFields(cmyk);
                            this.$element.find("input[name=':hex']").val(hex);
                            this.$element.find("div.color").css("background", hex);
                        }.bind(this));
                    }.bind(this));
                    table.find("input[name^=':cmyk_']").each(function(index, element){
                        $(element).attr("maxlength", "3");
                        $(element).on("blur", function(event){
                            var cmykRegex = /^[1-9]?[0-9]{1}$|^100$/;
                            if (!cmykRegex.test($(event.target).val().trim()) || $("input:text[value=''][name^='cmyk_']").length > 0){
                                $(event.target).val("");
                                this._clearRGBFields();
                                this.$element.find("input[name=':hex']").val("");
                                this.$element.find("div.color").removeAttr("style");
                                return;
                            }
                            var cmyk = {c:this.$element.find("input[name=':cmyk_c']").val(), m:this.$element.find("input[name=':cmyk_m']").val(), y:this.$element.find("input[name=':cmyk_y']").val(), k:this.$element.find("input[name=':cmyk_k']").val()};
                            var rgb = CUI.util.color.CMYKtoRGB(cmyk);
                            var hex = CUI.util.color.RGBToHex(rgb);
                            this.$element.find("input[name=':hex']").val(hex);
                            this._fillRGBFields(rgb);
                            this.$element.find("div.color").css("background", hex);
                        }.bind(this));
                    }.bind(this));
                    table.find("input[name=':hex']").each(function(index, element){
                        $(element).attr("maxlength", "7");
                        $(element).on("blur", function(event){
                            var hex = CUI.util.color.fixHex($(event.target).val().trim());
                            if (hex.length === 0){
                                this._clearRGBFields();
                                this._clearCMYKFields();
                                this.$element.find("div.color").removeAttr("style");
                                return;
                            }
                            var rgb = CUI.util.color.HexToRGB(hex);
                            var cmyk = CUI.util.color.RGBtoCMYK(rgb);
                            this._fillRGBFields(rgb);
                            this._fillCMYKFields(cmyk);
                            table.find("div.color").css("background", hex);
                        }.bind(this));
                    }.bind(this));
                    
                    table.on("click tap", "input, div",
                            function(event) {
                                event.stopPropagation();
                                event.preventDefault();
                                
                            });
                    table.on("click tap", "button",
                            function(event) {
                                event.stopPropagation();
                                event.preventDefault();
                                if (this.$element.find("input[name=':hex']").val() !== undefined && this.$element.find("input[name=':hex']").val().length > 0){
                                    this._setColor(this.$element.find("input[name=':hex']").val());
                                }
                            }.bind(this));

                    return table;
                },
                //set selected color on the launcher
                _setColor : function(color) {
                    this.$hiddenInput.val(color);
                    this.$openButton.css("background-color", this.$hiddenInput
                            .val());
                },

                _fillRGBFields : function(rgb) {
                    this.$element.find("input[name=':rgb_r']").val(rgb.r);
                    this.$element.find("input[name=':rgb_g']").val(rgb.g);
                    this.$element.find("input[name=':rgb_b']").val(rgb.b);
                },

                _clearRGBFields : function() {
                    this.$element.find("input[name^=':rgb']").val("");
                },
                
                _fillCMYKFields : function(cmyk) {
                    this.$element.find("input[name=':cmyk_c']").val(cmyk.c);
                    this.$element.find("input[name=':cmyk_m']").val(cmyk.m);
                    this.$element.find("input[name=':cmyk_y']").val(cmyk.y);
                    this.$element.find("input[name=':cmyk_k']").val(cmyk.k);
                },
                
                _clearCMYKFields : function() {
                    this.$element.find("input[name^=':cmyk']").val("");
                }
                
            });

    CUI.util.plugClass(CUI.Colorpicker);

    // Data API
    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
          $("[data-init~=colorpicker]", e.target).colorpicker();
           
        });
    }
}(window.jQuery));


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

/*
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
*/
(function($, console) {
    // This is the place where the validation rules and messages are defined.
    // But only for validation that is generic or make sense at Coral level.
    // Otherwise please put your validation in your own file at your level.

    "use strict";

    if (!$.message || !$.validator) {
        if (console) console.warn("$.message and/or $.validator are not available, thus nothing is registered.");
        return;
    }


    // Register all the messages for validation
    // IMPORTANT: the order is important, where the last one will be checked first; when in doubt check the source of jquery-message

    $.message.register({
        selector: "*",
        message: {
            "validation.required": "Please fill out this field."
        }
    });

    $.message.register({
        selector: ":lang(de)",
        message: {
        }
    });


    // Register all the validation rules
    // IMPORTANT: the order is important, where the last one will be checked first; when in doubt check the source of jquery-validator

    // TODO Currently we will only use the standard markup for validation rules (e.g. @required, @pattern) due to time constraints and no clear demand yet.
    // But we can certainly define more advance validations. For example the following markup:
    //
    // <input type="text" data-validation="cui.url" />
    // <input type="text" data-validation="granite.path" />
    // <input type="text" data-validation="granite.relativepath" />
    // <input type="text" data-validation="myrule1" />
    //
    // where @data-validation's value is a name (namespaced). The validator can be registered as usual:
    //
    // $.validator.register({
    //     selector: "form input[data-validation='cui.url']",
    //     validate: function(el) {}
    // });
    //
    // We can do all this without modifying jquery-validator and jquery-message at all.
    // So later when you need a fancy validation, please bring this issue to the mailing list first.


    // Cancel the native invalid event (which is triggered by the browser supporting native validation)
    // to show our own UI instead
    $(document).on("cui-contentloaded", function(e) {
        $(":submittable", e.target).on("invalid", function(e) {
            if (e.isJqueryValidator) return;

            e.preventDefault();

            var el = $(this);
            el.checkValidity();
            el.updateErrorUI();
        });
    });


    function simpleShow(el, message) {
        var error = el.next(".form-error");

        el.attr("aria-invalid", "true").toggleClass("error", true);

        if (error.length === 0) {
            el.after($("<span class='form-error' data-init='quicktip' data-quicktip-arrow='top' data-quicktip-type='error' />").html(message));
        } else {
            error.html(message);
        }
    }

    function simpleClear(el) {
        el.removeAttr("aria-invalid").removeClass("error");
        el.next(".form-error").remove();
    }


    // TODO TBD if we want to do validation only when then form is having a certain class
    // e.g. using selector "form.validate input" instead of just "form input"

    $.validator.register({
        selector: "form *",
        show: simpleShow,
        clear: simpleClear
    });

    // Check required & aria-required of input and textarea
    $.validator.register({
        selector: "form input, form textarea",
        validate: function(el) {
            var isRequired = el.prop("required") === true ||
                (el.prop("required") === undefined && el.attr("required") !== undefined) ||
                el.attr("aria-required") === "true";

            if (isRequired && el.val().length === 0) {
                return el.message("validation.required") || "required";
            }
        }
    });

    $(document).on("input", "form input, form textarea", function(e) {
        var el = $(this);
        el.checkValidity();
        el.updateErrorUI();
    });


    // Check role=listbox
    $.validator.register({
        selector: "[role=listbox]",
        validate: function(el) {
            if (el.attr("aria-required") !== "true") {
                return;
            }

            var selected = false;
            el.find("[role=option]").each(function() {
                if ($(this).attr("aria-selected") === "true") {
                    selected = true;
                    return false;
                }
            });

            if (!selected) {
                return el.message("validation.required") || "required";
            }
        }
    });
})(jQuery, window.console);
