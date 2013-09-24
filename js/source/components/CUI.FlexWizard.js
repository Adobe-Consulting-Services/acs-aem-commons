/*
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
*/
(function($) {
    function cloneLeft(buttons) {
        return buttons.filter("[data-action=prev], [data-action=cancel]").first().addClass("hidden")
            .clone().addClass("back left").each(processButton);
    }

    function cloneRight(buttons) {
        return buttons.filter("[data-action=next]").first().addClass("hidden")
            .clone().addClass("primary right").each(processButton);
    }

    function processButton(i, el) {
        $(el).removeClass("hidden").not("button").toggleClass("button", true);
    }

    function buildNav(wizard) {
        var sections = wizard.children(".step");
        var nav;

        if (wizard.children(".toolbar").length === 0) {
            wizard.prepend(function() {
                nav = $('<nav class="toolbar"><ol class="center"></ol></nav>');
                var ol = nav.children("ol");

                sections.map(function() {
                    return $("<li />").text(this.title);
                }).appendTo(ol);

                return nav;
            });
        } else {
            nav = wizard.children("nav");
        }

        nav.find("> ol > li").first().addClass("active").append("<div class='lead-fill' />");

        var buttons = sections.first().find(".flexwizard-control");

        nav.prepend(function() {
            return cloneLeft(buttons);
        }).append(function() {
            return cloneRight(buttons);
        });
    }

    function showNav(to) {
        if (to.length === 0) return;

        to.addClass("active").removeClass("stepped");
        to.prevAll("li").addClass("stepped").removeClass("active");
        to.nextAll("li").removeClass("active stepped");
    }

    function showStep(wizard, to, from) {
        if (to.length === 0) return;

        if (from) {
            from.removeClass("active");
        }

        to.toggleClass("active", true);

        wizard.trigger("flexwizard-stepchange", [to, from]);
    }

    function controlWizard(wizard, action) {
        var nav = wizard.children("nav");
        var from = wizard.children(".step.active");
        var fromNav = nav.children("ol").children("li.active");

        var to, toNav;
        switch (action) {
            case "prev":
                to = from.prev(".step");
                toNav = fromNav.prev("li");
                break;
            case "next":
                to = from.next(".step");
                toNav = fromNav.next("li");
                break;
            case "cancel":
                return;
        }

        if (to.length === 0) return;

        var buttons = to.find(".flexwizard-control");

        cloneLeft(buttons).replaceAll(nav.children(".left"));
        cloneRight(buttons).replaceAll(nav.children(".right"));

        showNav(toNav);
        showStep(wizard, to, from);
    }

    CUI.FlexWizard = new Class({
        toString: "FlexWizard",

        extend: CUI.Widget,

        construct: function(options) {
            var wizard = this.$element;

            buildNav(wizard);

            wizard.on("click", ".flexwizard-control", function(e) {
                controlWizard(wizard, $(this).data("action"));
            });

            showStep(wizard, wizard.children(".step").first());
        }
    });

    CUI.util.plugClass(CUI.FlexWizard);

    if (CUI.options.dataAPI) {
        $(document).on("cui-contentloaded.data-api", function(e) {
            $("[data-init~=flexwizard]", e.target).flexWizard();
        });
    }
}(window.jQuery));
