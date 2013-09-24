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
