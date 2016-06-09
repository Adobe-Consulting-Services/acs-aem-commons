$(document).on("foundation-contentloaded", function(e) {

    // container for the component styles
    var $el = $(".cq-dialog .cq-ComponentStyles");

    // Trigger the change event in any case
    // - as the select reacts on ul click or on native select
    $el.on('click', function (event) {
        if ($(event.target).data("value")) {
            $el.find(".coral-Select-select").trigger("change");
        }
    });

    // build the component style value by looping through all styles
    $el.find(".coral-Select-select").on("change", function (event) {
        var val = "";
        $el.find(".coral-Select-select option:selected").each(function() {
            var v = $(this).val();
            if (!v) return;
            if (val.length) val += " ";
            val += v;
        });
        $el.find("input[type=hidden]").val(val);
    });
});