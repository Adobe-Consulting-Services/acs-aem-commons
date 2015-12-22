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
 *      eg. /libs/foundation/components/text/dialog/items/tab1/items/text/rtePlugins/acs-commons/insertDialogContent
 * 5) Set the property "dialogPath" in "insertDialogContent" to path created in step 1 eg. /apps/test-dialogs/cq:dialog
 * 6) Set the property "onsubmit" in "insertDialogContent" with listener function (executed on dialog submit)
 *      eg. <code>function(dialogData) { return '<h1>' + dialogData.heading + '</h1>'; }</code>
 *
 */
(function ($, $document, Handlebars) {
    "use strict";

    var _ = window._,
        Class = window.Class,
        CUI = window.CUI,
        RTE = ACS.TouchUI.RTE,
        InsertTouchUIDialogCmd;

    function getUISetting() {
        return RTE.GROUP + "#" + RTE.INSERT_DIALOG_CONTENT_FEATURE;
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
    RTE.InsertDialogContentPluginDialog = new Class({
        extend: CUI.rte.ui.cui.AbstractBaseDialog,

        toString: "ACSPluginDialog",

        getDataType: function () {
            return RTE.INSERT_DIALOG_CONTENT_DIALOG;
        }
    });

    RTE.InsertTouchUIDialogPlugin = new Class({
        toString: "ACSTouchUIInsertDialogPlugin",

        extend: RTE.DialogPlugin,

        execute: function (id) {
            var ek = this.editorKernel,
                dm = ek.getDialogManager(),
                $popover, dialog, popoverConfig,
                dialogConfig = {
                    parameters: {
                        "command": getUISetting()
                    }
                };

            popoverConfig = this.config[RTE.INSERT_DIALOG_CONTENT_FEATURE];

            if(_.isEmpty(popoverConfig)){
                showErrorAlert("Config node '" + RTE.INSERT_DIALOG_CONTENT_FEATURE + "' not defined");
                return;
            }

            if(_.isEmpty(popoverConfig.dialogPath)){
                showErrorAlert("Parameter dialogPath of '" + RTE.INSERT_DIALOG_CONTENT_FEATURE + "' not defined");
                return;
            }

            if(_.isEmpty(popoverConfig.onsubmit)){
                showErrorAlert("Parameter onsubmit listener of '" + RTE.INSERT_DIALOG_CONTENT_FEATURE + "' not defined");
                return;
            }

            dialog = this.dialog = dm.create(RTE.INSERT_DIALOG_CONTENT_DIALOG, dialogConfig);

            dm.prepareShow(this.dialog);

            dm.show(this.dialog);

            $popover = this.dialog.$dialog.find(".coral-Popover-content");

            function receiveMessage(event) {
                if (_.isEmpty(event.data)) {
                    return;
                }

                var message = JSON.parse(event.data),
                    action;

                if (!message || message.sender !== RTE.GROUP) {
                    return;
                }

                action = message.action;

                if (action === "submit") {
                    if (!_.isEmpty(message.data)) {
                        ek.relayCmd(id, message.data);
                    }
                }

                dialog.hide();

                RTE.removeReceiveDataListener(receiveMessage);
            }

            function loadPopoverUI($popover) {
                var url = popoverConfig.dialogPath + ".html?" + RTE.REQUESTER + "=" + RTE.GROUP;

                $popover.parent().css("width", ".1px").height(".1px").css("border", "none");
                $popover.css("width", ".1px").height(".1px");
                $popover.find("iframe").attr("src", url);

                //receive the dialog values from child window
                RTE.registerReceiveDataListener(receiveMessage);
            }

            loadPopoverUI($popover);
        }
    });

    InsertTouchUIDialogCmd = new Class({
        toString: "ACSTouchUIInsertDialogCmd",

        extend: CUI.rte.commands.Command,

        isCommand: function (cmdStr) {
            return (cmdStr.toLowerCase() === RTE.INSERT_DIALOG_CONTENT_FEATURE);
        },

        getProcessingOptions: function () {
            var cmd = CUI.rte.commands.Command;
            return cmd.PO_BOOKMARK | cmd.PO_SELECTION;
        },

        execute: function (execDef) {
            var acsPlugins = execDef.component.registeredPlugins[RTE.GROUP],
                popoverConfig = acsPlugins.config[RTE.INSERT_DIALOG_CONTENT_FEATURE],
                /*jshint -W061 */
                onSubmitFn = eval("(" + popoverConfig.onsubmit + ")"),
                html = onSubmitFn(execDef.value);

            execDef.editContext.doc.execCommand("insertHTML", false, html);
        }
    });

    CUI.rte.commands.CommandRegistry.register(RTE.INSERT_DIALOG_CONTENT_FEATURE, InsertTouchUIDialogCmd);

    //returns the picker dialog html
    function dlgTemplate() {
        CUI.rte.Templates["dlg-" + RTE.INSERT_DIALOG_CONTENT_DIALOG] =
            Handlebars.compile('<div data-rte-dialog="' + RTE.INSERT_DIALOG_CONTENT_DIALOG +
                '" class="coral--dark coral-Popover coral-RichText-dialog">' +
                '<iframe width="1100px" height="700px"></iframe>' +
                '</div>');
    }

    dlgTemplate();
})(jQuery, jQuery(document), Handlebars);

(function($, $document){
    var RTE = ACS.TouchUI.RTE,
        HELP_BUTTON_SEL = ".cq-dialog-help",
        CANCEL_BUTTON_SEL = ".cq-dialog-cancel",
        SUBMIT_BUTTON_SEL = ".cq-dialog-submit";

    $document.on("foundation-contentloaded", stylePopoverIframe);

    function stylePopoverIframe(){
        if(RTE.queryParameters()[RTE.REQUESTER] !== RTE.GROUP ){
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
            sender: RTE.GROUP,
            action: "close"
        };

        parent.postMessage(JSON.stringify(message), "*");
    }

    function sendDataMessage(){
        var message = {
            sender: RTE.GROUP,
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