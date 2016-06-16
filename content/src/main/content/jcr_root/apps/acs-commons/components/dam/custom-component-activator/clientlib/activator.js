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
(function(document, $) {
    "use strict";

    function getQueryParameter(paramName) {
        var queryString = decodeURIComponent(window.location.search.substring(1)),
            queryParams = queryString.split('&'),
            queryParam,
            i;

        for (i = 0; i < queryParams.length; i++) {
            queryParam = queryParams[i].split('=');

            if (queryParam[0] === sParam) {
                return queryParam[1] === undefined ? "" : queryParam[1];
            }
        }
    }

    function activate(propertyName, path) {
        var selector = "input[name='./jcr:content/metadata/" + propertyName + "'][disabled]",
            $placeholder = $(selector),
            $container = $placeholder.parents(".foundation-field-editable").first(),
            currentFieldLabel = $container.find(".coral-Form-fieldlabel").html();

        $container.load(path + ".html?item=" + encodedItemPath, function() {
            $container.find(".coral-Form-fieldlabel").html(currentFieldLabel);
            $(this).find(".foundation-field-editable").first().unwrap();
        });
    }

    var encodedItemPath = encodeURIComponent(getQueryParameter("item"));

    $(document).on("foundation-contentloaded", function(e) {
        activate("xmpMM:History", "/apps/acs-commons/dam/content/admin/history");
        activate("xmpTPg:Fonts", "/apps/acs-commons/dam/content/admin/fonts");
        activate("xmpTPg:Colorants", "/apps/acs-commons/dam/content/admin/color-swatches");
        activate("location", "acs-commons/components/dam/asset-location-map");
    });
})(document, Granite.$);