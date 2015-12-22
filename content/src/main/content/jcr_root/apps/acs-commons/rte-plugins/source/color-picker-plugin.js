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
 * TouchUI RTE Plugin for color picker
 *
 * Steps:
 *
 * 1) Create ACS Commons plugin nt:unstructured node "acs-commons"
 *      eg. /apps/<project>/components/text/dialog/items/tab1/items/text/rtePlugins/acs-commons
 * 3) Add property "features" of type String[] and single value "colorPicker"
 */
(function ($, $document, Handlebars) {
    "use strict";

    var _ = window._,
        Class = window.Class,
        CUI = window.CUI,
        RTE = ACS.TouchUI.RTE,
        DIALOG_URL = "/apps/acs-commons/dialogs/color-picker-popover/cq:dialog",
        PICKER_NAME_IN_POPOVER = "color",
        ColorPickerCmd;

    function getUISetting() {
        return RTE.GROUP + "#" + RTE.COLOR_PICKER_FEATURE;
    }

    //popover dialog hosting iframe
    RTE.ColorPickerPluginDialog = new Class({
        extend: CUI.rte.ui.cui.AbstractBaseDialog,

        toString: "ACSColorPickerPluginDialog",

        getDataType: function () {
            return RTE.COLOR_PICKER_DIALOG;
        }
    });

    RTE.ColorPickerPlugin = new Class({
        toString: "ACSColorPickerDialogPlugin",

        extend: CUI.rte.plugins.Plugin,

        execute: function (id, value, envOptions) {
            var ek = this.editorKernel,
                dm = ek.getDialogManager(),
                selection, tag, dialogConfig,
                $popover, dialog, context = envOptions.editContext;

            function isValidSelection(){
                var winSel = window.getSelection();
                return winSel && winSel.type && winSel.type.toUpperCase() === "RANGE";
            }

            if(!isValidSelection()){
                return;
            }

            dialogConfig = {
                parameters: {
                    "command": getUISetting()
                }
            };

            dialog = this.dialog = dm.create(RTE.COLOR_PICKER_DIALOG, dialogConfig);

            dialog.restoreSelectionOnHide = false;

            dm.prepareShow(this.dialog);

            dm.show(this.dialog);

            $popover = this.dialog.$dialog.find(".coral-Popover-content");

            selection = CUI.rte.Selection.createProcessingSelection(context);

            tag = CUI.rte.Common.getTagInPath(context, selection.startNode, "span" );

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
                }else if(action === "remove"){
                    ek.relayCmd(id);
                }

                dialog.hide();

                RTE.removeReceiveDataListener(receiveMessage);
            }

            function loadPopoverUI($popover, color) {
                var url = DIALOG_URL + ".html?" + RTE.REQUESTER + "=" + RTE.GROUP;

                if(!_.isEmpty(color)){
                    url = url + "&" + PICKER_NAME_IN_POPOVER + "=" + color;
                }

                $popover.parent().css("width", ".1px").height(".1px").css("border", "none");
                $popover.css("width", ".1px").height(".1px");
                $popover.find("iframe").attr("src", url);

                //receive the dialog values from child window
                RTE.registerReceiveDataListener(receiveMessage);
            }

            loadPopoverUI($popover, $(tag).css("color"));
        }
    });

    ColorPickerCmd = new Class({
        toString: "ACSColorPickerDialogCmd",

        extend: CUI.rte.commands.Command,

        isCommand: function (cmdStr) {
            return (cmdStr.toLowerCase() === RTE.COLOR_PICKER_FEATURE);
        },

        getProcessingOptions: function () {
            var cmd = CUI.rte.commands.Command;
            return cmd.PO_SELECTION | cmd.PO_BOOKMARK | cmd.PO_NODELIST;
        },

        _getTagObject: function(color) {
            return {
                "tag": "span",
                "attributes": {
                    "style" : "color: " + color
                }
            };
        },

        execute: function (execDef) {
            var color = execDef.value ? execDef.value[PICKER_NAME_IN_POPOVER] : undefined,
                selection = execDef.selection,
                nodeList = execDef.nodeList,
                common, context, tagObj, tags;

            if (!selection || !nodeList) {
                return;
            }

            common = CUI.rte.Common;
            context = execDef.editContext;
            tagObj = this._getTagObject(color);

            //if no color value passed, assume delete and remove color
            if(_.isEmpty(color)){
                nodeList.removeNodesByTag(execDef.editContext, tagObj.tag, undefined, true);
                return;
            }

            tags = common.getTagInPath(context, selection.startNode, tagObj.tag);

            //remove existing color before adding new color
            if (tags !== null) {
                nodeList.removeNodesByTag(execDef.editContext, tagObj.tag, undefined, true);
            }

            nodeList.surround(execDef.editContext, tagObj.tag, tagObj.attributes);
        }
    });

    CUI.rte.commands.CommandRegistry.register(RTE.COLOR_PICKER_FEATURE, ColorPickerCmd);

    //returns the picker dialog html
    function dlgTemplate() {
        CUI.rte.Templates["dlg-" + RTE.COLOR_PICKER_DIALOG] =
            Handlebars.compile('<div data-rte-dialog="' + RTE.COLOR_PICKER_DIALOG +
                '" class="coral--dark coral-Popover coral-RichText-dialog">' +
                '<iframe width="525px" height="470px"></iframe>' +
                '</div>');
    }

    dlgTemplate();
}(jQuery, jQuery(document), window.Handlebars));

(function($, $document){
    var _ = window._,
        Class = window.Class,
        CUI = window.CUI,
        RTE = ACS.TouchUI.RTE,
        COLOR = "color",
        ADD_COLOR_BUT = "#EAEM_CP_ADD_COLOR",
        REMOVE_COLOR_BUT = "#EAEM_CP_REMOVE_COLOR",
        PICKER_COLORS = location.pathname.replace(".html", "") + "/content/items/column/items/picker/colors.infinity.json",
        HELP_BUTTON_SEL = ".cq-dialog-help",
        CANCEL_BUTTON_SEL = ".cq-dialog-cancel",
        SUBMIT_BUTTON_SEL = ".cq-dialog-submit",
        pickerInstance;

    if(RTE.queryParameters()[RTE.REQUESTER] !== RTE.GROUP ){
        return;
    }

    function sendCloseMessage(){
        var message = {
            sender: RTE.GROUP,
            action: "close"
        };

        parent.postMessage(JSON.stringify(message), "*");
    }

    function sendRemoveMessage(){
        var message = {
            sender: RTE.GROUP,
            action: "remove"
        };

        parent.postMessage(JSON.stringify(message), "*");
    }

    function sendDataMessage(){
        var message = {
            sender: RTE.GROUP,
            action: "submit",
            data: {}
        }, $dialog, color;

        $dialog = $(".cq-dialog");

        color = $dialog.find("[name='./" + COLOR + "']").val();

        if(color && color.indexOf("rgb") >= 0){
            color = CUI.util.color.RGBAToHex(color);
        }

        message.data[COLOR] = color;

        parent.postMessage(JSON.stringify(message), "*");
    }

    function stylePopoverIframe(){
        var queryParams = RTE.queryParameters(),
            $dialog = $(".cq-dialog"),
            $cancel = $dialog.find(CANCEL_BUTTON_SEL),
            $submit = $dialog.find(SUBMIT_BUTTON_SEL),
            $addColor = $dialog.find(ADD_COLOR_BUT),
            $removeColor = $dialog.find(REMOVE_COLOR_BUT);

        if(!_.isEmpty(RTE.queryParameters()[COLOR])){
            pickerInstance._setColor(decodeURIComponent(queryParams[COLOR]));
        }

        $dialog.css("border", "solid 2px");
        $dialog.find(HELP_BUTTON_SEL).hide();

        $document.find(".coral-ColorPicker").closest(".coral-Form-fieldwrapper")
                        .css("margin-bottom", "20px");

        $document.off("click", CANCEL_BUTTON_SEL);
        $document.off("click", SUBMIT_BUTTON_SEL);
        $document.off("submit");

        $cancel.click(sendCloseMessage);
        $submit.click(sendDataMessage);
        $addColor.click(sendDataMessage);
        $removeColor.click(sendRemoveMessage);
    }

    $document.on("foundation-contentloaded", stylePopoverIframe);

    CUI.Colorpicker = new Class({
        toString: "Colorpicker",
        extend: CUI.Colorpicker,

        _readDataFromMarkup: function () {
            this.superClass._readDataFromMarkup.call(this);

            var el = this.$element;

            //extend otb CUI.Colorpicker to workaround the pickerModes bug
            //in granite/ui/components/foundation/form/colorpicker/render.jsp
            //colorpickerJson.put("modes", pickerModes); should have been
            //colorpickerJson.put("pickerModes", pickerModes);
            if (el.data('config').modes) {
                this.options.config.displayModes = el.data('config').modes;
            }

            pickerInstance = this;

            function setColors(data){
                if(_.isEmpty(data)){
                    return;
                }

                var colors = {};

                _.each(data, function(color, key){
                    if(key.indexOf("jcr:") >= 0){
                        return;
                    }

                    colors[color.name] = color.value;
                });

                pickerInstance.options.config.colors = colors;
            }

            $.ajax({ url: PICKER_COLORS, async: false, dataType: 'json' } ).done(setColors);
        }
    });

    CUI.Widget.registry.register("colorpicker", CUI.Colorpicker);
}(jQuery, jQuery(document)));