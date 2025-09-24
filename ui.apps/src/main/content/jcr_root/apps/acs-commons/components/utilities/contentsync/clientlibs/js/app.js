/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
(function(document, $, Granite) {
    "use strict";

    var ui = $(window).adaptTo("foundation-ui");
    
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "acs-commons.dashboard.task.delete",
        handler: function(name, el, config, collection, selections) {	
            if (selections.length === 1) {
				var card = $(selections).find("coral-card");
                if(card.length > 0) {
					var taskName = card.data("host");
                    var message = "You are going to delete the following item:" +
                    "<p><b>" + taskName + "</b></p>";

                    ui.prompt("Delete Host", message, "notice", [
                        {
                            text: "Cancel"
                        },
                        {
                            text: "Delete",
                            warning: true,
                            handler: function() {
                                selections.map(function(item) {
                                    return $.ajax({
                                        type: "DELETE",
                                        url: $(item).find("coral-card").data("path")
                                    }).then(function() {
                                        window.location.reload(true);
                                    });
                                });
                            }
                        }
                    ]);
                }
            }
        }
    });

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "acs-commons.contentsync.host.create",
        handler: function(name, el, config, collection, selections) {
            var dlg = document.getElementById("modalConfigureHost");
            var form = dlg.querySelector("form");
            form.action = "/etc/replication/agents.author/*";
            form.reset();
            dlg.show();
        }
    });
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "acs-commons.contentsync.host.edit",
        handler: function(name, el, config, collection, selections) {
			var card = $(selections).find("coral-card");
            var properties = card.data("properties");
            var dlg = document.getElementById("modalConfigureHost");
            var form = dlg.querySelector("form");
            form.action = card.data("path");
            var select = dlg.querySelector("coral-select[name=\"./authType\"]");
			select.value = properties.authType;
            select.trigger("change");
            for(var i=0; i<form.elements.length;i++){
                var e = form.elements[i];
                var prefix = "./";
                if(e.name.startsWith(prefix)){
                    var key = e.name.substring(prefix.length); // remove leading './'
                    var value = properties[key];
                    if(value) e.value = value;
                }
            }
            dlg.show();
        }
    });

	document.addEventListener("DOMContentLoaded", function() {
        var dlg = document.getElementById("modalConfigureHost");
    	if(dlg) dlg.querySelector("coral-select[name=\"./authType\"]")
            .addEventListener("change", function(e) {
            	var authType = e.target.value;
            	dlg.querySelectorAll(".list-option-showhide-target").forEach(function(c){
                    if(authType === c.dataset.showhidetargetvalue){
                        c.classList.remove("hidden");
                    } else {
                        c.classList.add("hidden");
                    }
                });
        	}	
        );
        
        var workflowModelSelect = $("coral-select[name=workflowModel]");
        if(workflowModelSelect) {
	    	workflowModelSelect.get(0).value = $("input[name=defaultWorkflow]").get(0).value;
        }
    });
    

    $(window).adaptTo("foundation-registry").register("foundation.form.response.ui.error", {
        name: "foundation.default",
        handler: function(a, g, e, b, d) {
            a = "Error";
            d = "Fail to submit the form. Make sure you have permissions to run content sync.";
            ui.alert(a, d, "error");
        }
    });



})(document, Granite.$);
