/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2016 Adobe
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
 */
(function(document, Granite, $) {
    "use strict";

    function activate(encodedItemPath, propertyName, path) {
        var selector = "input[name='./jcr:content/metadata/" + propertyName + "'][disabled]",
            $placeholder = $(selector),
            $container = $placeholder.parents(".foundation-field-editable, .coral-Form-fieldwrapper").first(),
            currentFieldLabel = $container.find(".coral-Form-fieldlabel").html();

        $container.load(path + ".html?item=" + encodedItemPath, function() {
            $container.find(".coral-Form-fieldlabel.acs-dam-label-replacement").html(currentFieldLabel);
            $(this).find(".foundation-field-editable").first().unwrap();
        });
    }

    $(document).on("foundation-contentloaded", function() {
        var encodedItemPath = encodeURIComponent($("#aem-assets-metadataeditor-formid").data("formid"));
        $.get(Granite.HTTP.getContextPath() + "/bin/acs-commons/dam/custom-components.json", function(data) {
            var i;

            if (data.components) {
                for (i = 0; i < data.components.length; i++) {
                    activate(encodedItemPath, data.components[i].propertyName, data.components[i].componentPath);
                }
            }
        });
    });
})(document, Granite, Granite.$);