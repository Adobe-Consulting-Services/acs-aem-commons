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
        if (document.getElementById("processListing")) {
            ScriptRunner.processTable = document.getElementById("processListing");
            window.top.ScriptRunner = ScriptRunner;
            ScriptRunner.rebuildProcessList();
        }
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
                        window.top.document.body.removeChild(ScriptRunner.startDialog);
                    }
                });
                ScriptRunner.startDialog.fullscreen = true;
                window.top.document.body.appendChild(ScriptRunner.startDialog);
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
            window.top.document.getElementById("startProcessWizard").next();
        }
    },
    showProcessInputForm: function (definition) {
        var url = Granite.HTTP.getPath() + ".start-process-form.html";

        jQuery.ajax({
            url: url,
            dataType: "html",
            success: function (response) {
                var inputForm = window.top.jQuery("#processDefinitionInput");
                var $html, html = Granite.UI.Foundation.Utils.processHtml(response, "#processDefinitionInput", false, true);
                $html = window.top.jQuery(html);
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
        jQuery("#processDefinitionInput form", window.top.document).serializeArray().map(function (x) {
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
    startedSuccessfully: function (process) {
        ScriptRunner.startDialog.hide();
        ScriptRunner.rebuildProcessList();
        jQuery(ScriptRunner.processTable).find("#process-" + process.id)
                .animate({
                    backgroundColor: '#8F8'
                }, 100)
                .animate({
                    backgroundColor: '#FFF'
                }, 1000);
    },
    rebuildProcessList: function () {
        jQuery.ajax({
            url: "/bin/mcp",
            dataType: "json",
            data: {
                action: "list"
            },
            success: function (response) {
                var processDom, process, i, tableBody = jQuery(ScriptRunner.processTable).find("tbody");
                tableBody.empty();
                ScriptRunner.watchList = [];
                for (i = 0; i < response.length; i++) {
                    process = response[i];
                    if (process.infoBean.isRunning) {
                        ScriptRunner.watchList.push(process.id);
                    }

                    if (!process.infoBean.result) {
                        process.infoBean.result = {
                            tasksCompleted: '???'
                        };
                    }
                    processDom = jQuery("<tr is='coral-tr' id='process-" + process.id + "'>" +
                            "<td is='coral-td'>" + process.infoBean.name + "</td>" +
                            "<td is='coral-td'>" + process.infoBean.description + "</td>" +
                            "<td is='coral-td'>" + ScriptRunner.formatTime(process.infoBean.startTime) + "</td>" +
                            "<td is='coral-td' class='process-stop-time'>" +
                            (process.infoBean.isRunning ?
                                    "<coral-progress class='process-progress'></coral-progress>" :
                                    ScriptRunner.formatTime(process.infoBean.stopTime)
                                    ) +
                            "</td>" +
                            "<td is='coral-td' class='process-tasks-completed'>" + process.infoBean.result.tasksCompleted + "</td>" +
                            "<td is='coral-td' class='process-reported-errors'>" + process.infoBean.reportedErrors.length + "</td>" +
                            "</tr>"
                            );
                    processDom.click(ScriptRunner.viewProcessCallback(process.path));
                    tableBody.append(processDom);
                }
                jQuery("#processListing").trigger("foundation-contentloaded");
                ScriptRunner.pollingLoop();
            }
        });
    },
    pollingLoop: function () {
        if (ScriptRunner.watchList && ScriptRunner.watchList.length > 0) {
            window.setTimeout(function () {
                console.log("polling status...");
                jQuery.ajax({
                    url: "/bin/mcp",
                    dataType: "json",
                    success: function (statusList) {
                        var i, process, processRow;
                        ScriptRunner.watchList = [];
                        for (i = 0; i < statusList.length; i++) {
                            process = statusList[i];
                            processRow = jQuery(ScriptRunner.processTable).find("#process-" + process.id);
                            if (process.infoBean.isRunning) {
                                ScriptRunner.watchList.push(process.id);
                                ScriptRunner.setProgress(processRow.find(".process-progress"), process.infoBean.status || "Please wait...", process.infoBean.progress);
                            } else {
                                processRow.find(".process-stop-time").html(ScriptRunner.formatTime(process.infoBean.stopTime));
                            }
                            processRow.find(".process-reported-errors").html(process.infoBean.reportedErrors.length);
                            if (process.infoBean.result) {
                                processRow.find(".process-tasks-completed").html(process.infoBean.result.tasksCompleted);
                            }
                        }
                        ScriptRunner.pollingLoop();
                    },
                    error: ScriptRunner.error,
                    data: {
                        action: "status",
                        ids: ScriptRunner.watchList
                    }
                });
            }, 250);
        }
    },
    error: function (e) {
        console.log("Error condition detected -- check logs!");
        console.log(e);
    },
    setProgress: function (progress, label, val) {
        var percent;
        percent = val * 100;
        if (percent > 0 && percent < 100) {
            progress.attr("indeterminate", null);
            progress.attr("value", percent);
        } else {
            if (val < 0) {
                label = "Working...  Please wait...";
            }
            progress.attr("indeterminate", true);
            progress.attr("value", 50);
        }
        progress[0].label.show();
        progress[0].label.innerHTML = label;
    },
    formatTime: function (ms) {
        var d = new Date(ms), now = new Date();
        if (ms <= 0) {
            return "n/a";
        } else {
            if (d.toLocaleDateString() === now.toLocaleDateString()) {
                return d.toLocaleTimeString();
            } else {
                return d.toLocaleString();
            }
        }
    },
    viewProcessCallback: function(path) {
        return function(){
            ScriptRunner.viewProcess(path);
        };        
    },
    viewProcess: function (path) {
        var iframe = "<iframe src='" + path + ".html'>";
        var diag = new Coral.Dialog().set({
            id: 'viewProcess',
            header: {
                innerHTML: 'Process Details'
            },
            content: {
                innerHTML: iframe
            },
            footer: {
                innerHTML: '<button id="okButton" is="coral-button" variant="default" coral-close>Close</button>'
            },
            closable: true,
            variant: "info"
        });
//                ScriptRunner.startDialog.classList.add("coral--dark");
//                ScriptRunner.startDialog.on("coral-overlay:open", function () {
//                    ScriptRunner.initStartDialog(ScriptRunner.startDialog);
//                });
//                ScriptRunner.startDialog.on("coral-overlay:close", function (evt) {
//                    // This event also triggers for closing sub-dialogs and tooltips
//                    if (evt.target === evt.currentTarget) {
//                        window.top.document.body.removeChild(ScriptRunner.startDialog);
//                    }
//                });
        window.top.document.body.appendChild(diag);
        diag.show();
    }

};

jQuery('#processListing').ready(function () {
    ScriptRunner.init();
});
jQuery(document).on("click", "#startProcess", ScriptRunner.showStartProgressForm);