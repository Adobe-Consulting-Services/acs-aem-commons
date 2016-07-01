/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 *
 * A sample component dialog using the Touch UI Multi Field
 * Note the usage of empty valued acs-commons-nested property
 */
(function () {
    "use strict";

    if (typeof window.ACS === "undefined") {
        window.ACS = {};
    }

    if (typeof window.ACS.TouchUI === "undefined") {
        window.ACS.TouchUI = {};
    }

    ACS.TouchUI.Widget = new Class({
        toString: 'ACS TouchUI Widget Base Class',
        ACS_COMMONS_NESTED:  "acs-commons-nested",
        DATA_ACS_COMMONS_NESTED:  "data-acs-commons-nested",
        CFFW:  ".coral-Form-fieldwrapper",
        JSON_STORE: "JSON_STORE",
        NODE_STORE: "NODE_STORE",

        isSelectOne: function ($field) {
            return !_.isEmpty($field) && ($field.prop("type") === "select-one");
        },

        setSelectOne: function ($field, value) {
            var select = $field.closest(".coral-Select").data("select");

            if (select) {
                select.setValue(value);
            }
        },

        isCheckbox: function ($field) {
            return !_.isEmpty($field) && ($field.prop("type") === "checkbox");
        },

        setCheckBox: function ($field, value) {
            $field.prop("checked", $field.attr("value") === value);
        },

        isDateField: function ($field) {
            return !_.isEmpty($field) && $field.prop("type") === "hidden" && $field.parent().hasClass("coral-DatePicker");
        },

        setDateField: function ($field, value) {
            var date = moment(new Date(value));
            var $parent = $field.parent();
            $parent.find("input.coral-Textfield").val(date.format($parent.attr("data-displayed-format")));
            $field.val(date.format($parent.attr("data-stored-format")));
        },

        isRichTextField: function ($field) {
            return !_.isEmpty($field) && $field.prop("type") === "hidden" && !$field.hasClass("coral-RichText-isRichTextFlag") && $field.parent().hasClass("richtext-container");
        },

        setRichTextField: function ($field, value) {
            $field.val(value);
            $field.parent().find(".coral-RichText-editable.coral-RichText").empty().append(value);
        },

        isAutocomplete: function($field) {
            return !_.isEmpty($field) && ($field.find("ul").hasClass("js-coral-Autocomplete-tagList") || $field.closest("ul").hasClass("js-coral-Autocomplete-tagList"));
        },

        setAutocomplete: function($field,value) {
            var cmf = this;

            var tagsArray = value.split(',');

            var $tagList = CUI.Widget.fromElement(CUI.TagList,$field);

            if ($tagList) {
                $(tagsArray).each(function (i, item) {
                    var selectedItem = $field.closest(cmf.CFFW).find("li[data-value='" + item + "']");

                    $tagList._appendItem({"display": selectedItem.text(), "value": item});
                });
            }
        },

        setWidgetValue: function ($field, value) {
            if (_.isEmpty($field)) {
                return;
            }

            if (this.isSelectOne($field)) {
                this.setSelectOne($field, value);
            } else if (this.isCheckbox($field)) {
                this.setCheckBox($field, value);
            } else if (this.isRichTextField($field)) {
                this.setRichTextField($field, value);
            } else if (this.isDateField($field)) {
                this.setDateField($field, value);
            } else if (this.isAutocomplete($field)) {
                this.setAutocomplete($field,value);
            } else {
                $field.val(value);
            }
        },

        isJsonStore: function(name){
            return (_.isEmpty(name) || name === this.JSON_STORE);
        },

        isNodeStore: function(name){
            return (name === this.NODE_STORE);
        },

        addCompositeMultifieldRemoveListener: function($multifield){
            var cmf = this;

            $multifield.find(".js-coral-Multifield-remove").click(function(){
                setTimeout(function () {
                    cmf.addCompositeMultifieldValidator();
                }, 500);
            });
        },

        addCompositeMultifieldValidator: function(){
            var fieldErrorEl = $("<span class='coral-Form-fielderror coral-Icon coral-Icon--alert coral-Icon--sizeS' " +
                    "data-init='quicktip' data-quicktip-type='error' />"),
                cmf = this,
                selector = "[" + cmf.DATA_ACS_COMMONS_NESTED + "] >* input, [" + cmf.DATA_ACS_COMMONS_NESTED + "] >* textarea";

            $.validator.register({
                selector: selector,
                validate: validate,
                show: show,
                clear: clear
            });

            function validate($el){
                var $multifield = $el.closest(".coral-Multifield"),
                    $inputs = $multifield.find("input, textarea"),
                    $input, isRequired, message = null;

                $inputs.each(function(index, input){
                    $input = $(input);

                    isRequired = $input.attr("required") || ($input.attr("aria-required") === "true");

                    if (isRequired && $input.val().length === 0) {
                        $input.addClass("is-invalid");
                        message = "Please fill the required multifield items";
                    }else{
                        $input.removeClass("is-invalid");
                    }
                });

                if(message){
                    $(".cq-dialog-submit").attr("disabled", "disabled");
                }else{
                    $(".cq-dialog-submit").removeAttr("disabled");
                }

                return message;
            }

            function show($el, message){
                /* jshint validthis: true */
                this.clear($el);

                var $multifield = $el.closest(".coral-Multifield"),
                    arrow = $el.closest("form").hasClass("coral-Form--vertical") ? "right" : "top";

                fieldErrorEl.clone()
                    .attr("data-quicktip-arrow", arrow)
                    .attr("data-quicktip-content", message)
                    .insertAfter($multifield);
            }

            function clear($el){
                var $multifield = $el.closest(".coral-Multifield");
                $multifield.nextAll(".coral-Form-fielderror").tooltip("hide").remove();
            }

            validate($($(selector)[0]));
        }
    });
}());
