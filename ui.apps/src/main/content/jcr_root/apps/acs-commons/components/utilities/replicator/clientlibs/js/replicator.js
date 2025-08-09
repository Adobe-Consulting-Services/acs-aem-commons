(function(document, $, Granite) {
    "use strict";

    var ui = $(window).adaptTo("foundation-ui");
    
    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "acs-commons.replicator.agent.delete",
        handler: function(name, el, config, collection, selections) {	
            if (selections.length === 1) {
				var card = $(selections).find("coral-card");
                if(card.length > 0) {
                    var properties = card.data("properties");
                    var message = "You are going to delete the following item:" +
                    "<p><b>" + properties["jcr:title"] + "</b></p>";

                    ui.prompt("Delete Replication Agent", message, "notice", [
                        {
                            text: "Cancel"
                        },
                        {
                            text: "Delete",
                            warning: true,
                            handler: function() {
                                selections.map(function(item) {
                                    var path = $(item).find("coral-card").data("path");
                                    return $.ajax({
                                        type: "DELETE",
                                        url: "/replicator/configure/resource?path=" + encodeURIComponent(path)
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
        name: "acs-commons.replicator.agent.edit",
        handler: function(name, el, config, collection, selections) {
            if (selections.length === 1) {
				var card = $(selections).find("coral-card");
                if(card.length > 0) {
                    var properties = card.data("properties");
                    var dlg = document.getElementById('configureAgentDialog');
                    var form = dlg.querySelector("form");
                    form[":action"].value = card.data("path");
                    var authType = dlg.querySelector("coral-select[name=\"./jcr:content/authType\"]");
                    authType.value = properties.authType;
                    authType.trigger("change");

                    var accessTokenProviderName = dlg.querySelector("coral-select[name=\"./jcr:content/accessTokenProviderName\"]");
                    accessTokenProviderName.value = properties.accessTokenProviderName;
                    accessTokenProviderName.trigger("change");

                    for(var i = 0; i < form.elements.length; i++){
                        var e = form.elements[i];
                        var prefix = "./jcr:content/";
                        if(e.name.startsWith(prefix)){
                            var key = e.name.substring(prefix.length); // remove leading './jcr:content/'
                            var value = properties[key];
                            if(value) e.value = value;
                        }
                    }
                    dlg.show();
                }
            }
        }
    });

    $(window).adaptTo("foundation-registry").register("foundation.collection.action.action", {
        name: "acs-commons.replicator.agent.create",
        handler: function(name, el, config, collection, selections) {
            var dlg = document.getElementById('configureAgentDialog');
            var form = dlg.querySelector("form");
            var select = dlg.querySelector("coral-select[name=\"./jcr:content/authType\"]");
            select.value = "basic";
            select.trigger("change");
            form[":action"].value = "/etc/replication/agents.author/*";
            form.reset();
            dlg.show();
        }
    });

	document.addEventListener("DOMContentLoaded", function() {
        var dlg = document.getElementById("configureAgentDialog");
    	if(dlg) dlg.querySelector("coral-select[name=\"./jcr:content/authType\"]")
            .addEventListener("change", function(e) {
            	var authType = e.target.value;
            	dlg.querySelectorAll(".list-option-showhide-target").forEach(function(c){
                    if(authType === c.dataset.showhidetargetvalue){
                        c.classList.remove("hidden");
                    } else {
                        c.classList.add("hidden");
                    }
                });
                dlg.querySelector("input[name=\"./jcr:content/enableOauth\"]").value = authType == "oauth";
        	}
        );
		if(dlg){
        	var form = dlg.querySelector("form");
        	form.addEventListener("submit", function(e) {
                form[":nameHint"].value = form["./jcr:content/jcr:title"].value;
        	});
		}

        var publishCheckbox = document.querySelector("input[name=publish]");
        if(publishCheckbox){
            publishCheckbox.addEventListener("click", function(e) {
            	var notice = publishCheckbox.form.querySelector(".notice-wide");
				notice.classList.toggle("hidden");
        	});
        }
    });

	$(window).adaptTo("foundation-registry").register("foundation.validation.validator", {
        selector: "input[name=\"./jcr:content/transportUri\"]",
        validate: function(el) {


            if (!el.value.endsWith("/bin/receive")) {
                return "The url must end with /bin/receive and point to the replication receiver servlet, e.g. https://author-p00000-e111111.adobeaemcloud.com/bin/receive";
            }
        }
   });
})(document, Granite.$);
