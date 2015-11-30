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

        setWidgetValue: function ($field, value) {
            if (_.isEmpty($field)) {
                return;
            }

            if (this.isSelectOne($field)) {
                this.setSelectOne($field, value);
            } else if (this.isCheckbox($field)) {
                this.setCheckBox($field, value);
            } else {
                $field.val(value);
            }
        },

        isJsonStore: function(name){
            return (_.isEmpty(name) || name === this.JSON_STORE);
        },

        isNodeStore: function(name){
            return (name === this.NODE_STORE);
        }
    });
}());
