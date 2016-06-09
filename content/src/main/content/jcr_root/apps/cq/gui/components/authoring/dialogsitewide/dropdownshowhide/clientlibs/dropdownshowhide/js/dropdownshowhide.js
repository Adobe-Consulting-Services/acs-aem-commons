/**
 * Extension to the standard dropdown/select component. It enabled hidding/unhidding of other components based on the
 * selection made in the dropdown/select.
 *
 * How to use:
 *
 * - add the class cq-dialog-dropdown-showhide to the dropdown/select element
 * - add the data attribute cq-dialog-dropdown-showhide-target to the dropdown/select element, value should be the
 *   selector, usually a specific class name, to find all possible target elements that can be shown/hidden.
 * - add the target class to each target component that can be shown/hidden
 * - add the class hidden to each target component to make them initially hidden
 * - add the attribute showhidetargetvalue to each target component, the value should equal the value of the select
 *   option that will unhide this element.
 */
(function(document, $) {
    "use strict";

    // when dialog gets injected
    $(document).on("foundation-contentloaded", function(e) {
        // if there is already an inital value make sure the according target element becomes visible
        showHide($(".cq-dialog-dropdown-showhide", e.target));
    });

    $(document).on("selected", ".cq-dialog-dropdown-showhide", function(e) {
        showHide($(this));
    });

   function showHide(el){

       el.each(function(i, element) {

           var widget = $(element).data("select");

           if (widget) {

               // get the selector to find the target elements. its stored as data-.. attribute
               var target = $(element).data("cqDialogDropdownShowhideTarget");

               // get the selected value
               var value = widget.getValue();
               // make sure all unselected target elements are hidden.
               
               $(target).not(".hide").addClass("hide");

               // unhide the target element that contains the selected value as data-showhidetargetvalue attribute
               $(target).filter("[data-showhidetargetvalue='" + value + "']").removeClass("hide");
           }
       });
   }

})(document,Granite.$);
