/**
 * Extension to the standard dropdown/select and checkbox component. It enabled hidding/unhidding of multiple dialog fields based on the
 * selection made in the dropdown/select or on checkbox check. 
 *
 * How to use:
 * 1. Select Field
 * - add the empty property cq-dialog-dropdown-showhide to the dropdown/select element
 * - add the data attribute cq-dialog-showhide-target to the dropdown/select element, value should be the
 *   selector, usually a specific class name, to find all possible target elements that can be shown/hidden.
 * - add the target class to each target component that can be shown/hidden
 * - add the class hidden to each target component to make them initially hidden
 * - add the attribute showhidetargetvalue to each target component, the value should equal the value of the select
 *   option that will unhide this element.
 *   
 * 2. Checkbox Field
 * - add the empty property cq-dialog-checkbox-showhide to the checkbox element
 * - add the data attribute cq-dialog-showhide-target to the checkbox element, value should be the
 *   selector, usually a specific class name, to find all possible target elements that can be shown/hidden.
 * - add the target class to each target component that can be shown/hidden
 * - add the class hidden to each target component to make them initially hidden
 * - add the attribute showhidetargetvalue to each target component, the value should equal to:
 * 	 'true', if the field is to be displayed when Checkbox is selected.
 * 	 'false', if the field is to be displayed when Checkbox is unselected.
 */

(function(document, $) {
    "use strict";

    // when dialog gets injected
    $(document).on("foundation-contentloaded", function(e) {
        // if there is already an inital value make sure the according target element becomes visible
        showHide($("[data-cq-dialog-dropdown-showhide], [data-cq-dialog-checkbox-showhide]", e.target));
    });

    $(document).on("selected", "[data-cq-dialog-dropdown-showhide]", function(e) {
        showHide($(this));
    });

    $(document).on("change", "[data-cq-dialog-checkbox-showhide]", function(e) {
    	showHide($(this));
    });

    function showHide(el){

        el.each(function(i, element) {

            var target, value;
            var type = getFieldType(element);
            
            switch (type) {
		        case "select":
		        	var widget = $(element).data("select");
		        	if (widget) {	                
			        	// get the selected value
		                value =  widget.getValue();
		        	}
	                break;
		        case "checkbox":
		        	// get the selected value
		        	value =  $(element).prop('checked');
            }
            
            // get the selector to find the target elements. its stored as data-.. attribute
        	target = $(element).data("cq-dialog-showhide-target");
            
            if (target) {
            	hideUnselectedElements(target, value);
            	showTarget(target, value);
            }
        });
    }
    
    //Get type of field
    function getFieldType(element){
    	//Check if field is a checkbox
        var type = $(element).prop("type");
        if(type==="checkbox"){
        	return "checkbox";
        }
    	//Check if field is a dropdown
    	var select = $(element).hasClass("coral-Select");
    	if(select){
    		return "select";
    	}
	    
        //Check if field is a CoralUI3 checkbox
        if(element && element.tagName==="CORAL-CHECKBOX"){
        	return "checkbox";
        }
    }

    // make sure all unselected target elements are hidden.
    function hideUnselectedElements(target){
        $(target).not(".hide").each(function() {
            $(this).addClass('hide'); //If target is a container, hides the container
            $(this).closest('.coral-Form-fieldwrapper').addClass('hide'); // Hides the target field wrapper. Thus, hiding label, quicktip etc.
        });
    }
    
    // unhide the target element that contains the selected value as data-showhidetargetvalue attribute
    function showTarget(target, value){
        $(target).filter("[data-showhidetargetvalue*='" + value + "'],:has([data-showhidetargetvalue*='" + value + "'])").each(function() {
            $(this).removeClass('hide');  //If target is a container, unhides the container
            $(this).closest('.coral-Form-fieldwrapper').removeClass('hide'); // Unhides the target field wrapper. Thus, displaying label, quicktip etc.
        });
    }

})(document,Granite.$);
