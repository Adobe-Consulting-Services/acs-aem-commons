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
        SELECTOR_FORM_CQ_DIALOG: "form.cq-dialog",
        SELECTOR_FORM_SITES_PROPERTIES: "form#cq-sites-properties-form",
        SELECTOR_FORM_CREATE_PAGE: "form.cq-siteadmin-admin-createpage",
        SELECTOR_FORM_PROPERTIES_PAGE: "form#propertiesform",


        isSelectOne: function ($field) {
            return !_.isEmpty($field) && ($field.prop("type") === "select-one");
        },

        setSelectOne: function ($field, value) {
            var select = $field.closest(".coral-Select").data("select");
            if(!select){
                var dataInit = $field.closest('.coral-Select').data('init');
                if(dataInit === 'graphiciconselect'){
                    $field.val(value);
                    $field.closest('.coral-Form-field').find('.selected-icon i').removeClass().addClass(value);
                }
            }else{
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
        
        isTagsField: function ($field) {
            return !_.isEmpty($field) && ($field.hasClass("js-TagsPickerField-tagList") || $field.closest("ul").hasClass("js-TagsPickerField-tagList"));
        },
        
        getTagsFieldName: function ($fieldWrapper) {
            return $fieldWrapper.children(".js-cq-TagsPickerField").data("property-path").substr(2);
        },

        setTagsField: function($field, value) {
        	var cmf = this;

            var tagsArray = value.split(',');

            var $tagList = CUI.Widget.fromElement(CUI.TagList,$field);

            var cuiTagList = $field.data("tagList");
            if ($tagList) {
                $(tagsArray).each(function (i, item) {
                	var tagPath = "/etc/tags/" + item.replace(":", "/");
                    $.get(tagPath + ".tag.json").done(function(data){
                        cuiTagList._appendItem( { value: data.tagID, display: data.titlePath} );
                    });
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
            } else if (this.isTagsField($field)) {
                this.setTagsField($field,value);
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
                clear($el);

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
        },

        isPropertiesPage: function($document) {
            return $document.find(this.SELECTOR_FORM_SITES_PROPERTIES).length === 1;
        },

        isPropertiesFormPage: function($document) {
            return $document.find(this.SELECTOR_FORM_PROPERTIES_PAGE).length === 1;
        },

        isCreatePageWizard: function($document) {
            return $document.find(this.SELECTOR_FORM_CREATE_PAGE).length == 1;
        },

        getPropertiesFormSelector: function() {
            return this.SELECTOR_FORM_CQ_DIALOG + "," + this.SELECTOR_FORM_SITES_PROPERTIES + "," +
                this.SELECTOR_FORM_CREATE_PAGE + "," + this.SELECTOR_FORM_PROPERTIES_PAGE;
        },

    });

    if (!ACS.TouchUI.extendedMultfield) {
        //extend otb multifield for adjusting event propagation when there are nested multifields
        //for working around the nested multifield add and reorder
        CUI.Multifield = new Class({
            toString: "Multifield",
            extend: CUI.Multifield,

            construct: function (options) {
                this.script = this.$element.find(".js-coral-Multifield-input-template:last");
            },

            _addListeners: function () {
                this.superClass._addListeners.call(this);

                //otb coral event handler is added on selector .js-coral-Multifield-add
                //any nested multifield add click events are propagated to the parent multifield;
                //to prevent adding a new composite field in both nested multifield and parent multifield
                //when user clicks on add of nested multifield, stop the event propagation to parent multifield
                this.$element.on("click", ".js-coral-Multifield-add", function (e) {
                    e.stopPropagation();
                });

                this.$element.on("drop", function (e) {
                    e.stopPropagation();
                });
            }
        });

        CUI.Widget.registry.register("multifield", CUI.Multifield);
        ACS.TouchUI.extendedMultfield = true;
    }
}());
