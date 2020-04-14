(function ($, $document) {
    var isAllowed = true;
    var utcValueFormat = "YYYY-MM-DD[T]HH:mm:ss.SSS[Z]";
    function handleDatePickerWithTimeZone() {
        if (!isAllowed) {
            return;
        }
        $("coral-datepicker").each(function (index) {
            var displayFormat = $(this).attr('displayformat');
            if(displayFormat && displayFormat.indexOf("HH") && displayFormat.indexOf("mm")) {
                var datepickerName = $(this).attr("name");
                var datePickerValue = $(this).val();
                if (!datePickerValue) {
                    return;
                }
                $(this).attr("valueFormat", utcValueFormat);

                var datepickerTimezoneName = datepickerName + "tz";
                var selectedTimeZone = $(this).parent().find("coral-autocomplete[name='" + datepickerTimezoneName + "'] coral-autocomplete-item[selected]");
                var selectedTimeZoneValue = selectedTimeZone.val();
                var displayValue;

                if (selectedTimeZoneValue && selectedTimeZoneValue !== "Europe/London") {
                        displayValue = moment.utc(datePickerValue).tz(selectedTimeZoneValue).format(displayFormat);
                        //To show in datepicker
                        $(this).find(".coral-InputGroup-input").val(displayValue);

                        //To save the value on submit
                        $(this).attr('value',moment.utc(datePickerValue).tz(selectedTimeZoneValue).format());
                        $(this).find("input[name='" + datepickerName + "']").val(moment(datePickerValue).utc().format(utcValueFormat));
                } else {
                    if (datePickerValue.indexOf('Z') === -1) {
                        $(this).find("input[name='" + datepickerName + "']").val(datePickerValue + "Z");
                    } else {
                        $(this).find("input[name='" + datepickerName + "']").val(datePickerValue);
                    }
                }
            }
        });
    }

    $(document).on("change", "coral-datepicker", function (e) {
        var displayFormat = $(this).attr('displayformat');
        if(displayFormat && displayFormat.indexOf("HH") && displayFormat.indexOf("mm")) {
            if (!isAllowed) {
                return;
            }
            var $parent = $(this).parent();
            var $datePicker = $parent.find("coral-datepicker");
            var datePickerName = $datePicker.attr("name");
            var datePickerValue = $datePicker.val();

            if (!datePickerValue) {
                return;
            }
            $(this).attr("valueFormat", utcValueFormat);
            var timeZoneName = datePickerName + "tz";
            var timeZoneValue = $parent.find("coral-autocomplete[name='" + timeZoneName + "']").val();
            if (timeZoneValue !== "Europe/London") {
                var componentName=$("coral-dialog-content").find("[name='./sling:resourceType']").val(),
                    currentDate;
                if(typeof componentName !== "undefined") {
                    componentName = componentName.substring(componentName.lastIndexOf("/")+1);
                    if(componentName === 'event-schedule') {
                        $(this).find("input[name='" + datePickerName + "']").val(datePickerValue);
                    } else {
                        if (datePickerValue.indexOf('Z') !== -1) {
                            datePickerValue = datePickerValue.replace('Z', '');
                        }
                        currentDate = moment.tz(datePickerValue, timeZoneValue);
                        $(this).find("input[name='" + datePickerName + "']").val(currentDate.utc().format(utcValueFormat));
                    }
                } else {
                    if (datePickerValue.indexOf('Z') !== -1) {
                        datePickerValue = datePickerValue.replace('Z', '');
                    }
                    currentDate = moment.tz(datePickerValue, timeZoneValue);

                    $(this).find("input[name='" + datePickerName + "']").val(currentDate.utc().format(utcValueFormat));
                }
            } else {
                ZToAvoidSlingPostConversion = $(this).closest("coral-multifield-item").length === 0 && datePickerValue.indexOf("Z") === -1 ? "Z" : "";
                if (ZToAvoidSlingPostConversion) {
                    $(this).find("input[name='" + datePickerName + "']").val(datePickerValue + ZToAvoidSlingPostConversion);
                }
            }
          }

    });
   $(document).on("change", "coral-autocomplete.datepickertz", function (e) {
        if (!isAllowed) {
            return;
        }
        var displayValue;
        var $parent = $(this).parent();
        var $datePicker = $parent.find("coral-datepicker");
        var datePickerName = $datePicker.attr("name");
        var datePickerValue = $datePicker.find('input[type="hidden"]').val();
        var displayFormat = $datePicker.attr('displayformat');
        var timeZoneName = datePickerName + "tz";
        var timeZoneValue = $parent.find("coral-autocomplete[name='" + timeZoneName + "']").val();
        if (!datePickerValue) {
            return;
        }
        if (!moment(datePickerValue).isValid() && datePickerValue.indexOf('Z') !== -1) {
            datePickerValue = datePickerValue.replace('Z', '');
        }
        if(displayFormat && displayFormat.indexOf("HH") && displayFormat.indexOf("mm")) {
            if (timeZoneValue !== "Europe/London") {
                var utcTime = moment.tz(datePickerValue, "Europe/London");
                displayValue = utcTime.clone().tz(timeZoneValue).format(displayFormat);
                $parent.find(".coral-InputGroup-input").val(displayValue);
            } else {
                if (datePickerValue.indexOf('Z') !== -1) {
                    datePickerValue = datePickerValue.replace('Z', '');
                }
                displayValue = moment(datePickerValue).format(displayFormat);
                $parent.find(".coral-InputGroup-input").val(displayValue);
            }
        }
    });

   var targetNodes = $("coral-datepicker");
   targetNodes.each(function(index, targetNode){
       if (targetNode !== undefined) {
           if (targetNode.hasAttribute("disabled")) {
               $(targetNode).parent().find(".datepickertz").attr("disabled", "disabled");
           }
           var config = {attributes: true, childList: false, subtree: false};
           var callback = function (mutationsList, observer) {
               mutationsList.forEach(function(mutation) {
                   switch(mutation.type) {
                       case 'attributes' :
                           if (mutation.attributeName === "disabled") {
                              if (mutation.target.hasAttribute("disabled")) {
                                  $(mutation.target).parent().find(".datepickertz").attr("disabled", "disabled");
                              } else {
                                  $(mutation.target).parent().find(".datepickertz").removeAttr("disabled");
                              }
                           }
                       break;
                   }
               });
           };
           // Create an observer instance linked to the callback function
           var observer = new MutationObserver(callback);
           // Start observing the target node for configured mutations
           observer.observe(targetNode, config);
       }
   });

    var initialLoad = 1;
    initialLoadVar = true;
    Coral.commons.ready(function() {
        if(initialLoadVar) {
            handleDatePickerWithTimeZone();
            initialLoad=0;
            initialLoadVar = false;
        }
    });
    $(document).on('dialog-ready', function() {
        initialLoad = 1;
        Coral.commons.ready(function() {
            if(initialLoad) {
                handleDatePickerWithTimeZone();
                initialLoad=0;
            }
        });
    });

})(jQuery, jQuery(document));
