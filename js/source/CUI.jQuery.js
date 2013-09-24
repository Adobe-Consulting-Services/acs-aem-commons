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