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
    SERVLET_URL: "/apps/acs-commons/content/manage-controlled-processes/jcr:content",
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
//                window.top.document.body.appendChild(ScriptRunner.startDialog);
                ScriptRunner.startDialog.show();
            }
        });
    },
    initStartDialog: function (dialog) {
        dialog.started = false;
        dialog.querySelector("coral-Icon").icon = "pausePlay";
        dialog.querySelector("#processDefinitionSelector").on("coral-selectlist:change", ScriptRunner.processDefinitionSelected);
        dialog.querySelector("#startButton").on("click", function(evt) {
            evt.preventDefault();
            if (!dialog.started) {
                dialog.started = true;
                ScriptRunner.startProcess();
            }
        });    
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
                var inputForm = window.top.jQuery("#processDefinitionInput"),
                    $html = jQuery(response);

                $html.find("#processName").text(ScriptRunner.definitionName);
                $html.find("#process").val(ScriptRunner.definition);
                $html.find("coral-icon").each(function () {
                    if (this.icon) {
                        this.classList.add("coral-Icon--" + this.icon);
                    }
                });

                inputForm.html($html).trigger("foundation-contentloaded");

            },
            data: {
                processDefinition: definition
            }
        });
    },
    startProcess: function () {
        /* Use FormData object to support file uploads */
        var data = new FormData($('#processDefinitionInput form', window.top.document)[0]);
        ScriptRunner.startDialog.header.innerHTML = 'Starting Process, please wait';
        ScriptRunner.startDialog.content.innerHTML = '<coral-masonry layout="fixed-centered" columnwidth="200" spacing="20">'+
                '<coral-masonry-item><coral-wait size="L"></coral-wait></coral-masony-item></coral-masonry-item>';
        ScriptRunner.startDialog.footer.innerHTML = "";
        jQuery.ajax({
            url: ScriptRunner.SERVLET_URL + ".start.json",
            method: "POST",
            dataType: "json",
            success: ScriptRunner.startedSuccessfully,
            error: ScriptRunner.error,
            data: data,
            /* Requires to support Form Data uploads */
            cache: false,
            contentType: false,
            processData: false
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
            url: ScriptRunner.SERVLET_URL + ".list.json",
            dataType: "json",
            success: function (response) {
                var processDom, process, i, noErrors = true, tableBody = jQuery(ScriptRunner.processTable).find("tbody");
                tableBody.empty();
                ScriptRunner.watchList = [];
                for (i = 0; i < response.length; i++) {
                    process = response[i];
                    noErrors = noErrors && ScriptRunner.cleanProcessObject(process);
                    if (!noErrors) {
                        break;
                    }
                    if (process.infoBean.isRunning) {
                        ScriptRunner.watchList.push(process.id);
                    }
                    processDom = jQuery("<tr is='coral-table-row' id='process-" + process.id + "'>" +
                            "<td is='coral-table-cell'>" + process.infoBean.name + "</td>" +
                            "<td is='coral-table-cell'>" + process.infoBean.description + "</td>" +
                            "<td is='coral-table-cell' value='"+process.infoBean.startTime+"'>" + ScriptRunner.formatTime(process.infoBean.startTime) + "</td>" +
                            "<td is='coral-table-cell' value='"+process.infoBean.stopTime+"' class='process-stop-time'>" +
                            (process.infoBean.isRunning ?
                                    "<coral-progress class='process-progress'></coral-progress>" :
                                    ScriptRunner.formatTime(process.infoBean.stopTime)
                                    ) +
                            "</td>" +
                            "<td is='coral-table-cell' class='process-tasks-completed'>" + process.infoBean.result.tasksCompleted + "</td>" +
                            "<td is='coral-table-cell' class='process-reported-errors'>" + process.infoBean.reportedErrors + "</td>" +
                            "</tr>"
                            );
                    processDom.click(ScriptRunner.viewProcessCallback(process.id, process.path));
                    tableBody.append(processDom);
                }
                if (noErrors) {
                    jQuery("#processListing").trigger("foundation-contentloaded");
                    ScriptRunner.pollingLoop();
                } else {
                    ScriptRunner.rebuildProcessList();
                }
            }
        });
    },
    cleanProcessObject: function (process) {
        if (!process || !process.infoBean) {
            return false;
        } else {
            if (!process.infoBean.result) {
                process.infoBean.result = {
                    tasksCompleted: '???'
                };
            }
            if (!process.infoBean.reportedErrors) {
                process.infoBean.reportedErrors = [];
            }
            return true;
        }
    },
    pollingLoop: function () {
        if (ScriptRunner.watchList && ScriptRunner.watchList.length > 0) {
            window.setTimeout(function () {
                console.log("polling status...");
                jQuery.ajax({
                    url: ScriptRunner.SERVLET_URL + ".status.json",
                    dataType: "json",
                    success: function (statusList) {
                        var i, process, processRow, noErrors = true;
                        ScriptRunner.watchList = [];
                        for (i = 0; i < statusList.length; i++) {
                            process = statusList[i];
                            noErrors = noErrors && ScriptRunner.cleanProcessObject(process);
                            if (!noErrors) {
                                break;
                            }
                            processRow = jQuery(ScriptRunner.processTable).find("#process-" + process.id);
                            if (process.infoBean.isRunning) {
                                ScriptRunner.watchList.push(process.id);
                                ScriptRunner.setProgress(processRow.find(".process-progress"), process.infoBean.status || "Please wait...", process.infoBean.progress);
                            } else {
                                processRow.find(".process-stop-time").html(ScriptRunner.formatTime(process.infoBean.stopTime));
                                processRow.find(".process-stop-time").attr("value", process.infoBean.stopTime);
                            }
                            processRow.find(".process-reported-errors").html(process.infoBean.reportedErrors.length);
                            processRow.find(".process-tasks-completed").html(process.infoBean.result.tasksCompleted);
                        }
                        if (noErrors) {
                            ScriptRunner.pollingLoop();
                        } else {
                            ScriptRunner.rebuildProcessList();
                        }
                    },
                    error: ScriptRunner.error,
                    data: {
                        ids: ScriptRunner.watchList
                    }
                });
            }, 500);
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
    viewProcessCallback: function (id, path) {
        return function () {
            ScriptRunner.viewProcess(id, path);
        };
    },
    viewProcess: function (processId, path) {
        jQuery.ajax({
            url: path + ".html",
            dataType: "html",
            error: ScriptRunner.error,
            success: function(response) {
                var ended = response.indexOf("Ended") > 0;
                var diag = new Coral.Dialog().set({
                    id: 'viewProcess',
                    header: {
                        innerHTML: 'Process Details'
                    },
                    content: {
                        innerHTML: response
                    },
                    footer: {
                        innerHTML: (!ended ? '<button id="haltButton" is="coral-button" variant="default">Halt</button>':'') +
                                '<button id="okButton" is="coral-button" variant="default" coral-close>Close</button>'
                    },
                    closable: true,
                    variant: "info"
                });
//                window.top.document.body.appendChild(diag);
                diag.show();                
                diag.on('click', '#haltButton', function () {
                    diag.hide();
                    ScriptRunner.haltProcess(processId);                    
                });
            }
        });
    },
    haltProcess: function (processId) {
        var haltDialog = new Coral.Dialog().set({
            id: 'haltProcessDialog',
            header: {
                innerHTML: 'Halt Process'
            },
            content: {
                innerHTML: "Do you want to proceed?"
            },
            footer: {
                innerHTML: '<button id="haltButton" is="coral-button" variant="default" coral-close>Yes</button><button id="cancelButton" is="coral-button" variant="default" coral-close>No</button>'
            },
            closable: true,
            variant: "warning"
        });
//        window.top.document.body.appendChild(haltDialog);
        haltDialog.show();
        haltDialog.on('click', '#haltButton', function () {
            jQuery.ajax({
                url: ScriptRunner.SERVLET_URL + ".halt.json",
                dataType: "json",
                error: ScriptRunner.error,
                data: {
                    id: processId
                }
            });
            haltDialog.hide();
        });
    }
};

jQuery('#processListing').ready(function () {
    ScriptRunner.init();
});
jQuery(document).on("click", "#startProcess", ScriptRunner.showStartProgressForm);
