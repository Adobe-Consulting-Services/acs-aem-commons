(function($, $document) {
     "use strict";

	var registry = $(window).adaptTo("foundation-registry"); 
    var DIALOG_FORM_SELECTOR = "#fn-acsCommons-save_redirects";
    var DIALOG_SELECTOR = "#editRuleDialog";
    var TABLE_SELECTOR = "#edit-redirect-coral-table";

    function showEditDialog() {
        var dialog = document.querySelector(DIALOG_SELECTOR);
        dialog.show();
    }


    [
        'coral-table:change',
        'coral-table:beforecolumnsort',
        'coral-table:columnsort',
        'coral-table:beforecolumndrag',
        'coral-table:columndrag',
        'coral-table:rowchange',
        'coral-table:rowlock',
        'coral-table:rowunlock',
        'coral-table:roworder',
        'coral-table:beforeroworder',
      ]
      .forEach(function(eventName) {
        $document.on(eventName, TABLE_SELECTOR, function(e) {
          if(e.type=='coral-table:beforeroworder') {
        	  var thisElement = $(this);
          	  var rowIndex = e.detail.row.rowIndex;
          	  var beforeRow = e.detail.before;
          	  var beforeIndex;
              if(!beforeRow) {
        		  beforeIndex = $(TABLE_SELECTOR).get(0)._items.length + 1;
        	  } else {
        		  beforeIndex =e.detail.before.rowIndex;
        	  }
        	  var source = $(e.detail.row).find('.source').data('value');
              var target = $(e.detail.row).find('.target').data('value');
        	  updateFormData($(e.detail.row));

        	  var $form = $(DIALOG_FORM_SELECTOR);
              var data = $form.serialize();
              var nextIndex = beforeIndex - 1;
              if(rowIndex == beforeIndex || rowIndex == nextIndex) {
            	  return false;
              }  else if(rowIndex > nextIndex  ) { // dragging up ward
            	 // nextIndex = nextIndex - 2;
              } else if(rowIndex < nextIndex) { // dragging down ward
            	  nextIndex = nextIndex - 1;
              }
              if(nextIndex <=0) {
            	  nextIndex =0;
              }
              data += "&:order=" + nextIndex;
              $.ajax({
                url: $form.attr('action'),
                type: "POST",
                data: data,
                async: false
                });
          }
        });
      });

    $(document).on("click", "button[icon='delete']", function (e) {
    	e.preventDefault();
        var formId = $(this).attr('form');
        var $form = $("#" + formId);
        var buttonId = $(this).attr('id');
        var cmd = buttonId && buttonId.startsWith(":") ? buttonId : null;
        var data = $form.serialize();
        if(cmd){
            data += "&" + cmd;
        }
        var message = $(this).attr('alert');
        $.ajax({
            url: $form.attr('action'),
            type: "POST",
            data: data,
            async: false
        }).done(function(response /*json response from the Sling POST servlet*/){
        	var dialog = $(DIALOG_SELECTOR);
    		dialog.find('.close-dialog-box').click();

            var ruleId = $form.attr('ruleId');

       		$('#'+ruleId).remove();
        });
        return false;
     });

	registry.register("foundation.form.response.ui.success", {
        name: "acs.redirects.update",
        handler: function(form, config, response, textStatus, xhr) {
                var dialog = $(DIALOG_SELECTOR);
                dialog.find('.close-dialog-box').click();

                var redirectPath = response.path;
                // redirectId is the node name, i.e. the last segment of the path
                var redirectId = redirectPath.substring(redirectPath.lastIndexOf('/') + 1);

                // fetch the table row and update/insert it in the table
                $.ajax({
                    url: redirectPath + ".html"
                }).done(function(trHtml){
                    if(response.isCreate){
                        var editRedirectTable = $(TABLE_SELECTOR);
                        var tr = editRedirectTable.find("tbody")[0].appendChild(document.createElement('tr'));
                        $(tr).replaceWith(trHtml);
                    } else {
                        $('#'+redirectId).replaceWith(trHtml);
                    }
                    var ui = $(window).adaptTo('foundation-ui');
					ui.clearWait();

                });
        }
    }); 
    $(document).on("click", ".cq-dialog-upload", function (e) {
        var $form = $(this).closest('form');
        var data = new FormData($form[0]);

        var message = $(this).attr('alert');
        $.ajax({
            url: $form.attr('action'),
            type: "POST",
            data: data,
            cache: false,
            contentType: false,
            processData: false,
            async: false
        }).done(function(response){
           location.reload(true);
        });
        return false;
    });

    $(document).on("click", ".cq-dialog-download", function (e) {
        var $form = $(this).closest('form');
        var data = new FormData($form[0]);

        $.ajax({
            url: $form.attr('action'),
            type: "POST",
            data: data,
            async: false
        });
        return false;
    });

    $(document).on("click", ".cq-dialog-publish", function (e) {
    	e.preventDefault();
        var $form = $(this).closest('form');
        var data = $form.serialize();

        $.ajax({
            url: $form.attr('action'),
            type: "POST",
            data: data,
            async: true
        }).done(function(response){
           location.reload(true);
        });
        return false;
    });

    function updateFormData(tr) {
    	var path = tr.data('path');
   	 	var name = tr.attr('id');

        var source = tr.find('.source').data('value');
        var target = tr.find('.target').data('value');
        var note = tr.find('.note').data('value');
        var statusCode = tr.find('.statusCode').data('value');
        var untilDate = tr.find('.untilDate').data('value');

        var form = $('#editRuleDialog').find("form");
        form[0].reset();
        if(source) form.find('foundation-autocomplete[name="./source"]').val(source);
        if(target) form.find('foundation-autocomplete[name="./target"]').val(target);
        var select = $('#status-code-select-box').get(0);
        select.value =statusCode;
        form.find('coral-datepicker[name="./untilDate"]').val(untilDate);
        form.find('input[name="./note"]').val(note);

        form.attr('action', path);
        form.attr('ruleId', name);
    }

    $(document).on("click", ".edit-redirect-rule", function (e) {
    	e.preventDefault();
         var tr = $(this).closest('tr');
         updateFormData(tr);
    	 showEditDialog();

    });

    $(document).on("click", ".new-redirect-rule", function (e) {
    	e.preventDefault();
        updateFormData($(this));
    	showEditDialog();

    });



    $(document).ready(function() {
        var panelList = document.querySelector('#main-panel-1');
        var first = $(panelList).children('coral-panel').first();
        first.attr('selected', '');
        $('.coral-panel-stack').show();

        $("#redirect-search-box").bind("keyup keydown change", function(e) {
           var searchText = $(this).val();
           var editRedirectTable = $('#edit-redirect-coral-table');
           var rows = editRedirectTable.find('tr');
           $.each(rows, function(rowIndex, row) {
               var source = $(row).find('.source').data('value');
               var target  = $(row).find('.target').data('value');
               var comment  = $(row).find('.note').data('value');
               if(( source && source.toLowerCase().indexOf(searchText.toLowerCase()) != -1 ) ||
                (target && target.toLowerCase().indexOf(searchText.toLowerCase()) != -1) ||
                (comment && comment.toLowerCase().indexOf(searchText.toLowerCase()) != -1)) {
                   $(row).show();
               } else {
                   if(rowIndex > 0) $(row).hide();
               }
           });
        });

        $("#status-code-select-box").change(function (e) {
            var val = $(e.target).find(":selected").val();
            if(val == 301){
                $("#until-date-picker").val("");
    
                var dialog = new Coral.Dialog();
                dialog.header.innerHTML = 'Warning';
                dialog.content.innerHTML = "Permanent Redirect Selected" +
                    "<p>The HTTP 301 status code is cached in browsers with no expiry date and cannot be reverted.<br>"+
                       "The Until Date was cleared because once 301 is applied, it is forever</p>";
                dialog.footer.innerHTML = '<button is="coral-button" variant="primary" coral-close>Ok</button>';
                dialog.variant = 'warning';
                dialog.show();
            }
        });


    });

    $(document).on("click", "#addConfigurationButton", function (e) {
    	e.preventDefault();
        var dialog = document.querySelector('#createDialog');
        dialog.show();

    });

    $(document).on("click", ".caconfig-configuration-submit", function (e) {
    	e.preventDefault();
        var $form = $(this).closest('form');
        var action = $form.attr('action');
        var data = $form.serialize();
        $.ajax({
            url: action,
            type: "POST",
            data: data,
            async: false
        }).success(function(response /*json response from the Sling POST servlet*/){
           location.reload(true);
        }).error(function (data, status, headers, config) {
            var dialog = new Coral.Dialog();
            dialog.header.innerHTML = 'Failed to create configuration';
    		dialog.content.innerHTML = data.responseJSON.message;
            dialog.variant = 'error';
            dialog.closable = "on";
            dialog.show();
        });
        return false;
    });


})($, $(document));


