(function ($, ns, channel, window, undefined) {

    var actionDef = {
        icon: 'coral-Icon--share',
        text: Granite.I18n.get('Configure Shared Component Properties'),
        handler: function (editable, param, target) { // will be called on click
            var originalDialogSrc = editable.config.dialogSrc;
            var originalDialog = editable.config.dialog;
            
            try {
                var dialogSrcArray = editable.config.dialogSrc.split(".html");
                
                var sharedComponentDialogSrc = dialogSrcArray[0].replace("_cq_dialog", "dialogshared") +
                                        ".html" +
                                        ns.page.info.sharedComponentProperties.root +
                                        "/jcr:content/shared-component-properties/" +
                                        editable.type;
                
                editable.config.dialogSrc = sharedComponentDialogSrc;
                editable.config.dialog = editable.config.dialog.replace("cq:dialog", "dialogshared");
                
                ns.edit.actions.doConfigure(editable);
            
            } catch(err) {
                if (console && console.error) {
                    console.error("Error getting the dialogshared dialog: " + err);
                }
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
            if (!!enabled && !!editable.config.dialog && canModify) {
                // Use this timeout to move the shared component configuration icon to the
                // right of the standard compoennt configuration icon.
                setTimeout(function() {
                    var toolbar = $("#EditableToolbar");
                    var propsButton = toolbar.find("[data-action='CONFIGURE']");
                    if (propsButton.size() > 0) {
                        var sharedPropsButton = toolbar.find("[data-action='SHARED-COMPONENT-PROPS']");
                        sharedPropsButton.remove();
                        propsButton.after(sharedPropsButton);
                    }
                }, 0);
                return true;
            }
            return false;
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
