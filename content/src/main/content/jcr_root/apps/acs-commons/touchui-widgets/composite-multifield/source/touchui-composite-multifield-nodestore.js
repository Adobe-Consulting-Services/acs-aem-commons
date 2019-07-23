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
 * Note the usage of acs-commons-nested property, value set to NODE_STORE
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
                                        sling:resourceType="granite/ui/components/foundation/container"> <items jcr:primaryType="nt:unstructured">
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
                                                acs-commons-nested="NODE_STORE"
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

    var _ = window._, Class = window.Class;

    ACS.TouchUI.NodeCompositeMultiField = new Class({
        toString: 'ACS TouchUI Composite Multifield Store as Nodes',
        extend: ACS.TouchUI.Widget,

        getMultiFieldNames: function(){
            var cmf = this, mNames = {}, mName, $multifield, $template,
                $multiTemplates = $(".js-coral-Multifield-input-template");

            $multiTemplates.each(function (i, template) {
                $template = $(template);
                $multifield = $($template.html());

                if(!cmf.isNodeStore($multifield.data(cmf.ACS_COMMONS_NESTED))){
                    return;
                }

                mName = $multifield.data("name").substring(2);

                mNames[mName] = $template.closest(".coral-Multifield");
            });

            return mNames;
        },

        buildMultiField: function(data, $multifield, mName){
            var cmf = this;

            if(_.isEmpty(mName) || _.isEmpty(data)){
                return;
            }

            _.each(data, function(value, key){
                if(key === "jcr:primaryType"){
                    return;
                }

                $multifield.find(".js-coral-Multifield-add").click();

                _.each(value, function(fValue, fKey){
                    if(fKey === "jcr:primaryType"){
                        return;
                    }

                    var $field = $multifield.find("[name='./" + fKey + "']").last();

                    if (_.isEmpty($field) || $field.closest('ul').hasClass('js-coral-Autocomplete-tagList')) {
                        $field = $multifield.find("[data-fieldname='./" + fKey + "']").last();

                        if (_.isEmpty($field)) {
                            return;
                        }
                    }

                    if(!_.isEmpty($field) && $field.siblings( "input.autocomplete-has-suggestion-btn")) {
                        cmf.setWidgetValue($field.siblings( "input.autocomplete-has-suggestion-btn"), fValue);
                    }

                    cmf.setWidgetValue($field, fValue);
                });
            });
        },

        addDataInFields: function () {
            var cmf = this, mNames = cmf.getMultiFieldNames(),
                $form = $(cmf.getPropertiesFormSelector()), $multifield,
                actionUrl = $form.attr("action") + ".infinity.json";

            $(".js-coral-Multifield-add").click(function(){
                $multifield = $(this).parent();

                setTimeout(function(){
                    cmf.addCompositeMultifieldRemoveListener($multifield);
                    cmf.addCompositeMultifieldValidator();
                }, 500);
            });

            if (_.isUndefined(actionUrl)) {
                return;
            }

            $.ajax(actionUrl).done(postProcess);

            function postProcess(data){
                _.each(mNames, function($multifield, mName){
                    cmf.buildMultiField(cmf.nestedPluck(data,mName), $multifield, mName);
                });

                $document.trigger("touchui-composite-multifield-nodestore-ready", mNames);

                cmf.addCompositeMultifieldValidator();
            }
        },

        getNodeStoreMultifields: function(){
            return $("[" + this.DATA_ACS_COMMONS_NESTED + "='" + this.NODE_STORE + "']");
        },

        collectDataFromFields: function(){
            var $multifields = this.getNodeStoreMultifields();

            if(_.isEmpty($multifields)){
                return;
            }

            var $form = $(this.getPropertiesFormSelector()), $fields,
                cmf = this;

            $multifields.each(function(counter, multifield){
                // This looks for children inside children, there is a problem if we try to put in 
                // tabs can be fixed
                //$fields = $(multifield).children().children(cmf.CFFW);
                $fields = $(multifield).find(cmf.CFFW);

                $fields.each(function (j, field) {
                    fillValue($form, $(multifield).data("name"), $(field).find("[name]").not("[name*='@']"), (counter + 1));
                });
            });

            function checkboxPrimativeType(value){
                if (typeof value != "string") {
                    return value;
                }
                switch(value){
                    case true:
                    case "true":
                        return true;
                    case false:
                    case "false":
                        return false;
                    default:
                        return value;
                }
            }

            function fillValue($form, fieldSetName, $field, counter){
                var name = $field.attr("name"), value;

                if (!name) {
                    return;
                }

                //strip ./
                if (name.indexOf("./") === 0) {
                    name = name.substring(2);
                }

                value = $field.val();

                if (cmf.isCheckbox($field)) {
                    var defaultVal = $field.parent().find("[name='./" + name + "@DefaultValue']").attr('value');
                    value = checkboxPrimativeType($field.prop("checked") ? $field.val() : (defaultVal ? defaultVal : ""));
                }

                if (cmf.isAutocomplete($field) || cmf.isTagsField($field)) {
                    var tags = [];
                    var $tagItems = $field.closest("ul").find("li.coral-TagList-tag");
                    $tagItems.each(function (k, tagItem) {
                        var $inputItem = $(tagItem).find("input[name='./" + name + "']");
                        tags[k] = $inputItem.val();
                        $inputItem.remove();
                    });
                    value = tags.toString();
                }

                //remove the field, so that individual values are not POSTed
                if (!cmf.isAutocomplete($field)) {
                    $field.remove();
                }

                $('<input />').attr('type', 'hidden')
                    .attr('name', fieldSetName + "/" + counter + "/" + name)
                    .attr('value', value)
                    .appendTo($form);
            }
        }
    });

    $document.ready(function () {
        var compositeMultiField = new ACS.TouchUI.NodeCompositeMultiField();

        if (compositeMultiField.isPropertiesPage($document)) {
            compositeMultiField.addDataInFields();

            $document.on("click", "[form=cq-sites-properties-form]", function(){
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
