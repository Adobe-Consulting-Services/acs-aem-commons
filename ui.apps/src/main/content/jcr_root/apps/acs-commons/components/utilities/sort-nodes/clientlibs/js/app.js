(function (window, document, $) {
    "use strict";

    $(document).on("click", ".sort-nodes-button", function (e) {
    	e.preventDefault();
        var form = $(this).closest('form');
        var data = form.serialize();
        var pathInput = $(form).find(".js-coral-pathbrowser-input");
        var path = pathInput[0].value;
        if(!path){
			pathInput.attr("required", "true");
            return;
        }
        $.ajax({
            url: path,
            type: "POST",
            data: data
        }).success(function(response){
			var changes = response.changes;
            var html = changes.reverse().map(function(c){ return "<li>" + c.argument[0] + "</li>"; } ).join("");

            var dialog = new Coral.Dialog();
            dialog.id = 'dialogSuccess';
            dialog.header.innerHTML = 'Success';
    		dialog.content.innerHTML = html;
            dialog.footer.innerHTML = '<button class="ok-button" is="coral-button" variant="primary" icon="check" coral-close>OK</button>';
            dialog.variant = 'success';
            dialog.closable = "on";
            dialog.show();

        }).error(function(response){
            var dialog = new Coral.Dialog();
            dialog.id = 'dialogFailure';
            dialog.header.innerHTML = 'Failure';
    		dialog.content.innerHTML = response.responseJSON["status.message"];
    		dialog.footer.innerHTML = '<button class="error-button" is="coral-button" variant="primary" icon="check" coral-close>OK</button>';
            dialog.variant = 'error';
            dialog.closable = "on";
            dialog.show();
        });


    });

})(window, document, $);
