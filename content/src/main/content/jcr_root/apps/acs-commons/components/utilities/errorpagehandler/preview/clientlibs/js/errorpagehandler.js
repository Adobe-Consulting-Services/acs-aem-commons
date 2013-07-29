;$(function() {
    $('#error-page-handler .toggle').click(function() {
        var $this = $(this),
        $section = $this.closest('.section');

        if($section.hasClass('collapsed')) {
            $section.removeClass('collapsed');
            $section.addClass('expanded');
            $this.text($this.data('collapse-text'));
        } else {
            $section.removeClass('expanded');
            $section.addClass('collapsed');
            $this.text($this.data('expand-text'));
        }
    });

    $('#error-page-handler .edit-mode').click(function () {
        CQ.WCM.setMode(CQ.WCM.MODE_EDIT);
        return true;
    });

    $('#error-page-handler .edit-error-page').click(function () {
        CQ.WCM.setMode(CQ.WCM.MODE_EDIT);
        return true;
    });
});
