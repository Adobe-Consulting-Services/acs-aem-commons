(function() {
    CQ.wcm.EditBase.EDITSHARED = "EDITSHARED";
    CQ.wcm.EditBase.EDITGLOBAL = "EDITGLOBAL";

    var addSharedConfigActions = function(xtype) {
        var origType = CQ.Ext.ComponentMgr.types[xtype];
        var newType = CQ.Ext.extend(origType, {
            applyConfigDefaults: function(config, defaults) {
                if (config.actions) {
                    var pageInfo = CQ.utils.WCM.getPageInfo(config.path);
                    var enabled = pageInfo.sharedComponentProperties && pageInfo.sharedComponentProperties.enabled;
                    var canModify = pageInfo.permissions && pageInfo.permissions.modify;

                    if (!!enabled && !!canModify) {
                        if (config.actions.indexOf(CQ.wcm.EditBase.EDITSHARED) < 0) {
                            var actionsCount = config.actions.length;
                            var editActionIdx = null;
                            for (var i = 0; i < actionsCount; i++) {
                                if (config.actions[i] == CQ.wcm.EditBase.EDIT) {
                                    editActionIdx = i;
                                    break;
                                }
                            }
                            if (editActionIdx != null) {
                                config.actions.splice(editActionIdx + 1, 0, CQ.wcm.EditBase.EDITGLOBAL);
                                config.actions.splice(editActionIdx + 1, 0, CQ.wcm.EditBase.EDITSHARED);
                            }
                        }
                    }
                }
                CQ.wcm.EditBase.applyConfigDefaults.call(this, config, defaults);
            }
        });
        CQ.Ext.reg(xtype, newType);
    };

    // Define the handlers for CQ.wcm.EditBase.EDITGLOBAL and CQ.wcm.EditBase.EDITSHARED
    var editSharedHandler = function(type, editBaseType) {
        var origDialog = this.dialog;
        var origDialogCached = this.dialogs[CQ.wcm.EditBase.EDIT];
        var origPath = this.path;

        var pageInfo = CQ.utils.WCM.getPageInfo(this.path);
        var newPath = pageInfo.sharedComponentProperties.root + "/jcr:content/" + type + "-component-properties";
        if (type == "shared") {
            newPath += "/" + this.getResourceType();
        }

        try {
            this.dialog = this.dialog + type;
            this.dialogs[CQ.wcm.EditBase.EDIT] = this.dialogs[editBaseType];
            this.path = newPath;

            // For global props, need to unregister any previous fetch since it
            // always has the same data path.
            if (type == "global") {
                CQ.WCM.unregisterDialog("editdialog-" + this.path);
            }

            CQ.wcm.EditBase.showDialog(this, editBaseType);
        } catch (e) {
            if (window.console && console.error) {
                console.error(e);
            }
        } finally {
            this.dialog = origDialog;

            // Don't allow the global props dialog to be cached, as it causes
            // problems due to always having the same data path.
            if (type != "global") {
                this.dialogs[editBaseType] = this.dialogs[CQ.wcm.EditBase.EDIT];
            }

            this.dialogs[CQ.wcm.EditBase.EDIT] = origDialogCached;
            this.path = origPath;
        }
    };

    function addSharedConfigActionHandlers(clazz) {
        clazz.ActionsConvertor[CQ.wcm.EditBase.EDITSHARED] = {
            text: CQ.I18n.getMessage("Edit Shared"),
            handler: function (evt) {
                // Using 'evt.ownerCt.defaults.scope' instead of 'this' because
                // 'this' has a permanent reference to the first component for
                // which the menu is launched.
                editSharedHandler.call(evt.ownerCt.defaults.scope, "shared", CQ.wcm.EditBase.EDITSHARED);
            }
        };

        clazz.ActionsConvertor[CQ.wcm.EditBase.EDITGLOBAL] = {
            text: CQ.I18n.getMessage("Edit Global"),
            handler: function (evt) {
                // Using 'evt.ownerCt.defaults.scope' instead of 'this' because
                // 'this' has a permanent reference to the first component for
                // which the menu is launched.
                editSharedHandler.call(evt.ownerCt.defaults.scope, "global", CQ.wcm.EditBase.EDITGLOBAL);
            }
        };
    }

    addSharedConfigActions("editrollover");
    addSharedConfigActionHandlers(CQ.wcm.EditRollover);

})();