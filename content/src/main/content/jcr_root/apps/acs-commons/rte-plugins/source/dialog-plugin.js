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
 * RTE Plugin to show configured Touch UI Dialogs
 *
 * Steps:
 *
 * 1) Create the Touch UI Dialog eg. /apps/test-dialogs/cq:dialog (of sling:resourceType cq/gui/components/authoring/dialog)
 * 2) Create ACS Commons plugin nt:unstructured node "acs-commons"
 *      eg. /apps/<project>/components/text/dialog/items/tab1/items/text/rtePlugins/acs-commons
 * 3) Add property "features" of type String[] and single value "insertDialogContent"
 * 4) Create nt:unstructured "insertDialogContent" under "acs-commons"
 *      eg. /apps/<project>/components/text/dialog/items/tab1/items/text/rtePlugins/acs-commons/insertDialogContent
 * 5) Set the property "dialogPath" in "insertDialogContent" to path created in step 1 eg. /apps/test-dialogs/cq:dialog
 * 6) Set the property "onsubmit" in "insertDialogContent" with listener function (executed on dialog submit)
 *      eg. <code>function(dialogData) { return '<h1>' + dialogData.heading + '</h1>'; }</code>
 * 7) Add to the fullscreen uiSettings "toolbar" property. Include "acs-commons#insertDialogContent" in the desired location
 *      e.g. /apps/<project>/components/text/dialog/items/tab1/items/text/uiSettings/cui/fullscreen
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
        InsertDialogContentPluginDialog,
        InsertTouchUIDialogPlugin,
        InsertTouchUIDialogCmd;

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

    //popover dialog hosting iframe
    InsertDialogContentPluginDialog = new Class({
        extend: CUI.rte.ui.cui.AbstractBaseDialog,

        toString: "ACSPluginDialog",

        getDataType: function () {
            return INSERT_DIALOG_CONTENT_DIALOG;
        }
    });

    InsertTouchUIDialogPlugin = new Class({
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

            var config = this.config[INSERT_DIALOG_CONTENT_FEATURE];

            this.pickerUI = tbGenerator.createElement(INSERT_DIALOG_CONTENT_FEATURE,
                                            this, true, config.tooltip || "Insert TouchUI Dialog");

            tbGenerator.addElement(GROUP, plg.Plugin.SORT_FORMAT, this.pickerUI, 120);
            tbGenerator.registerIcon(GROUP + "#" + INSERT_DIALOG_CONTENT_FEATURE, "coral-Icon coral-Icon--tableEdit");
        },

        execute: function (id) {
            var ek = this.editorKernel,
                dm = ek.getDialogManager(),
                context = ek.getEditContext(),
                $popover, dialog, popoverConfig, $container,
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

            $container = CUI.rte.UIUtils.getUIContainer($(context.root));
            dialog = this.dialog = new InsertDialogContentPluginDialog();
            dialog.attach(dialogConfig, $container, ek);

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

    CUI.rte.plugins.PluginRegistry.register(GROUP, InsertTouchUIDialogPlugin);

    InsertTouchUIDialogCmd = new Class({
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

    CUI.rte.commands.CommandRegistry.register(INSERT_DIALOG_CONTENT_FEATURE, InsertTouchUIDialogCmd);

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