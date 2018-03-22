/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

/**
 * Touch-UI Listeners.
 */
(function ($, $document, gAuthor) {

    "use strict";

    /**
     * Event handler for when submitting/saving the dialog.
     */
    $document.on("click", ".cq-dialog-submit", function (e) {
        var $element = $(this).parents(".cq-dialog").find(".component-cloner");
        if ($element.length) {
            e.stopPropagation();
            e.preventDefault();

            $.ajax({
                method: 'GET',
                url: $element.parents("form").attr("action") + '.clone.json',
                dataType: 'JSON',
                data: { path: $('input[name="./path"]').val() }
            }).success(function(data, status, headers, config) {
                var href = window.location.href;

                // remove any existing error query params
                href = href.replace('&componentClonerError=true', '');
                href = href.replace('?componentClonerError=true', '');

                if (data.componentClonerError) {
                    var hrefError = (window.location.href.indexOf('?') > 1 ? '&' : '?') + 'componentClonerError=true';
                    window.location.replace(hrefError);
                } else {
                    window.location.replace(href);
                }
            }).error(function(data, status, headers, config) {
                var hrefError = (window.location.href.indexOf('?') > 1 ? '&' : '?') + 'componentClonerError=true';
                window.location.replace(hrefError);
            });
        }
    });
}(jQuery, jQuery(document), Granite.author));
