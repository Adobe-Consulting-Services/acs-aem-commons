(function(document, $, Coral){

	var FOUNDATION_CONTENT_LOADED = "foundation-contentloaded",
	INSTANT_PACKAGE_ACTION_BAR = "button.acs-commons-instant-package-action",
	CREATE_PACKAGE_BUTTON_ID = "#acs-commons-instant-package-create",
	INSTANT_PACKAGE_BUTTON_URL = "/apps/acs-commons/components/utilities/instant-package/option-selection/instantPackageButton.html",
	INSTANT_PACKAGE_OPTION_FORM_URL= "/apps/acs-commons/components/utilities/instant-package/option-selection/optionForm.html",
	PACKAGE_DOWNLOAD_ROOT_URL = "/crx/packmgr/download.jsp?_charset_=utf-8&path=",
	INSTANT_PACKAGE_REQUEST_URL = "/etc/acs-commons/instant-package/_jcr_content.package.json";

	var ui = $(window).adaptTo("foundation-ui");  
	
	$(document).ready(function(){ 
 
		var actionBar = document.querySelector("coral-actionbar");
		Coral.commons.ready(actionBar, function() {
			var $applyToActionBtn = $(INSTANT_PACKAGE_ACTION_BAR);

			if(_.isEmpty($applyToActionBtn)){
				$.ajax(INSTANT_PACKAGE_BUTTON_URL).done(function(html){
					actionBar.primary.items.add({}).appendChild($.parseHTML(html)[0]); 
				});
			}

		});

		// add the dialogs to the dom
		var applyDialog = document.querySelector("#acs-commons-instant-package");
		if(_.isEmpty(applyDialog)){
			$.ajax(INSTANT_PACKAGE_OPTION_FORM_URL).done(function(html){
				$("body").append(html);
				applyDialog = document.querySelector("#acs-commons-instant-package");
				$(applyDialog).trigger(FOUNDATION_CONTENT_LOADED);
			});
		}

	});


	var isPackageCreated = false, packageDownloadPath, optionType = "selectedResource", paths = [], data, pathList, optionEl, packEl;
	
	$(document).on("click", INSTANT_PACKAGE_ACTION_BAR , function(){
		var dialog = document.querySelector("#acs-commons-instant-package");
		dialog.show();
	
		packEl = document.getElementById("acs-commons-instant-package-create");
		optionEl = document.getElementById("acs-commons-instant-package-optiontype");
		
		// Reset package creation on option change		
		optionEl.onchange = function(e) {
			optionType = e.target.defaultValue;
			isPackageCreated = false;
			packEl.label.innerHTML = "Create Package";
			packEl.icon = "";
			
		};		
		
		var resetDialog = function() {
			ui.alert("Error", "Something went wrong while creating instant package", "error");
			packEl.disabled = false;
			packEl.label.innerHTML = "Create Package";
		};
		
		// Click handler on create package button
		dialog.on("click", CREATE_PACKAGE_BUTTON_ID , function(){
			paths = [];						
			if(isPackageCreated) {
				window.open(PACKAGE_DOWNLOAD_ROOT_URL+packageDownloadPath, "_blank");
				return;
			}
			
			// collect selected paths
			$(".foundation-collection-item.is-selected").each(function(){
				paths.push($(this).data("foundation-collection-item-id"));
				
			});

			pathList = paths.join(",");

			// prepare data to be sent
			data = "optionType=" + optionType + "&pathList=" + pathList;

			packEl.disabled = true;
			packEl.label.innerHTML = "Creating Package...";
			
			$.ajax({
				url : INSTANT_PACKAGE_REQUEST_URL,
				method :"POST",
				data : data,
				success : function(response){			
					
					try {
						
						var servletResponse = JSON.parse(response);
						if(servletResponse.status ==  "success") {
							packageDownloadPath = servletResponse.path;
							packEl.disabled = false;
							packEl.label.innerHTML = "Download Package";
							packEl.icon = "download";
							isPackageCreated = true;
							
						} else {
							resetDialog();
							
						}
					} catch (err) {
						resetDialog();
						
			        }					
					
				},
				error : function(){
					resetDialog();
					
				}
			});
			
		});
	});


})(document, Granite.$, Coral);