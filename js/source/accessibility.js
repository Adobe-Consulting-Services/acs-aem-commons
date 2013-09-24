(function ($, window, undefined) {

    /**
     * initializes role=button
     * http://www.w3.org/WAI/PF/aria-practices/#button
     * @private
     * @param  {jQuery} parent container within the buttons may be located
     */
    function initButton(parent) {
        parent.on('keydown', '[role="button"]:not(button)', function (event) {
            var elem = $(event.currentTarget);

            // space or enter
            if (event.which === 32 || event.which === 13) {

                // button is not allowed to be disabled
                if (elem.attr('aria-disabled')) {
                    return;
                }

                // simple click
                elem.trigger('click');
            }
        });
    }

    $(function () {
        var doc = $(document);

        initButton(doc);
    });

}(jQuery, this));
