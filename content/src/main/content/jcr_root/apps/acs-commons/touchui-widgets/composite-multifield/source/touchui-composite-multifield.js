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
 * Note the usage of acs-commons-nested property, value set to JSON_STORE
 *
 * <code>
 <?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
        jcr:primaryType="nt:unstructured"
        jcr:title="ACS AEM Commions Multifield TouchUI Component"
        sling:resourceType="cq/gui/components/authoring/dialog"
        helpPath="en/cq/current/wcm/default_components.html#Text">
    <content jcr:primaryType="nt:unstructured"
        sling:resourceType="granite/ui/components/foundation/container">
        <layout jcr:primaryType="nt:unstructured"
                sling:resourceType="granite/ui/components/foundation/layouts/fixedcolumns"/>
        <items jcr:primaryType="nt:unstructured">
            <column jcr:primaryType="nt:unstructured"
                    sling:resourceType="granite/ui/components/foundation/container">
                <items jcr:primaryType="nt:unstructured">
                    <fieldset jcr:primaryType="nt:unstructured"
                            jcr:title="Sample Dashboard"
                            sling:resourceType="granite/ui/components/foundation/form/fieldset">
                        <layout jcr:primaryType="nt:unstructured"
                                sling:resourceType="granite/ui/components/foundation/layouts/fixedcolumns"/>
                        <items jcr:primaryType="nt:unstructured">
                            <column jcr:primaryType="nt:unstructured"
                                    sling:resourceType="granite/ui/components/foundation/container">
                                <items jcr:primaryType="nt:unstructured">
                                    <dashboard jcr:primaryType="nt:unstructured"
                                            sling:resourceType="granite/ui/components/foundation/form/textfield"
                                            fieldDescription="Enter Dashboard name"
                                            fieldLabel="Dashboard name"
                                            name="./dashboard"/>
                                    <pages jcr:primaryType="nt:unstructured"
                                            sling:resourceType="granite/ui/components/foundation/form/multifield"
                                            class="full-width"
                                            fieldDescription="Click '+' to add a new page"
                                            fieldLabel="Pages">
                                        <field jcr:primaryType="nt:unstructured"
                                                sling:resourceType="granite/ui/components/foundation/form/fieldset"
                                                acs-commons-nested="JSON_STORE"
                                                name="./pages">
                                            <layout jcr:primaryType="nt:unstructured"
                                                    sling:resourceType="granite/ui/components/foundation/layouts/fixedcolumns"
                                                    method="absolute"/>
                                            <items jcr:primaryType="nt:unstructured">
                                                <column jcr:primaryType="nt:unstructured"
                                                        sling:resourceType="granite/ui/components/foundation/container">
                                                    <items jcr:primaryType="nt:unstructured">
                                                        <page jcr:primaryType="nt:unstructured"
                                                                sling:resourceType="granite/ui/components/foundation/form/textfield"
                                                                fieldDescription="Name of Page"
                                                                fieldLabel="Page Name"
                                                                name="./page"/>
                                                        <path jcr:primaryType="nt:unstructured"
                                                                sling:resourceType="granite/ui/components/foundation/form/pathbrowser"
                                                                fieldDescription="Select Path"
                                                                fieldLabel="Path"
                                                                name="./path"
                                                                rootPath="/content"/>
                                                    </items>
                                                </column>
                                            </items>
                                        </field>
                                    </pages>
                                </items>
                            </column>
                        </items>
                    </fieldset>
                </items>
            </column>
        </items>
    </content>
</jcr:root>
</code>
 */
(function ($, $document) {
    "use strict";

    var _ = window._, CUI = window.CUI,
        Class = window.Class;

    ACS.TouchUI.CompositeMultiField = new Class({
        toString: 'ACS TouchUI Composite Multifield',
        extend: ACS.TouchUI.Widget,

        //reads multifield data from server, creates the nested composite multifields and fills them
        addDataInFields: function () {
            var cmf = this, mNames = [],
                $fieldSets = $("[" + cmf.DATA_ACS_COMMONS_NESTED + "][class='coral-Form-fieldset']"),
                $form = $fieldSets.closest("form.foundation-form"),
                actionUrl = $form.attr("action") + ".json",
                mValues, $field, name, $multifield;

            $(".js-coral-Multifield-add").click(function(){
                $multifield = $(this).parent();

                setTimeout(function(){
                    cmf.addCompositeMultifieldRemoveListener($multifield);
                    cmf.addCompositeMultifieldValidator();
                }, 500);
            });

            if (_.isEmpty($fieldSets)) {
                return;
            }


            $fieldSets.each(function (i, fieldSet) {
                if(!cmf.isJsonStore($(fieldSet).data(cmf.ACS_COMMONS_NESTED))){
                    return;
                }

                mNames.push($(fieldSet).data("name"));
            });

            mNames = _.uniq(mNames);

            if (_.isUndefined(actionUrl)) {
                return;
            }

            //creates & fills the nested multifield with data
            function fillNestedFields($multifield, valueArr) {
                _.each(valueArr, function (record, index) {
                    $multifield.find(".js-coral-Multifield-add").click();

                    //a setTimeout may be needed
                    _.each(record, function (value, key) {
                        var $item = $multifield.find("[name='./" + key + "']").last();

                        if (_.isEmpty($item) || $item.closest('ul').hasClass('js-coral-Autocomplete-tagList')) {
                            $item = $multifield.find("ul[data-fieldname='./" + key + "']").last();

                            if (_.isEmpty($item)) {
                                return;
                            }
                        }

                        cmf.setWidgetValue($($item), value);
                    });
                });
            }

            function postProcess(data) {
                _.each(mNames, function (mName) {
                    if (_.isEmpty(mName)) {
                        return;
                    }

                    $fieldSets = $("[data-name='" + mName + "']");

                    //strip ./
                    mName = mName.substring(2);

                    mValues = data[mName];

                    if (_.isString(mValues)) {
                        mValues = [JSON.parse(mValues)];
                    }

                    _.each(mValues, function (record, i) {
                        if (!record) {
                            return;
                        }

                        if (_.isString(record)) {
                            record = JSON.parse(record);
                        }

                        _.each(record, function (rValue, rKey) {
                            $field = $($fieldSets[i]).find("[name='./" + rKey + "']").last();

                            if (_.isEmpty($field) || $field.closest('ul').hasClass('js-coral-Autocomplete-tagList')) {
                                $field = $($fieldSets[i]).find("ul[data-fieldname='./" + rKey + "']").last();
                            }

                            if (_.isArray(rValue) && !_.isEmpty(rValue)) {
                                fillNestedFields($($fieldSets[i]).find("[data-init='multifield']"), rValue);
                            } else {
                                cmf.setWidgetValue($field, rValue);
                            }
                        });
                    });
                });

                $document.trigger("touchui-composite-multifield-ready", mNames);

                cmf.addCompositeMultifieldValidator();
            }

            $.ajax(actionUrl).done(postProcess);
        },

        fillValue: function ($field, record) {
            var name = $field.attr("name"), value;

            if (!name) {
                return;
            }

            //strip ./
            if (name.indexOf("./") === 0) {
                name = name.substring(2);
            }

            value = $field.val();

            if (this.isCheckbox($field)) {
                value = $field.prop("checked") ? $field.val() : "";
            }

            if (this.isAutocomplete($field)) {
                var tags = [];
                var $tagItems = $field.closest("ul").find("li.coral-TagList-tag");
                $tagItems.each(function (k, tagItem) {
                    var $inputItem = $(tagItem).find("input[name='./" + name + "']");
                    tags[k] = $inputItem.val();
                    $inputItem.remove();
                });

                value = tags.toString();
            }

            record[name] = value;

            //remove the field, so that individual values are not POSTed
            if (!this.isAutocomplete($field)) {
                $field.remove();
            }
        },

        //for getting the nested multifield data as js objects
        getRecordFromMultiField: function ($multifield) {
            var cmf = this, $fieldSets = $multifield.find("[class='coral-Form-fieldset']"),
                records = [], record, $fields, name;

            $fieldSets.each(function (i, fieldSet) {
                $fields = $(fieldSet).find("[name]");

                record = {};

                $fields.each(function (j, field) {
                    if (!record[$(field).attr('name').substring(2)]) {
                        cmf.fillValue($(field), record);
                    }
                });

                if (!$.isEmptyObject(record)) {
                    records.push(record);
                }
            });

            return records;
        },

        //collect data from widgets in multifield and POST them to CRX as JSON
        collectDataFromFields: function () {
            var cmf = this, $form = $(cmf.getPropertiesFormSelector()),
                $fieldSets = $("[" + cmf.DATA_ACS_COMMONS_NESTED + "][class='coral-Form-fieldset']"),
                record, $fields, $field, $fieldSet, name, $nestedMultiField;

            $fieldSets.each(function (i, fieldSet) {
                $fieldSet = $(fieldSet);

                if(!cmf.isJsonStore($(fieldSet).data(cmf.ACS_COMMONS_NESTED))){
                    return;
                }

                $fields = $fieldSet.children().children(cmf.CFFW);

                record = {};

                $fields.each(function (j, field) {
                    $field = $(field);

                    //may be a nested multifield
                    $nestedMultiField = $field.find("[data-init='multifield']");

                    if ($nestedMultiField.length === 0) {
                        cmf.fillValue($field.find("[name]"), record);
                    } else {
                        name = $nestedMultiField.find("[class='coral-Form-fieldset']").data("name");

                        if (!name) {
                            return;
                        }

                        //strip ./
                        name = name.substring(2);

                        record[name] = cmf.getRecordFromMultiField($nestedMultiField);
                    }
                });

                if ($.isEmptyObject(record)) {
                    return;
                }

                //add the record JSON in a hidden field as string
                $('<input />').attr('type', 'hidden')
                    .attr('name', $(fieldSet).data("name"))
                    .attr('value', JSON.stringify(record))
                    .appendTo($form);
            });
        }
    });

    $document.ready(function () {
        var compositeMultiField = new ACS.TouchUI.CompositeMultiField();

        if (compositeMultiField.isPropertiesPage($document)) {
            compositeMultiField.addDataInFields();

            $document.on("click", "[form=cq-sites-properties-form]", function () {
                compositeMultiField.collectDataFromFields();
            });
        } else if(compositeMultiField.isPropertiesFormPage($document)) {
            compositeMultiField.addDataInFields();
            $document.on("click", ":submit[form=propertiesform]", function () {
                compositeMultiField.collectDataFromFields();
            });
        } else if (compositeMultiField.isCreatePageWizard($document)) {
            $document.on("click", ".foundation-wizard-control[type='submit']", function () {
                compositeMultiField.collectDataFromFields();
            });
        } else {
            $document.on("dialog-ready", function(){
                compositeMultiField.addDataInFields();
            });

            $document.on("click", ".cq-dialog-submit", function(){
                compositeMultiField.collectDataFromFields();
            });
        }
    });
}(jQuery, jQuery(document)));
