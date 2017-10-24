/*
 * #%L
 * ACS AEM Commons Package
 * %%
 * Copyright (C) 2017 Adobe
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

    $( document ).one('foundation-toggleable-show', '#aem-assets-show-publish-url', function(e) {
        var modalBody = $(e.target).find('.coral-Dialog-content');
        var headingFailure = Granite.I18n.get('An error occured in fetching publish URL');

        var publishUrlFrag = '/apps/acs-commons/gui/content/publishurl.html';

        var content = '<textarea class="acs-aem-commons__dam-copy-published-url__text" readonly>';
        var result =
            Granite.$.ajax({
                type : "GET",
                async : false,
                dataType : 'text',
                url : Granite.HTTP.externalize(publishUrlFrag + $(e.target).data('assetpath'))
            });
        result.done(function(text) {
            content += text + '</textarea>';
            modalBody.html(content);
        });
        result.fail(function() {
            modalBody.html('<p class="asset-publishurl-err-content">'+headingFailure+'</p>');
        });
    });

    $( document ).one('foundation-contentloaded', function(e) {
        if (!document.execCommand && !document.queryCommandSupported) {
            $('#asset-publishurl-copy-cmd').hide();
        }
        if (!document.queryCommandSupported('copy')) {
            $('#asset-publishurl-copy-cmd').hide();
        }
        $('#asset-publishurl-copy-cmd').on('click', function(e) {
            var ecData = document.querySelector('.acs-aem-commons__dam-copy-published-url__text');
            ecData.select();
            try {
                document.execCommand('copy');
            } catch (ign) {

            }
        });
    });
})(document, Granite.$);
