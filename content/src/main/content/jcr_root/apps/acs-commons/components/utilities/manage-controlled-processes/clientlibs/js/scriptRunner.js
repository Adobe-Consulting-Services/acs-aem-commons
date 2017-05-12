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

var ScriptRunner = {
    init : function() {
//        $(".coral-Pathbrowser-picker").on("coral-pathbrowser-picker-confirm", ScriptRunner.capturePath);
        jQuery("#startButton").on("click", ScriptRunner.performMove);
        ScriptRunner.progress = jQuery("#moveProgress");
    },
    performMove : function() {
        var source,dest;
        source = document.forms[0].sourceFolder.value;
        dest = document.forms[0].destinationFolder.value;
        ScriptRunner.setProgress(-1);
        jQuery.ajax({
            url: "/bin/mcp",
            dataType:"json",
            success: ScriptRunner.pollingLoop,
            error: ScriptRunner.error,
            data: {
                action:"start",
                description:"demo",
                definition:"com.adobe.acs.commons.mcp.processes.FolderRelocator",
                source:source,
                destination:dest
            }
        });
    },
    pollingLoop: function(data) {
        ScriptRunner.status = data;
        ScriptRunner.updateProgress();
        if (data.infoBean.isRunning) {
            window.setTimeout(function() {
                console.log("polling status...");
                jQuery.ajax({
                    url: "/bin/mcp",
                    dataType:"json",
                    success: ScriptRunner.pollingLoop,
                    error: ScriptRunner.error,
                    data: {
                        action:"status",
                        id:data.id
                    }
                });
            }, 250);
        } else {
            ScriptRunner.finished();
        }
    },
    error: function() {
        ScriptRunner.progress.hide();
        console.log("Error condition detected -- check logs!");
    },
    updateProgress: function() {
        var i, total, sectionWeight, action;
        total = 0;
        sectionWeight = 1.0 / ScriptRunner.status.actions.length;
        for (i=0; i < ScriptRunner.status.actions.length; i++) {
            action = ScriptRunner.status.actions[i];
            if (action.manager.tasksAdded.value > 0) {
                total += (action.manager.tasksCompleted.value / action.manager.tasksAdded.value) * sectionWeight;
            }
        }
        console.log("Percent completion " + (total*100));
        ScriptRunner.setProgress(total);
    },
    setProgress: function(val) {
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
        ScriptRunner.progress[0].label.innerHTML=label;
    },
    finished: function() {
        ScriptRunner.progress.hide();
        console.log("DONE!");
    }
};

jQuery('#processManager').ready(function() {
    ScriptRunner.init();
});
