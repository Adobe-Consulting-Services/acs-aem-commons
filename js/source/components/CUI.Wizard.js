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
