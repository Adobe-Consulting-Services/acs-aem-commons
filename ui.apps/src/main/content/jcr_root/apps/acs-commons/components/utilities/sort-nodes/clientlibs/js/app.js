(function (window, document, $) {
  "use strict";

  $(document).on("click", ".sort-nodes-button", function (e) {
    e.preventDefault();
    var form = $(this).closest("form");
    var data = form.serialize();
    var pathInput = $(form).find(".js-coral-pathbrowser-input");
    var path = pathInput[0].value;
    if (!path) {
      pathInput.attr("required", "true");
      return;
    }
    $.ajax({
      url: path,
      type: "POST",
      data: data
    })
    .then(
      function (response) {
        var data = response.data;
        var status = response.status;
        var headers = response.headers;
        var config = response.config;
        var changes = response.changes;
        var html = changes
          .reverse()
          .map(function (c) {
            return "<li>" + c.argument[0] + "</li>";
          })
          .join("");

        var dialog = new Coral.Dialog();
        dialog.id = "dialogSuccess";
        dialog.header.innerHTML = "Success";
        dialog.content.innerHTML = html;
        dialog.footer.innerHTML =
          '<button class="ok-button" is="coral-button" variant="primary" icon="check" coral-close>OK</button>';
        dialog.variant = "success";
        dialog.closable = "on";
        dialog.show();
      },
      function (error) {
        var data = error.data;
        var status = error.status;
        var message =
          "Unable to sort the selected node. Please ensure it is sortable.";

        try {
          if (
            response.responseJSON &&
            response.responseJSON["status.message"]
          ) {
            message = response.responseJSON["status.message"];
          }
        } catch (innerError) {
          // Stick w/ default message
          console.error(innerError);
        }

        var dialog = new Coral.Dialog();
        dialog.id = "dialogFailure";
        dialog.header.innerHTML = "Failure";
        dialog.content.innerHTML = message;
        dialog.footer.innerHTML =
          '<button class="error-button" is="coral-button" variant="primary" icon="check" coral-close>OK</button>';
        dialog.variant = "error";
        dialog.closable = "on";
        dialog.show();
      }
    );
  });
}(window, document, $));
