/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
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
 *
 * A sample component dialog using the Touch UI Multi Field
 * Note the usage of empty valued acs-commons-nested property
 *
 */
(function ($, $document, Handlebars) {
    "use strict";

    var _ = window._,
        Class = window.Class,
        CUI = window.CUI,
        REQUESTER = "requester",
        GROUP = "acs-commons",
        INSERT_DIALOG_CONTENT_FEATURE = "insertDialogContent",
        INSERT_DIALOG_CONTENT_DIALOG = "insertDialogContentDialog",
        ACS_CUI_TOOLBAR_BUILDER,
        INSERT_DIALOG_CONTENT_PLUGIN_DIALOG,
        ACS_DIALOG_MANAGER,
        ACS_TOOLKIT_IMPL,
        INSERT_TOUCHUI_DIALOG_PLUGIN,
        INSERT_TOUCHUI_DIALOG_CMD;

    function getUISetting() {
        return GROUP + "#" + INSERT_DIALOG_CONTENT_FEATURE;
    }

    function showErrorAlert(message, title){
        var fui = $(window).adaptTo("foundation-ui"),
            options = [{
                text: "OK",
                warning: true
            }];

        message = message || "Unknown Error";
        title = title || "Error";

        fui.prompt(title, message, "notice", options);
    }

    //extend the toolbar builder to register plugin icon in fullscreen mode
    ACS_CUI_TOOLBAR_BUILDER = new Class({
        toString: "ACSCuiToolbarBuilder",

        extend: CUI.rte.ui.cui.CuiToolbarBuilder,

        _getUISettings: function (options) {
            var uiSettings = this.superClass._getUISettings(options),
                toolbar = uiSettings.fullscreen.toolbar,
                feature = getUISetting();

            if (toolbar.indexOf(feature) === -1) {
                toolbar.splice(3, 0, feature);
            }

            if (!this._getClassesForCommand(feature)) {
                this.registerAdditionalClasses(feature, "coral-Icon coral-Icon--tableEdit");
            }

            return uiSettings;
        }
    });

    //popover dialog hosting iframe
    INSERT_DIALOG_CONTENT_PLUGIN_DIALOG = new Class({
        extend: CUI.rte.ui.cui.AbstractBaseDialog,

        toString: "ACSPluginDialog",

        getDataType: function () {
            return INSERT_DIALOG_CONTENT_DIALOG;
        }
    });

    //extend the CUI dialog manager to register popover dialog
    ACS_DIALOG_MANAGER = new Class({
        toString: "ACSDialogManager",

        extend: CUI.rte.ui.cui.CuiDialogManager,

        create: function (dialogId, config) {
            if (dialogId !== INSERT_DIALOG_CONTENT_DIALOG) {
                return this.superClass.create.call(this, dialogId, config);
            }

            var context = this.editorKernel.getEditContext(),
                $container = CUI.rte.UIUtils.getUIContainer($(context.root)),
                dialog = new INSERT_DIALOG_CONTENT_PLUGIN_DIALOG();

            dialog.attach(config, $container, this.editorKernel, true);

            return dialog;
        }
    });

    //extend the toolkit implementation for custom toolbar builder and dialog manager
    ACS_TOOLKIT_IMPL = new Class({
        toString: "ACSToolkitImpl",

        extend: CUI.rte.ui.cui.ToolkitImpl,

        createToolbarBuilder: function () {
            return new ACS_CUI_TOOLBAR_BUILDER();
        },

        createDialogManager: function (editorKernel) {
            return new ACS_DIALOG_MANAGER(editorKernel);
        }
    });

    CUI.rte.ui.ToolkitRegistry.register("cui", ACS_TOOLKIT_IMPL);

    INSERT_TOUCHUI_DIALOG_PLUGIN = new Class({
        toString: "TouchUIInsertDialogPlugin",

        extend: CUI.rte.plugins.Plugin,

        pickerUI: null,

        getFeatures: function () {
            return [ INSERT_DIALOG_CONTENT_FEATURE ];
        },

        initializeUI: function (tbGenerator) {
            var plg = CUI.rte.plugins;

            if (!this.isFeatureEnabled(INSERT_DIALOG_CONTENT_FEATURE)) {
                return;
            }

            this.pickerUI = tbGenerator.createElement(INSERT_DIALOG_CONTENT_FEATURE,
                                            this, true, "Insert TouchUI Dialog");

            tbGenerator.addElement(GROUP, plg.Plugin.SORT_FORMAT, this.pickerUI, 120);
        },

        execute: function (id) {
            var ek = this.editorKernel,
                dm = ek.getDialogManager(),
                $popover, dialog, popoverConfig,
                dialogConfig = {
                    parameters: {
                        "command": getUISetting()
                    }
                };

            popoverConfig = this.config[INSERT_DIALOG_CONTENT_FEATURE];

            if(_.isEmpty(popoverConfig)){
                showErrorAlert("Config node '" + INSERT_DIALOG_CONTENT_FEATURE + "' not defined");
                return;
            }

            if(_.isEmpty(popoverConfig.dialogPath)){
                showErrorAlert("Parameter dialogPath of '" + INSERT_DIALOG_CONTENT_FEATURE + "' not defined");
                return;
            }

            if(_.isEmpty(popoverConfig.onsubmit)){
                showErrorAlert("Parameter onsubmit listener of '" + INSERT_DIALOG_CONTENT_FEATURE + "' not defined");
                return;
            }

            dialog = this.dialog = dm.create(INSERT_DIALOG_CONTENT_DIALOG, dialogConfig);

            dm.prepareShow(this.dialog);

            dm.show(this.dialog);

            $popover = this.dialog.$dialog.find(".coral-Popover-content");

            function removeReceiveDataListener(handler) {
                if (window.removeEventListener) {
                    window.removeEventListener("message", handler);
                } else if (window.detachEvent) {
                    window.detachEvent("onmessage", handler);
                }
            }

            function registerReceiveDataListener(handler) {
                if (window.addEventListener) {
                    window.addEventListener("message", handler, false);
                } else if (window.attachEvent) {
                    window.attachEvent("onmessage", handler);
                }
            }

            function receiveMessage(event) {
                if (_.isEmpty(event.data)) {
                    return;
                }

                var message = JSON.parse(event.data),
                    action;

                if (!message || message.sender !== GROUP) {
                    return;
                }

                action = message.action;

                if (action === "submit") {
                    if (!_.isEmpty(message.data)) {
                        ek.relayCmd(id, message.data);
                    }
                }

                dialog.hide();

                removeReceiveDataListener(receiveMessage);
            }

            function loadPopoverUI($popover) {
                var url = popoverConfig.dialogPath + ".html?" + REQUESTER + "=" + GROUP;

                $popover.parent().css("width", ".1px").height(".1px").css("border", "none");
                $popover.css("width", ".1px").height(".1px");
                $popover.find("iframe").attr("src", url);

                //receive the dialog values from child window
                registerReceiveDataListener(receiveMessage);
            }

            loadPopoverUI($popover);
        },

        //to mark the icon selected/deselected
        updateState: function (selDef) {
            var hasUC = this.editorKernel.queryState(INSERT_DIALOG_CONTENT_FEATURE, selDef);

            if (this.pickerUI !== null) {
                this.pickerUI.setSelected(hasUC);
            }
        }
    });

    CUI.rte.plugins.PluginRegistry.register(GROUP, INSERT_TOUCHUI_DIALOG_PLUGIN);

    INSERT_TOUCHUI_DIALOG_CMD = new Class({
        toString: "TouchUIInsertDialogCmd",

        extend: CUI.rte.commands.Command,

        isCommand: function (cmdStr) {
            return (cmdStr.toLowerCase() === INSERT_DIALOG_CONTENT_FEATURE);
        },

        getProcessingOptions: function () {
            var cmd = CUI.rte.commands.Command;
            return cmd.PO_BOOKMARK | cmd.PO_SELECTION;
        },

        execute: function (execDef) {
            var acsPlugins = execDef.component.registeredPlugins[GROUP],
                popoverConfig = acsPlugins.config[INSERT_DIALOG_CONTENT_FEATURE],
                /*jshint -W061 */
                onSubmitFn = eval("(" + popoverConfig.onsubmit + ")"),
                html = onSubmitFn(execDef.value);

            execDef.editContext.doc.execCommand("insertHTML", false, html);
        }
    });

    CUI.rte.commands.CommandRegistry.register(INSERT_DIALOG_CONTENT_FEATURE, INSERT_TOUCHUI_DIALOG_CMD);

    //returns the picker dialog html
    //Handlebars doesn't do anything useful here, but the framework expects a template
    function dlgTemplate() {
        CUI.rte.Templates["dlg-" + INSERT_DIALOG_CONTENT_DIALOG] =
            Handlebars.compile('<div data-rte-dialog="' + INSERT_DIALOG_CONTENT_DIALOG +
                '" class="coral--dark coral-Popover coral-RichText-dialog">' +
                '<iframe width="1100px" height="700px"></iframe>' +
                '</div>');
    }

    dlgTemplate();
})(jQuery, jQuery(document), Handlebars);

(function($, $document){
    var SENDER = "acs-commons",
        REQUESTER = "requester",
        HELP_BUTTON_SEL = ".cq-dialog-help",
        CANCEL_BUTTON_SEL = ".cq-dialog-cancel",
        SUBMIT_BUTTON_SEL = ".cq-dialog-submit";

    $document.on("foundation-contentloaded", stylePopoverIframe);

    function queryParameters() {
        var result = {}, param,
            params = document.location.search.split(/\?|\&/);

        params.forEach( function(it) {
            if (_.isEmpty(it)) {
                return;
            }

            param = it.split("=");
            result[param[0]] = param[1];
        });

        return result;
    }

    function stylePopoverIframe(){
        if(queryParameters()[REQUESTER] !== SENDER ){
            return;
        }

        var $dialog = $(".cq-dialog"),
            $cancel = $dialog.find(CANCEL_BUTTON_SEL),
            $submit = $dialog.find(SUBMIT_BUTTON_SEL);

        $dialog.css("border", "solid 2px");
        $dialog.find(HELP_BUTTON_SEL).hide();

        $document.off("click", CANCEL_BUTTON_SEL);
        $document.off("click", SUBMIT_BUTTON_SEL);
        $document.off("submit");

        $cancel.click(sendCloseMessage);
        $submit.click(sendDataMessage);
    }

    function sendCloseMessage(){
        var message = {
            sender: SENDER,
            action: "close"
        };

        parent.postMessage(JSON.stringify(message), "*");
    }

    function sendDataMessage(){
        var message = {
            sender: SENDER,
            action: "submit"
        }, dialogData = {}, $dialog, $field;

        $dialog = $(".cq-dialog");

        $dialog.find("[name^='./']").each(function(index, field){
            $field = $(field);
            dialogData[$field.attr("name").substr(2)] = $field.val();
        });

        message.data = dialogData;

        parent.postMessage(JSON.stringify(message), "*");
    }
})(jQuery, jQuery(document));