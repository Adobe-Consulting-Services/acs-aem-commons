(function($, $document) {
    "use strict";

    function showEditDialog() {
        var dialog = document.querySelector('#demoDialog');
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
        $document.on(eventName, '#edit-redirect-coral-table', function(e) {
          if(e.type=='coral-table:beforeroworder') {
        	  var thisElement = $(this);
          	  var rowIndex = e.detail.row.rowIndex;
          	  var beforeRow = e.detail.before;
          	  var beforeIndex;
              if(!beforeRow) {
        		  beforeIndex = $('#edit-redirect-coral-table').get(0)._items.length + 1;
        	  } else {
        		  beforeIndex =e.detail.before.rowIndex;
        	  }
        	  var source = $(e.detail.row).find('.source').data('value');
              var target = $(e.detail.row).find('.target').data('value');
        	  updateFormData($(e.detail.row));

        	  var $form = $("form.acs-redirect-rule-form");
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

    $(document).on("click", ".cq-dialog-submit", function (e) {
    	e.preventDefault();
        var $form = $(this).closest('form');
        var cmd = $(this).attr('cmd');
        var data = $form.serialize();
        var command;
        if(cmd){
            if(cmd.indexOf(":order") === 0){
            var dialog = $('#demoDialog');
            var id = dialog.find('.acs-redirect-rule-form').attr('id');
            var editRedirectTable = $('#edit-redirect-coral-table');
            var $rows = editRedirectTable.find('.coral-Table-row');
    		var tr = editRedirectTable.find('#'+id);
    		var idx = tr.index();
            if(cmd.indexOf("$prev") > 0){
                cmd = cmd.replace("$prev", idx > 0 ? idx-1 : 0);
                command = 'prev';
            } else {
                cmd = cmd.replace("$next", idx < $rows.length ? idx+1 : $rows.length);
                command = 'next';
            }
        }
            data += "&" + cmd;
        }
        var message = $(this).attr('alert');
        $.ajax({
            url: $form.attr('action'),
            type: "POST",
            data: data,
            async: false
        }).done(function(response /*json response from the Sling POST servlet*/){

        	var dialog = $('#demoDialog');
            var id = dialog.find('.acs-redirect-rule-form').attr('id');
    		dialog.find('.close-dialog-box').click();
       		var editRedirectTable = $('#edit-redirect-coral-table');

        	if(command == 'prev') {
        		var trPrev = editRedirectTable.find('#'+id);
        		if(trPrev.prev()) {
        			$('#'+id).prev().before($('#'+id));
        		}
        	} else if(command == 'next') {
        		var tr = editRedirectTable.find('#'+id);
        		if(tr.next()) {
        			$('#'+id).next().after($('#'+id));
        		}
        	}  else if(cmd == ':operation=delete'){
        		$('#'+id).remove();
        	}  else {
        	    var redirectPath = response.path;
        	    // redirectId is the node name, i.e. the last segment of the path
        	    var redirectId = redirectPath.substring(redirectPath.lastIndexOf('/') + 1);
        	    // fetch the table row and update/insert it in the table
        	    $.ajax({
                    url: redirectPath + ".html"
                }).done(function(trHtml){
                    if(response.isCreate){
                        editRedirectTable.find("tbody")[0].insertAdjacentHTML("beforeend", trHtml);
                    } else {
                        $('#'+redirectId).replaceWith(trHtml);
                    }
                });
        	}
        });
        return false;
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
        var statusCode = tr.find('.statusCode').data('value');
        var untilDate = tr.find('.untilDate').data('value');
        var cloudFront = tr.find('.cloudFront').html();
        var dialog = $('#demoDialog');
        dialog.find('.source').val(source);
        dialog.find('.target').val(target);
        var select = $('#status-code-select-box').get(0);
        select.value =statusCode;
        dialog.find('.untilDate').val(untilDate);

        dialog.find('.acs-redirect-rule-form').attr('action', path);
        dialog.find('.acs-redirect-rule-form').attr('id', name);
    }

    $(document).on("click", ".edit-redirect-rule", function (e) {
    	e.preventDefault();
         var tr = $(this).parents('.coral-Table-row');
         updateFormData(tr);
    	 showEditDialog();

    });

    $(document).on("click", ".new-redirect-rule", function (e) {
    	e.preventDefault();
        updateFormData($(this));
    	showEditDialog();

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


    $(document).ready(function() {
        var panelList = document.querySelector('#main-panel-1');
        var first = $(panelList).children('coral-panel').first();
        first.attr('selected', '');
        $('.coral-panel-stack').show();

        $("#redirect-search-box").bind("keyup keydown change", function(e) {
           var searchText = $(this).val();
           var editRedirectTable = $('#edit-redirect-coral-table');
           var rows = editRedirectTable.find('.coral-Table-row');
           $.each(rows, function(rowIndex, row) {
               var source = $(row).find('.source').data('value');
               var target  = $(row).find('.target').data('value');
               if(( source && source.toLowerCase().indexOf(searchText.toLowerCase()) != -1 ) ||
                (target && target.toLowerCase().indexOf(searchText.toLowerCase()) != -1)) {
                   $(row).show();
               } else {
                   $(row).hide();
               }
           });
        });

    });

})($, $(document));


