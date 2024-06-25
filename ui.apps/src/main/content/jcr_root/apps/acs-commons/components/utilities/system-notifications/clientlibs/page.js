/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

$(function() {
     $('[data-fn-acs-commons-system-notification-cancel]').click(function(event) {
        var url = $(this).attr('data-fn-acs-commons-system-notification-cancel');
        event.preventDefault();
        event.stopPropagation();

        window.location.href = url;

     });

    $('[data-fn-acs-commons-system-notification-form]').submit(function(event) {
        event.preventDefault();
        event.stopPropagation();

        $.post($(this).attr('action'), $(this).serialize(), function() {
            $('body').fadeOut(500, function() {
              location.reload(true);
           });
        });
    });
});
