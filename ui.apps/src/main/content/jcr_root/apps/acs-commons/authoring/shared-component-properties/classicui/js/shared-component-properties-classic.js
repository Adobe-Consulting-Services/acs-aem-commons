/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2016 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
                        var resourceType = config.params["./sling:resourceType"];
                        var componentSharedDialogs = pageInfo.sharedComponentProperties.components[resourceType] || {};
                        if (componentSharedDialogs[2] || componentSharedDialogs[3]) {
                            if (config.actions.indexOf(CQ.wcm.EditBase.EDITSHARED) < 0) {
                                var actionsCount = config.actions.length;
                                var editActionIdx = null;
                                for (var i = 0; i < actionsCount; i++) {
                                    if (config.actions[i] == CQ.wcm.EditBase.EDIT) {
                                        editActionIdx = i;
                                        break;
                                    }
                                }
                                if (editActionIdx !== null) {
                                    // If global properties are enabled for this component...
                                    if (componentSharedDialogs[3]) {
                                        config.actions.splice(editActionIdx + 1, 0, CQ.wcm.EditBase.EDITGLOBAL);
                                    }

                                    // If shared properties are enabled for this component...
                                    if (componentSharedDialogs[2]) {
                                        config.actions.splice(editActionIdx + 1, 0, CQ.wcm.EditBase.EDITSHARED);
                                    }
                                }
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
            this.dialog = this.dialog + "_" + type;
            this.dialogs[CQ.wcm.EditBase.EDIT] = this.dialogs[editBaseType];
            this.path = newPath;

            // For global props, need to unregister a previously fetched global
            // props dialog since it always has the same data path but the
            // dialog itself can be different for different components.
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