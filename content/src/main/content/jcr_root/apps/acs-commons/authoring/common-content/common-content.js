(function ($, author, channel, window, undefined) {

    var actionDef = {
        icon: 'coral-Icon--add',
        text: Granite.I18n.get('Configure Shared Content'),
        handler: function (editable, param, target) { // will be called on click
        	alert("this is where we would load a dialog, someone find that");
        },
        /*
         * In the event that we need to restrict this to specific components or something, we would potentially
         * do that in this "condition" function. If we don't, we , we would do that here. Otherwise, leave it out!
         */
//        condition: function(editable) {
//            //restrict to just a specific component, otherwise it will show up on all components.
//            var result = editable.type === "fmmp-base/components/content/[path to component here]";
//            return result;
//        },
        isNonMulti: true
    };

    // we listen to the messaging channel
    // to figure out when a layer got activated
    channel.on('cq-layer-activated', function (ev) {
        // we continue if the user switched to the Edit layer
        if (ev.layer === 'Edit') {
            // we use the editable toolbar and register an additional action
            author.EditorFrame.editableToolbar.registerAction('COMMON-CONTENT', actionDef);
        }
    });

}(jQuery, Granite.author, jQuery(document), this));
