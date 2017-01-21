/**
 * Extension to the standard checkbox component. It enabled hidding/unhidding of multiple dialog fields based on the
 * toggling of checkbox. 
 *
 * - add the class cq-dialog-checkbox-showhide to the checkbox element
 * - add the data attribute cq-dialog-checkbox-showhide-target to the checkbox element, value should be the
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
        showHide($(".cq-dialog-checkbox-showhide", e.target));
    });

    $(document).on("change", ".cq-dialog-checkbox-showhide", function(e) {
    	showHide($(this));
    });

    function showHide(el){

        el.each(function(i, element) {

            var target, value;
            
            // get the selector to find the target elements. its stored as data-.. attribute
        	target = $(element).data("cqDialogCheckboxShowhideTarget");
        	// get the selected value
        	value =  $(element).prop('checked');
            
            if (target) {
            	hideUnselectedElements(target);
            	showTarget(target, value);
            }
        });
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
    	$(target).filter("[data-showhidetargetvalue='" + value + "']").each(function() {
            $(this).removeClass('hide');  //If target is a container, unhides the container
            $(this).closest('.coral-Form-fieldwrapper').removeClass('hide'); // Unhides the target field wrapper. Thus, displaying label, quicktip etc.
        });
    }

})(document,Granite.$);
