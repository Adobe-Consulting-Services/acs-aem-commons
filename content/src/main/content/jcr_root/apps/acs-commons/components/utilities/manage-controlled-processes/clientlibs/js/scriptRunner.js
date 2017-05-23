/* 
 * Copyright 2017 Adobe.
 *
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
 */

/* global Coral, Granite */

var ScriptRunner = {
    init: function () {
//        $(".coral-Pathbrowser-picker").on("coral-pathbrowser-picker-confirm", ScriptRunner.capturePath);
        ScriptRunner.progress = jQuery("#moveProgress");
    },
    showStartProgressForm: function () {
        var url;
        url = Granite.HTTP.getPath() + ".start-process.html";

        jQuery.ajax({
            url: url,
            dataType: "html",
            success: function (html) {
                ScriptRunner.startDialog = new Coral.Dialog().set({
                    id: 'startProcessDialog',
                    header: {
                        innerHTML: 'Start Process'
                    },
                    content: {
                        innerHTML: html
                    },
                    footer: {
                        innerHTML: '<button id="cancelButton" is="coral-button" variant="default" coral-close>Cancel</button>'
                    },
                    closable: false,
                    variant: "warning"
                });
                ScriptRunner.startDialog.classList.add("coral--dark");
                ScriptRunner.startDialog.on("coral-overlay:open", function () {
                    ScriptRunner.initStartDialog(ScriptRunner.startDialog);
                });
                ScriptRunner.startDialog.on("coral-overlay:close", function (evt) {
                    // This event also triggers for closing sub-dialogs and tooltips
                    if (evt.target === evt.currentTarget) {
                        document.body.removeChild(ScriptRunner.startDialog);
                    }
                });
                ScriptRunner.startDialog.fullscreen = true;
                document.body.appendChild(ScriptRunner.startDialog);
                ScriptRunner.startDialog.show();
            }
        });
    },
    initStartDialog: function (dialog) {
        dialog.querySelector("coral-Icon").icon = "pausePlay";
        dialog.querySelector("#processDefinitionSelector").on("coral-selectlist:change", ScriptRunner.processDefinitionSelected);
        dialog.querySelector("#startButton").on("click", ScriptRunner.startProcess);
    },
    processDefinitionSelected: function (event) {
        if (event && event.target && event.target.selectedItem) {
            ScriptRunner.definition = event.target.selectedItem.value;
            ScriptRunner.definitionName = event.target.selectedItem.innerText;
            ScriptRunner.showProcessInputForm(ScriptRunner.definition);
            document.getElementById("startProcessWizard").next();
        }
    },
    showProcessInputForm: function (definition) {
        var url = Granite.HTTP.getPath() + ".start-process-form.html";

        jQuery.ajax({
            url: url,
            dataType: "html",
            success: function (response) {
                var inputForm = jQuery("#processDefinitionInput");
                var $html, html = Granite.UI.Foundation.Utils.processHtml(response, "#processDefinitionInput", false, true);
                $html = jQuery(html);
                $html.find("#processName").text(ScriptRunner.definitionName);
                $html.find("#process").val(ScriptRunner.definition);
                $html.find("coral-icon").each(function () {
                    if (this.icon) {
                        this.classList.add("coral-Icon--" + this.icon);
                    }
                });
                inputForm.html("").append($html);
                $html.trigger("foundation-contentloaded");
            },
            data: {
                processDefinition: definition
            }
        });
    },
    startProcess: function () {
        var data = {};
        jQuery("#processDefinitionInput form").serializeArray().map(function (x) {
            data[x.name] = x.value;
        });
        jQuery.ajax({
            url: "/bin/mcp",
            dataType: "json",
            success: ScriptRunner.startedSuccessfully,
            error: ScriptRunner.error,
            data: data
        });
    },
    startedSuccessfully: function () {
        var success = new Coral.Dialog().set({
            header: {
                innerHTML: 'Process Started'
            },
            content: {
                innerHTML: "The process was started successfully."
            },
            backdrop: "modal",
            closable: "on",
            variant: "info"
        });
        success.classList.add("coral--dark");
        document.body.appendChild(success);
        success.show();
        ScriptRunner.startDialog.hide();
    },
    pollingLoop: function (data) {
        ScriptRunner.status = data;
        ScriptRunner.updateProgress();
        if (data.infoBean.isRunning) {
            window.setTimeout(function () {
                console.log("polling status...");
                jQuery.ajax({
                    url: "/bin/mcp",
                    dataType: "json",
                    success: ScriptRunner.pollingLoop,
                    error: ScriptRunner.error,
                    data: {
                        action: "status",
                        id: data.id
                    }
                });
            }, 250);
        } else {
            ScriptRunner.finished();
        }
    },
    error: function () {
        ScriptRunner.progress.hide();
        console.log("Error condition detected -- check logs!");
    },
    updateProgress: function () {
        var i, total, sectionWeight, action;
        total = 0;
        sectionWeight = 1.0 / ScriptRunner.status.actions.length;
        for (i = 0; i < ScriptRunner.status.actions.length; i++) {
            action = ScriptRunner.status.actions[i];
            if (action.manager.tasksAdded.value > 0) {
                total += (action.manager.tasksCompleted.value / action.manager.tasksAdded.value) * sectionWeight;
            }
        }
        console.log("Percent completion " + (total * 100));
        ScriptRunner.setProgress(total);
    },
    setProgress: function (val) {
        var percent, label;
        ScriptRunner.progress.show();
        percent = val * 100;
        label = "Move in progress...";
        if (percent > 0 && percent < 100) {
            ScriptRunner.progress.attr("indeterminate", null);
            ScriptRunner.progress.attr("value", percent);
        } else {
            if (val < 0) {
                label = "Working...  Please wait...";
            }
            ScriptRunner.progress.attr("indeterminate", true);
            ScriptRunner.progress.attr("value", 50);
        }
        ScriptRunner.progress[0].label.show();
        ScriptRunner.progress[0].label.innerHTML = label;
    },
    finished: function () {
        ScriptRunner.progress.hide();
        console.log("DONE!");
    }
};

jQuery('#processManager').ready(function () {
    ScriptRunner.init();
});
jQuery(document).on("click", "#startProcess", ScriptRunner.showStartProgressForm);