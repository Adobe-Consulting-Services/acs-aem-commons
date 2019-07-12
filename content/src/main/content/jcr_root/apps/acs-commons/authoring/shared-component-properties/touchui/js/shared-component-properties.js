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
(function ($, ns, channel, window, undefined) {
    // check and set flag to prevent other instances of this script from executing
    // when embedded in multiple app clientlibs.
    if (ns.acsSharedComponentPropertiesIsListening) {
        return;
    } else {
        ns.acsSharedComponentPropertiesIsListening = true;
    }

    function initDialog(type) {
        var dialogSrc = "dialog" + type;
        var dialogIcon = 'coral-Icon--' + (type === "shared" ? "layersForward" : "globe");
        var dialogTitle = type == "shared" ? "Configure Shared Properties" : "Configure Global Properties";
        return {
            icon: dialogIcon,
            text: Granite.I18n.get(dialogTitle),
            handler: function (editable, param, target) { // will be called on click
                var originalDialogSrc = editable.config.dialogSrc;
                var originalDialog = editable.config.dialog;

                try {
                    var dialogSrcArray = editable.config.dialogSrc.split(".html");

                    var sharedComponentDialogSrc = dialogSrcArray[0].replace("_cq_dialog", dialogSrc) +
                        ".html" + ns.page.info.sharedComponentProperties.root +
                        "/jcr:content/" + type + "-component-properties";
                    if (type === "shared") {
                        sharedComponentDialogSrc += "/" + editable.type;
                    }

                    editable.config.dialogSrc = sharedComponentDialogSrc;
                    editable.config.dialog = editable.config.dialog.replace("cq:dialog", dialogSrc);

                    ns.edit.actions.doConfigure(editable);
                } catch (err) {
                    if (typeof console === "object" && console.error) {
                        console.error("Error configuring " + dialogSrc + ": " + err);
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
            condition: function (editable) {
                var enabled = ns.page.info.sharedComponentProperties && ns.page.info.sharedComponentProperties.enabled;
                var canModify = ns.page.info.permissions && ns.page.info.permissions.modify;
                if (!!enabled && !!editable.config.dialog && !!canModify) {
                    var componentSharedDialogs = ns.page.info.sharedComponentProperties.components[editable.type] || {};
                    if (componentSharedDialogs[0] || componentSharedDialogs[1]) {
                        if (type == "shared") {
                            // Use this timeout to move the shared component configuration icons to the
                            // right of the standard component configuration icon.
                            setTimeout(function () {
                                var toolbar = $("#EditableToolbar");
                                var propsButton = toolbar.find("[data-action='CONFIGURE']");
                                if (propsButton.size() > 0) {
                                    var sharedPropsButton = toolbar.find("[data-action='SHARED-COMPONENT-PROPS']");
                                    sharedPropsButton.remove();
                                    var globalPropsButton = toolbar.find("[data-action='GLOBAL-COMPONENT-PROPS']");
                                    globalPropsButton.remove();

                                    // If shared properties are enabled for this component...
                                    if (componentSharedDialogs[0]) {
                                        propsButton.after(sharedPropsButton);
                                        propsButton = sharedPropsButton;
                                    }

                                    // If global properties are enabled for this component...
                                    if (componentSharedDialogs[1]) {
                                        propsButton.after(globalPropsButton);
                                    }
                                }
                            }, 0);
                        }
                        return true;
                    }
                }
                return false;
            },
            isNonMulti: true
        };
    }

    // we listen to the messaging channel
    // to figure out when a layer got activated
    channel.on('cq-layer-activated', function (ev) {
        // we continue if the user switched to the Edit layer
        if (ev.layer === 'Edit' && !ns.acsSharedComponentPropertiesIsRegistered) {
            // set flag to prevent multiple firings of this event from triggering our action registration
            ns.acsSharedComponentPropertiesIsRegistered = true;
            // we use the editable toolbar and register an additional action
            ns.EditorFrame.editableToolbar.registerAction('SHARED-COMPONENT-PROPS', initDialog("shared"));
            ns.EditorFrame.editableToolbar.registerAction('GLOBAL-COMPONENT-PROPS', initDialog("global"));
        }
    });
    // AEM 6.3 Bug Fix for #1982
    // we listen to the editables to reset the Shared Component Properties Registered flag so that the Shared and
    // Global properties are loaded as page refreshes due to Edit Config Listeners
    channel.on('cq-editables-updated', function (ev) {
        // reset the flag so that the shared and global icons are loaded again after page refresh
        if (ns.acsSharedComponentPropertiesIsRegistered &&
            ns.EditorFrame && ns.EditorFrame.editableToolbar && ns.EditorFrame.editableToolbar._customActions) {
            ns.acsSharedComponentPropertiesIsRegistered = false;
        }
    });

}(jQuery, Granite.author, jQuery(document), this));
