(function ($, $document) {
  "use strict";

  var registry = $(window).adaptTo("foundation-registry");
  var DIALOG_FORM_SELECTOR = "#fn-acsCommons-save_redirects";
  var DIALOG_SELECTOR = "#editRuleDialog";
  var TABLE_SELECTOR = "#edit-redirect-coral-table";
  var PREFIX_DIALOG_SELECTOR = "#editPrefixDialog";

  function showEditDialog() {
    var dialog = document.querySelector(DIALOG_SELECTOR);
    dialog.show();
  }

  function showPrefixDialog() {
    var dialog = document.querySelector(PREFIX_DIALOG_SELECTOR);
    dialog.show();
  }

  [
    "coral-table:change",
    "coral-table:beforecolumnsort",
    "coral-table:columnsort",
    "coral-table:beforecolumndrag",
    "coral-table:columndrag",
    "coral-table:rowchange",
    "coral-table:rowlock",
    "coral-table:rowunlock",
    "coral-table:roworder",
    "coral-table:beforeroworder"
  ].forEach(function (eventName) {
    $document.on(eventName, TABLE_SELECTOR, function (e) {
      if (e.type == "coral-table:beforeroworder") {
        var thisElement = $(this);
        var rowIndex = e.detail.row.rowIndex;
        var beforeRow = e.detail.before;
        var beforeIndex;
        if (!beforeRow) {
          beforeIndex = $(TABLE_SELECTOR).get(0)._items.length + 1;
        } else {
          beforeIndex = e.detail.before.rowIndex;
        }
        var source = $(e.detail.row).find(".source").data("value");
        var target = $(e.detail.row).find(".target").data("value");
        updateFormData($(e.detail.row));

        var $form = $(DIALOG_FORM_SELECTOR);
        var nextIndex = beforeIndex - 1;
        if (rowIndex == beforeIndex || rowIndex == nextIndex) {
          return false;
        } else if (rowIndex > nextIndex) {
          // dragging up ward
          // nextIndex = nextIndex - 2;
        } else if (rowIndex < nextIndex) {
          // dragging down ward
          nextIndex = nextIndex - 1;
        }
        if (nextIndex <= 0) {
          nextIndex = 0;
        }
        var payload = {
          ":http-equiv-accept": "application/json",
          ":order": nextIndex
        };
        $.ajax({
          url: $form.attr("action"),
          type: "POST",
          data: payload,
          async: false
        });
      }
    });
  });

  $(document).on("click", "button[icon='delete']", function (e) {
    e.preventDefault();
    var formId = $(this).attr("form");
    var $form = $("#" + formId);
    var buttonId = $(this).attr("id");
    var cmd = buttonId && buttonId.startsWith(":") ? buttonId : null;
    var data = $form.serialize();
    if (cmd) {
      data += "&" + cmd;
    }
    var message = $(this).attr("alert");
    $.ajax({
      url: $form.attr("action"),
      type: "POST",
      data: data,
      async: false
    }).done(function (response /*json response from the Sling POST servlet*/) {
      var dialog = $(DIALOG_SELECTOR);
      dialog.find(".close-dialog-box").click();

      var ruleId = $form.attr("ruleId");

      $("#" + ruleId).remove();
    });
    return false;
  });

  registry.register("foundation.form.response.ui.success", {
    name: "acs.redirects.update",
    handler: function (form, config, response, textStatus, xhr) {
      var dialog = $(DIALOG_SELECTOR);
      dialog.find(".close-dialog-box").click();

      var redirectPath = response.path;
      // redirectId is the node name, i.e. the last segment of the path
      var redirectId = redirectPath.substring(
        redirectPath.lastIndexOf("/") + 1
      );

      // fetch the table row and update/insert it in the table
      $.ajax({
        url: redirectPath + ".html"
      }).done(function (trHtml) {
        var tr;
        if (response.isCreate) {
          var editRedirectTable = $(TABLE_SELECTOR);
          tr = editRedirectTable
            .find("tbody")[0]
            .appendChild(document.createElement("tr"));
          tr.id = redirectId;
          tr.dataset.path = redirectPath;
        } else {
          tr = $("#" + redirectId)[0];
        }
        tr.innerHTML = trHtml;
        var ui = $(window).adaptTo("foundation-ui");
        ui.clearWait();
      });
    }
  });

  registry.register("foundation.form.response.ui.success", {
    name: "acs.prefix.update",
    handler: function (form, config, response, textStatus, xhr) {
      location.reload();
    }
  });

  $(document).on("click", ".cq-dialog-download", function (e) {
    var $form = $(this).closest("form");
    var data = new FormData($form[0]);

    $.ajax({
      url: $form.attr("action"),
      type: "POST",
      data: data,
      async: false
    });
    return false;
  });

  $(document).on("click", ".cq-dialog-publish", function (e) {
    e.preventDefault();
    var $form = $(this).closest("form");
    var data = $form.serialize();

    $.ajax({
      url: $form.attr("action"),
      type: "POST",
      data: data,
      async: true
    }).done(function (response) {
      location.reload(true);
    });
    return false;
  });

  function updateFormData(tr) {
    var path = tr.data("path");
    var name = tr.attr("id");

    var source = tr.find(".source").data("value");
    var target = tr.find(".target").data("value");
    var note = tr.find(".note").data("value");
    var statusCode = tr.find(".statusCode").data("value");
    var untilDate = tr.find(".untilDate").data("value");
    var effectiveFrom = tr.find(".effectiveFrom").data("value");
    var contextPrefixIgnored = tr.find(".contextPrefixIgnored").data("value");
    var evaluateURI = tr.find(".evaluateURI").data("value");
    var tags = tr.find(".tags").data("value");
    var cacheControlHeader = tr.find(".cacheControlHeader").data("value");
    var caseInsensitive = tr.find(".source").data("case-insensitive");

    var form = $("#editRuleDialog").find("form");
    form[0].reset();
    if (source)
      form.find('foundation-autocomplete[name="./source"]').val(source);
    if (target)
      form.find('foundation-autocomplete[name="./target"]').val(target);
    if (tags)
      form
        .find('foundation-autocomplete[name="./cq:tags"]')
        .adaptTo("foundation-field")
        .setValues(tags.split(","));
    var select = $("#status-code-select-box").get(0);
    select.value = "" + statusCode;
    form.find('coral-datepicker[name="./untilDate"]').val(untilDate);
    form.find('coral-datepicker[name="./effectiveFrom"]').val(effectiveFrom);
    form.find('input[name="./note"]').val(note);
    form.find('input[name="./cacheControlHeader"]').val(cacheControlHeader);
    var cpi = form.find('input[name="./contextPrefixIgnored"]');
    cpi.val(contextPrefixIgnored);
    cpi.prop("checked", contextPrefixIgnored);
    var evalURI = form.find('input[name="./evaluateURI"]');
    evalURI.val(evaluateURI);
    evalURI.prop("checked", evaluateURI);

    var ciInput = form.find('input[name="./caseInsensitive"]');
    if(caseInsensitive) {
      ciInput.val(caseInsensitive);
      ciInput.prop("checked", caseInsensitive);
    }
    evalURI.click(function () {
      evalURI.val(evalURI.is(":checked"));
    });

    cpi.click(function () {
      cpi.val(cpi.is(":checked"));
    });

    if (isContextPrefixEnabled()) {
      //show entire coral-checkbox
      cpi.parent().show();
    } else {
      //hide entire coral-checkbox
      cpi.parent().hide();
    }

    form.attr("action", path);
    form.attr("ruleId", name);
  }

  function updatePrefixFormData(ele) {
    console.log(ele);
    var path = ele.data("path");
    var prefix = ele.data("prefix");

    var form = $("#editPrefixDialog").find("form");
    form[0].reset();
    if (prefix)
      form.find('foundation-autocomplete[name="./contextPrefix"]').val(prefix);

    form.attr("action", path);
    form.attr("ruleId", name);
  }

  $(document).on("click", ".edit-redirect-rule", function (e) {
    e.preventDefault();
    var tr = $(this).closest("tr");
    updateFormData(tr);
    showEditDialog();
  });

  function isContextPrefixEnabled() {
    return $(".context-prefix-set")[0];
  }

  $(document).on("click", ".new-redirect-rule", function (e) {
    e.preventDefault();
    updateFormData($(this));
    showEditDialog();
  });

  $(document).on("click", ".context-prefix", function (e) {
    e.preventDefault();
    updatePrefixFormData($(this));
    showPrefixDialog();
  });

  $(document).on("click", ".acs-redirects-form-import", function (e) {
    e.preventDefault();

    var $form = $(this).closest("form");
    var data = new FormData($form[0]);
    $.ajax({
      url: $form.attr("action"),
      type: "POST",
      data: new FormData($form[0]),
      processData: false,
      contentType: false
    }).done(function (response) {
      var isErr = response.log.length;
      if (response.log.length) {
        var maxItems = 10;
        var html = "";
        if (response.log.length > maxItems) {
          html +=
            "<p>Showing " + maxItems + " of " + response.log.length + "</p>";
        }
        html += "<table is='coral-table'>";

        var arr = response.log.slice(0, maxItems);
        for (var i = 0; i < arr.length; i++) {
          var row = arr[i];
          html +=
            "<tr is='coral-table-row' class='dlg-import-cell'><td is='coral-table-cell' class='dlg-import-cell'>" +
            row.level +
            "</td><td is='coral-table-cell' class='dlg-import-cell'>" +
            row.cell +
            "</td><td is='coral-table-cell'>" +
            row.msg +
            "</td></tr>";
        }
        html += "</table>";
        html +=
          "<p><a href='" +
          response.path +
          "' target=_blank>Click to download the full log</a></p>";
        var dialog = new Coral.Dialog().set({
          header: {
            innerHTML: "Issues Importing Redirects"
          },
          content: {
            innerHTML: html
          },
          footer: {
            innerHTML:
              "<button class='ok-button' is='coral-button' variant='primary' icon='check' coral-close>OK</button>"
          },
          closable: "on",
          variant: "warning"
        });
        document.body.appendChild(dialog);

        $(dialog)
          .find(".ok-button")
          .click(function () {
            location.reload();
          });
        dialog.show().center();
      } else {
        location.reload(true);
      }
    });

    return false;
  });

  $(document).ready(function () {
    var panelList = document.querySelector("#main-panel-1");
    var first = $(panelList).children("coral-panel").first();
    first.attr("selected", "");
    $(".coral-panel-stack").show();

    var searching = false;
    var searchTimeout;

  $("#redirect-search-box").on("input", function (e) {
      var searchText = $(this).val().trim();
      var caconfig = $("input[name='caconfig']").val();
      var supportsFulltextSearch = $("input[name='fulltextSearchEnabled']").val() || false;

      if (supportsFulltextSearch) {
          var swap = $("#swap");
          var table = $("#edit-redirect-coral-table");
          var tbody = table.find("tbody");
          var tableFooter = $("#table-footer");
          var mode = table.data("mode") || "browse"; // Default mode is 'browse'

          // Clear previous timeout if it exists
          clearTimeout(searchTimeout);

          // Set a new timeout to trigger search after 2 seconds of inactivity
          searchTimeout = setTimeout(function () {
              if (searchText !== "") {
                  if (searching) { return; } // If a search is already in progress, do nothing
                  searching = true; // Set searching flag to true
                  table.removeAttr("orderable");

                   var url = "/apps/acs-commons/content/redirect-manager.search.html" + caconfig + "?term=" + searchText;
                  // Perform AJAX request for search
                  $.ajax(url).done(function (response) {
                      searching = false; // Reset searching flag once search is complete

                      tableFooter.hide();

                      if (mode !== "search") {
                          // Save the browse rows and display search results
                          swap.html(tbody.html());
                      }

                      table.data("mode", "search");
                      tbody.html(response);

                      if (tbody.children().length === 0) {
                        tbody.append(EMPTY_ROW);
                      }

                  });
              } else if (mode !== "browse") {
                  // If search text is empty and mode is not 'browse', switch back to browse mode
                  tbody.html(swap.html());
                  swap.html("");
                  table.data("mode", "browse");
                  table.attr("orderable", true);

                  tableFooter.show();

                  if (tbody.children().length === 0) {
                    tbody.append(EMPTY_ROW);
                  }
              }

          }, 150); // in ms
      } else {
          var editRedirectTable = $("#edit-redirect-coral-table");
          var rows = editRedirectTable.find("tr");
          $.each(rows, function (rowIndex, row) {
            var source = $(row).find(".source").data("value");
            var target = $(row).find(".target").data("value");
            var comment = $(row).find(".note").data("value");
            if (
              (source &&
                source.toLowerCase().indexOf(searchText.toLowerCase()) != -1) ||
              (target &&
                target.toLowerCase().indexOf(searchText.toLowerCase()) != -1) ||
              (comment &&
                comment.toLowerCase().indexOf(searchText.toLowerCase()) != -1)
            ) {
              $(row).show();
            } else {
              if (rowIndex > 0) $(row).hide();
            }
          });
      }
    });

    var EMPTY_ROW =
      '<tr is="coral-table-row" class="empty-row">' +
        '<td is="coral-table-cell" colspan="14" style="text-align: center;">No redirect rules match this search.</td>' +
      '</tr>';

    $("#status-code-select-box").change(function (e) {
      var val = $(e.target).find(":selected").val();
      if (val == 301) {
        $("#until-date-picker").val("");

        var dialog = new Coral.Dialog();
        dialog.header.innerHTML = "Warning";
        dialog.content.innerHTML =
          "Permanent Redirect Selected" +
          "<p>The HTTP 301 status code is cached in browsers with no expiry date and cannot be reverted.<br>" +
          "The Until Date was cleared because once 301 is applied, it is forever</p>";
        dialog.footer.innerHTML =
          '<button is="coral-button" variant="primary" coral-close>Ok</button>';
        dialog.variant = "warning";
        dialog.show();
      }
    });
  });

  $(document).on("click", "#addConfigurationButton", function (e) {
    e.preventDefault();
    var dialog = document.querySelector("#createDialog");
    dialog.show();
  });

  $(document).on("click", ".caconfig-configuration-submit", function (e) {
    e.preventDefault();
    var $form = $(this).closest("form");
    var action = $form.attr("action");
    var data = $form.serialize();
    $.ajax({
      url: action,
      type: "POST",
      data: data,
      async: false
    }).then(
      function (response) {
        var data = response.data;
        var status = response.status;
        var headers = response.headers;
        var config = response.config;
        location.reload(true);
      },
      function (error) {
        var data = error.data;
        var status = error.status;
        var dialog = new Coral.Dialog();
        dialog.header.innerHTML = "Failed to create configuration";
        dialog.content.innerHTML = data.responseJSON.message;
        dialog.variant = "error";
        dialog.closable = "on";
        dialog.show();
      }
    );

    return false;
  });

  $(document).on(
    "change",
    'input[name="./contextPrefixIgnored"],input[name="./evaluateURI"]',
    function (e) {
      e.preventDefault();
      $(this).val($(this).is(":checked"));
    }
  );
}($, $(document)));

