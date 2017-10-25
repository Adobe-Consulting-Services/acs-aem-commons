$(document).on("dialog-ready", function() {
    // the hidden input
    var $hidden = $('input#acs-hidden-html-editor');
    // if the hidden input is not in the dialog, there is no need to execute the rest of this code.
    if(!$hidden.length) return;

    var initialCode = $hidden.val();
    // new codeflask instance
    var flask = new CodeFlask();
    flask.run('div#acs-html-editor-wrapper', { language: 'markup'});
    // insert initial code.
    flask.update(initialCode);
    // whne code is updated, update hidden input field.
    flask.onUpdate(function(code) { $hidden.val(code); });
});