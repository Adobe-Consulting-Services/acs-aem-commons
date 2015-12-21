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
 * RTE Base Plugin to show configured Touch UI Dialogs
 */
(function ($) {
    "use strict";

    if (typeof window.ACS === "undefined") {
        window.ACS = {};
    }

    if (typeof window.ACS.TouchUI === "undefined") {
        window.ACS.TouchUI = {};
    }

    window.ACS.TouchUI.RTE = {
        GROUP: "acs-commons",
        INSERT_DIALOG_CONTENT_FEATURE: "insertDialogContent",
        INSERT_DIALOG_CONTENT_DIALOG: "insertDialogContentDialog",
        COLOR_PICKER_FEATURE: "colorPicker",
        COLOR_PICKER_DIALOG: "colorPickerDialog",
        REQUESTER: "requester"
    };

    var _ = window._,
        Class = window.Class,
        CUI = window.CUI,
        RTE = ACS.TouchUI.RTE,
        GROUP = "acs-commons",
        AcsCuiToolbarBuilder,
        AcsDialogManager,
        AcsToolkitImpl;

    function getInsertDialogUISetting() {
        return GROUP + "#" + RTE.INSERT_DIALOG_CONTENT_FEATURE;
    }

    function getColorPickerUISetting() {
        return GROUP + "#" + RTE.COLOR_PICKER_FEATURE;
    }

    //extend the toolbar builder to register plugin icon in fullscreen mode
    AcsCuiToolbarBuilder = new Class({
        toString: "ACSCuiToolbarBuilder",

        extend: CUI.rte.ui.cui.CuiToolbarBuilder,

        _getUISettings: function (options) {
            var uiSettings = this.superClass._getUISettings(options),
                toolbar = uiSettings.fullscreen.toolbar,
                feature = getInsertDialogUISetting();

            if (toolbar.indexOf(feature) === -1) {
                toolbar.splice(3, 0, feature);
            }

            if (!this._getClassesForCommand(feature)) {
                this.registerAdditionalClasses(feature, "coral-Icon coral-Icon--tableEdit");
            }

            feature = getColorPickerUISetting();

            if (toolbar.indexOf(feature) === -1) {
                toolbar.splice(3, 0, feature);
            }

            if (!this._getClassesForCommand(feature)) {
                //.coral-ColorPicker-button
                this.registerAdditionalClasses(feature, "coral-Icon coral-Icon--textColor");
            }

            return uiSettings;
        }
    });

    //extend the CUI dialog manager to register popover dialog
    AcsDialogManager = new Class({
        toString: "ACSDialogManager",

        extend: CUI.rte.ui.cui.CuiDialogManager,

        create: function (dialogId, config) {
            var context = this.editorKernel.getEditContext(),
                $container = CUI.rte.UIUtils.getUIContainer($(context.root)),
                dialog;

            if (dialogId === RTE.INSERT_DIALOG_CONTENT_DIALOG) {
                dialog = new RTE.InsertDialogContentPluginDialog();
            } else if (dialogId === RTE.COLOR_PICKER_DIALOG) {
                dialog = new RTE.ColorPickerPluginDialog();
            } else {
                return this.superClass.create.call(this, dialogId, config);
            }

            dialog.attach(config, $container, this.editorKernel, true);

            return dialog;
        }
    });

    //extend the toolkit implementation for custom toolbar builder and dialog manager
    AcsToolkitImpl = new Class({
        toString: "ACSToolkitImpl",

        extend: CUI.rte.ui.cui.ToolkitImpl,

        createToolbarBuilder: function () {
            return new AcsCuiToolbarBuilder();
        },

        createDialogManager: function (editorKernel) {
            return new AcsDialogManager(editorKernel);
        }
    });

    CUI.rte.ui.ToolkitRegistry.register("cui", AcsToolkitImpl);

    RTE.DialogPlugin = new Class({
        toString: "TouchUIInsertDialogPlugin",

        extend: CUI.rte.plugins.Plugin,

        pickerUI: null,

        getFeatures: function () {
            return [ RTE.INSERT_DIALOG_CONTENT_FEATURE, RTE.COLOR_PICKER_FEATURE ];
        },

        initializeUI: function (tbGenerator) {
            var plg = CUI.rte.plugins, config;

            if (this.isFeatureEnabled(RTE.INSERT_DIALOG_CONTENT_FEATURE)) {
                config = this.config[RTE.INSERT_DIALOG_CONTENT_FEATURE];

                this.pickerUI = tbGenerator.createElement(RTE.INSERT_DIALOG_CONTENT_FEATURE,
                    this, true, config.tooltip || "Insert TouchUI Dialog");

            }else if(this.isFeatureEnabled(RTE.COLOR_PICKER_FEATURE)){
                config = this.config[RTE.COLOR_PICKER_FEATURE];

                this.pickerUI = tbGenerator.createElement(RTE.COLOR_PICKER_FEATURE,
                    this, true, config.tooltip || "Select Color");
            }

            tbGenerator.addElement(RTE.GROUP, plg.Plugin.SORT_FORMAT, this.pickerUI, 120);
        },

        execute: function (id, value, envOptions) {
            return new RTE.InsertTouchUIDialogPlugin(id, value, envOptions);
        }
    });

    CUI.rte.plugins.PluginRegistry.register(RTE.GROUP, RTE.DialogPlugin);
}(jQuery));
