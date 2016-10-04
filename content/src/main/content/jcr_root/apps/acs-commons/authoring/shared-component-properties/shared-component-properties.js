(function ($, ns, channel, window, undefined) {

    var actionDef = {
        icon: 'coral-Icon--globe',
        text: Granite.I18n.get('Configure Shared Component Properties'),
        handler: function (editable, param, target) { // will be called on click
            var originalDialogSrc = editable.config.dialogSrc;
            var originalDialog = editable.config.dialog;
            
            try {
                var dialogSrcArray = editable.config.dialogSrc.split(".html");
                
                var sharedComponentDialogSrc = dialogSrcArray[0].replace("_cq_dialog", "_cq_dialogsharedcomponent") +
                                        ".html" +
                                        ns.page.info.sharedComponentProperties.root +
                                        "/jcr:content/sharedcomponentproperties/" +
                                        editable.type;
                
                editable.config.dialogSrc = sharedComponentDialogSrc;
                editable.config.dialog = editable.config.dialog.replace("cq:dialog", "cq:dialogsharedcomponent");
                
                ns.edit.actions.doConfigure(editable);
            
            } catch(err) {
                console.error("Error getting the dialogsharedcomponent dialog: " + err);
            } finally {
                //set the dialog and dialogSrc back to the original values so normal edit dialog continues to work
                editable.config.dialogSrc = originalDialogSrc;
                editable.config.dialog = originalDialog;
            }
            // do not close toolbar
            return false;
        },
        //Restrict to users with correct permissions and if the dialog exists
        condition: function(editable) {
            var enabled = ns.page.info.sharedComponentProperties && ns.page.info.sharedComponentProperties.enabled;
            var canModify = ns.page.info.permissions && ns.page.info.permissions.modify;
            return !!enabled && !!editable.config.dialog && canModify;
        },
        isNonMulti: true
    };

    // we listen to the messaging channel
    // to figure out when a layer got activated
    channel.on('cq-layer-activated', function (ev) {
        // we continue if the user switched to the Edit layer
        if (ev.layer === 'Edit') {
            // we use the editable toolbar and register an additional action
            ns.EditorFrame.editableToolbar.registerAction('SHARED-COMPONENT-PROPS', actionDef);
        }
    });

}(jQuery, Granite.author, jQuery(document), this));
