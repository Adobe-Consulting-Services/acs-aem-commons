(function ($, $document) {
    var isAllowed = true;
	function handleDatePickerWithTimeZone() {
        if (!isAllowed) {
            return;
        }
        $("coral-datepicker").each(function (index) {

       if($(this).attr('displayformat') !== 'YYYY-MM-DD') {
            var defaultTimezone = "UTC+00:00";
            var datepickerName = $(this).attr("name");
            var datePickerValue = $(this).val();
            var ZToAvoidSlingPostConversion;
            if (!datePickerValue) {
                return;
            }
            $(this).attr("valueFormat", "YYYY-MM-DD[T]HH:mm:ss.000[Z]");
            var displayFormat = $(this).attr('displayformat');
            var datepickerTimezoneName = datepickerName + "tz";
            var selectedTimeZone = $(this).parent().find("coral-select[name='" + datepickerTimezoneName + "']");
            var selectedTimeZoneValue = selectedTimeZone.val();
            var hiddenInputFieldVal, displayValue;
            if (!selectedTimeZoneValue) {
                selectedTimeZone.val(defaultTimezone);
                selectedTimeZoneValue = defaultTimezone;
            }
            if (selectedTimeZoneValue === "UTC-04:00") {
                displayValue = moment.utc(datePickerValue).tz("America/New_York").format(displayFormat);
                //To show in datepicker
                $(this).find(".coral-InputGroup-input").val(displayValue);
                //To save the value on submit
                ZToAvoidSlingPostConversion = $(this).closest("coral-multifield-item").length === 0 && datePickerValue.indexOf("Z") === -1 ? "Z" : "";
                $(this).attr('value',moment.utc(datePickerValue).tz("America/New_York").format());
                $(this).find("input[name='" + datepickerName + "']").val(datePickerValue + ZToAvoidSlingPostConversion);
            } else {
                ZToAvoidSlingPostConversion = $(this).closest("coral-multifield-item").length === 0 && datePickerValue.indexOf("Z") === -1 ? "Z" : "";
                if (ZToAvoidSlingPostConversion) {
                    $(this).find("input[name='" + datepickerName + "']").val(datePickerValue + ZToAvoidSlingPostConversion);
                }
            }
       }
        });
    }

   $(document).on("change", "coral-datepicker", function (e) {
       if($(this).attr('displayformat') !== 'YYYY-MM-DD') {

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
        $(this).attr("valueFormat", "YYYY-MM-DD[T]HH:mm:ss.000[Z]");
        var timeZoneName = datePickerName + "tz";
        var timeZoneValue = $parent.find("coral-select[name='" + timeZoneName + "']").val();
        if (timeZoneValue === "UTC-04:00") {
            var another = moment(datePickerValue).utc().clone();
            var utcValue = another.tz('America/New_York', true);
            $(this).attr('value', utcValue.format());
            $(this).find("input[name='" + datePickerName + "']").val(moment(utcValue).utc().format());
        } else {
            ZToAvoidSlingPostConversion = $(this).closest("coral-multifield-item").length === 0 && datePickerValue.indexOf("Z") === -1 ? "Z" : "";
            if (ZToAvoidSlingPostConversion) {
                $(this).find("input[name='" + datePickerName + "']").val(datePickerValue + ZToAvoidSlingPostConversion);
            }
        }
       }

    });
    $(document).on("change", "coral-select.datepickertz", function (e) {
        if (!isAllowed) {
            return;
        }
        var hiddenInputFieldVal, displayValue;
        var $parent = $(this).parent();
        var $datePicker = $parent.find("coral-datepicker");
        var datePickerName = $datePicker.attr("name");
        var datePickerValue = $datePicker.find('input[type="hidden"]').val();
        var displayFormat = $datePicker.attr('displayformat');
        var timeZoneName = datePickerName + "tz";
        var timeZoneValue = $parent.find("coral-select[name='" + timeZoneName + "']").val();
        if (!datePickerValue) {
            return;
        }
        if($datePicker.attr('displayformat') !== 'YYYY-MM-DD') {
        if (timeZoneValue === "UTC-04:00") {
            var utcTime = moment(datePickerValue).utc();
			var gmtDateTime = utcTime.format(displayFormat);
            displayValue = moment.utc(gmtDateTime).tz("America/New_York").format(displayFormat);
            $parent.find(".coral-InputGroup-input").val(displayValue);
            $parent.find('coral-datepicker').attr('value', moment.utc(gmtDateTime).tz("America/New_York").format());
		  	$parent.find('coral-datepicker').find('input[type="hidden"]').val(utcTime.format());
        } else {
            displayValue = moment(datePickerValue).utc().format(displayFormat);
            $parent.find(".coral-InputGroup-input").val(displayValue);
            $parent.find('coral-datepicker').attr('value', moment(datePickerValue).utc().format());
		  $parent.find('coral-datepicker').find('input[type="hidden"]').val(moment(datePickerValue).utc().format());
        }
        }

    });
	var initialLoad = 1;

    Coral.commons.ready(function() {
        if(initialLoad) {
            handleDatePickerWithTimeZone();
            initialLoad=0;
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

