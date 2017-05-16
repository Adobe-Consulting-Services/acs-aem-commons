/**
 * Extension to the standard dropdown/select component. It enabled hidding/unhidding of multiple dialog fields based on the
 * selection made in the dropdown/select. 
 *
 * How to use:
 * - add the class cq-dialog-dropdown-showhide-extended to the dropdown/select element
 * - add the data attribute cq-dialog-dropdown-showhide-target to the dropdown/select element, value should be the
 *   selector, usually a specific class name, to find all possible target elements that can be shown/hidden.
 * - add the target class to each target component that can be shown/hidden
 * - add the class hidden to each target component to make them initially hidden
 * - add the attribute showhidetargetvalue to each target component, the value should equal the value of the select
 *   option that will unhide this element.
 *   
 */
(function(document, $) {
    "use strict";

    // when dialog gets injected
    $(document).on("foundation-contentloaded", function(e) {
        // if there is already an inital value make sure the according target element becomes visible
        showHide($(".cq-dialog-dropdown-showhide-extended", e.target));
    });

    $(document).on("selected", ".cq-dialog-dropdown-showhide-extended", function(e) {
        showHide($(this));
    });

    function showHide(el){

        el.each(function(i, element) {

            var target, value;
            
        	var widget = $(element).data("select");
        	if (widget) {
                // get the selector to find the target elements. its stored as data-.. attribute
	        	target = $(element).data("cqDialogDropdownShowhideTarget");
	        	// get the selected value
                value =  widget.getValue();
        	}

            if (target) {
            	hideUnselectedElements(target);
            	showTarget(target, value);
            }
        });
    }

    // make sure all unselected target elements are hidden.
    function hideUnselectedElements(target){
        $(target).not(".hide").each(function() {
            $(this).addClass('hide'); //If target is a container, it hides the container
            $(this).closest('.coral-Form-fieldwrapper').addClass('hide'); // Hides the target field wrapper. Thus, hiding label, quicktip etc.
        });
    }
    
    // unhide the target element that contains the selected value as data-showhidetargetvalue attribute
    function showTarget(target, value){
    	$(target).filter("[data-showhidetargetvalue='" + value + "']").each(function() {
            $(this).removeClass('hide'); //If target is a container, it displays the container
            $(this).closest('.coral-Form-fieldwrapper').removeClass('hide'); // Displays the target field wrapper. Thus, displaying label, quicktip etc.
        });
    }

})(document,Granite.$);
