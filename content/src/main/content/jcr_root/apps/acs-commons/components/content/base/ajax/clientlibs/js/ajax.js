;$(function() {
    $('[data-ajax-component]').each(function() {
        var $this = $(this),
            url = $this.data('url');

        url += "?t=" + (new Date()).getTime();

        $.get(url).success(function(data) {
            $this.replaceWith(data);
        });
    });
});