/*
 * #%
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2024 Adobe
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

    $( document ).one('foundation-toggleable-show', '#aem-sites-show-publish-url', function(e) {
        var modalBody = $(e.target).find('coral-dialog-content'),
            failureMessage = Granite.I18n.get('An error occurred determining the page\'s publish URLs.'),
            missingConfigMessage = Granite.I18n.get('Missing configs for the Publish URL servlet or Externalizer.'),
            publishUrl = Granite.HTTP.externalize('/apps/acs-commons/components/utilities/sites-publish-url.txt'),
            path = $(e.target).data('assetpath');

        var result =
            Granite.$.ajax({
                type : "GET",
                //async : false,
                dataType : 'text',
                data : {
                    path : path
                },
                url : publishUrl
            });
        result.done(function(text) {
            var jsonResponse = JSON.parse(text);
            var content = '';
            var labelWidth = 0;
            var inputWidth = 0;
            if (jsonResponse.size === 0) {
                modalBody.html('<p class="acs-aem-commons__sites-copy-published-url__text--failure">' + missingConfigMessage + '</p>');
                return;
            }

            Object.keys(jsonResponse).forEach(function(key) {
                if (key.length > labelWidth) {
                    labelWidth = key.length;
                }
                if (jsonResponse[key].length > inputWidth) {
                    inputWidth = jsonResponse[key].length;
                }
            });
            inputWidth = inputWidth > 0 ? inputWidth - 10 : 0;

            Object.keys(jsonResponse).forEach(function(key) {
                content += '<div class="coral-Form-fieldwrapper copy-publish-url-group">' +
                    '<label class="coral-Form-fieldlabel" style="width: ' + labelWidth + 'ch; display: inline-block">' + key + ' : </label>' +
                    '<input type="text" class="coral-Form-field" value="' + jsonResponse[key] + '" readonly style="width: ' + inputWidth + 'ch;" />' +
                    '<button type="button" class="sites-publishurl-copy-cmd coral3-Button coral3-Button--primary" data-copy-target="' + key + '"><coral-icon class="coral3-Icon coral3-Icon--attach coral3-Icon--sizeXS" icon="attach" size="XS" autoarialabel="off" alt=""></coral-icon><coral-button-label>Copy</coral-button-label></button>' +
                    '</div>';
            });
            modalBody.html(content);
            document.querySelectorAll('.sites-publishurl-copy-cmd').forEach(function(button) {
                button.addEventListener('click', function() {
                    var key = this.getAttribute('data-copy-target');
                    var inputField = this.previousElementSibling;
                    var textToCopy = inputField.value;
                    inputField.select();
                    try {
                        navigator.clipboard.writeText(textToCopy);
                        console.log("Text copied to clipboard");
                    } catch (err) {
                        console.error("Failed to copy: ", err);
                    }
                });
            });
        });
        result.fail(function() {
            modalBody.html('<p class="acs-aem-commons__sites-copy-published-url__text--failure">' + failureMessage + '</p>');
        });
    });

    $( document ).one('foundation-contentloaded', function(e) {
        if (!document.execCommand && !document.queryCommandSupported) {
            $('#sites-publishurl-copy-cmd').hide();
        }
        if (!document.queryCommandSupported('copy')) {
            $('#sites-publishurl-copy-cmd').hide();
        }
    });
})(document, Granite.$);
