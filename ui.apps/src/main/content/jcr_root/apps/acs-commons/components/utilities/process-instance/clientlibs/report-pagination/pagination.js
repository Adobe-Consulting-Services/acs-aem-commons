/* ACS AEM Commons: apply the selected page size and return to the first page. */
(function () {
    "use strict";
    document.addEventListener("change", function (event) {
        var select = event.target.closest("coral-select[data-report-page-size]");
        if (select && select.value) {
            select.closest("form").submit();
        }
    });
}());
