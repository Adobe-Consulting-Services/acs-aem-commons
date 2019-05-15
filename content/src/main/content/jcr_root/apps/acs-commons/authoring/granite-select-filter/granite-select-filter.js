(function(){

  // new coral text field to be use as filter input
  function newFilterTextField() {
    var filter = new Coral.Textfield();
    filter.classList.add("coral-Form-field");
    filter.placeholder = "filter";
    return filter;
  }

  /**
   * validates some assumptions about the select element structure:
   *  1. it must have a coral-overlay child.
   *  2. coral-overlay must have a coral-selectlist child.
   *  3. coral-selectlist must have items.
   */
  function isValidSelectDomStruture(selectEl) {
    var overlay = selectEl.querySelector("coral-overlay");
    var selectList = overlay ? overlay.querySelector("coral-selectlist") : null;
    var items = selectList ? selectList.items : false;
    // coral-overlay must exist & coral-selectlist must exist & coral-selectlist must have items.
    return !!overlay && !!selectList && !!items;
  }

  /**
   * Hides all items on the selectListEl that do not conatain the filterText, ignoring case.
   * @param {*} selectListEl the "coral-selectlist" element.
   * @param {*} filterText the search string (filter).
   */
  function filterSelectList(selectListEl, filterText) {
    selectListEl.items.getAll().forEach(function (item) {
      var itemText = item.textContent;
      var bothNotEmpty = itemText && filterText;
      var match =
        bothNotEmpty &&
        itemText.toLowerCase().indexOf(filterText.toLowerCase()) > -1;
      var eitherIsEmpty = !itemText || !filterText;
      if (eitherIsEmpty || match) {
        item.show();
      } else {
        item.hide();
      }
    });
  }

  var ATTR_FILTER_READY = "data-select-filter-ready";
  /**
   * Initialize filter on the passed select element.
   */
  function initFilter(selectEl) {
    if (selectEl.hasAttribute(ATTR_FILTER_READY)) {
      return; // exit, already initialized.
    } else {
      selectEl.setAttribute(ATTR_FILTER_READY, "true");
    }
    // exit here if structure does not match our assumptions
    if (!isValidSelectDomStruture(selectEl)) {
      console.info(
        "Could not add select filter to the following coral select element. It does not follow the specified structure",
        selectEl
      );
      return;
    }
    var filter = newFilterTextField();
    var overlay = selectEl.querySelector("coral-overlay");
    var selectList = overlay.querySelector("coral-selectlist");
    // add the filter field to the beginning of the list
    selectList.items.add(filter, selectList.items.first());
    // apply filter on keyup
    filter.addEventListener("keyup", function() {
      var filterValue = filter.value;
      filterSelectList(selectList, filterValue);
    });
  }

  // listen for component initialization (AEM component dialogs or otherwise)
  $(document).on("foundation-contentloaded", function(e) {
    var container = e.target;
    $("coral-select", container)
    .filter("[data-acs-select-filter]") // only elements with attribute data-acs-select-filter
    .each(function (i, el) {
      Coral.commons.ready(el, function(selectEl) {
        initFilter(selectEl);
      });
    });
  });
})();