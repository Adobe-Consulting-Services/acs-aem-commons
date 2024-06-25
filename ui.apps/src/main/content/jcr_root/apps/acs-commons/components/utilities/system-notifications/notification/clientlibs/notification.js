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
    var LOCAL_STORAGE_KEY = 'acs-commons-system-notifications-dismissed-uids';

    $.get('/etc/acs-commons/notifications/_jcr_content.list.html', function(html) {
        var $tmp = $('<div>').html(html),
            $notification,
            uids = localStorage.getItem(LOCAL_STORAGE_KEY) || '';
            uids = uids.split(',');

        uids.forEach(function(uid) {
            $tmp.find('[data-fn-acs-commons-system-notification-uid="'+ uid +'"]').remove();
        });

        $('body').append($tmp.html());
    });

    /* Handle dismissing of notifications */
    
    $('body').on('click', '[data-fn-acs-commons-system-notification-dismiss]', function(e) {
        e.preventDefault();

        if ($('[data-fn-acs-commons-system-notification-form]').length > 0) {
            return;
        }

        var uid = $(this).data('fn-acs-commons-system-notification-dismiss'),
            uids = localStorage.getItem(LOCAL_STORAGE_KEY) || '';

        if (uids.indexOf(uid) === -1) {
            // This notification has not been dismissed before, mark as dismissed
            localStorage.setItem(LOCAL_STORAGE_KEY, uids + "," + uid);
        } else {
            // Nothing has been dismissed, mark this notification as dismissed
            localStorage.setItem(LOCAL_STORAGE_KEY, uid);
        }

        $(this).closest('[data-fn-acs-commons-system-notification-uid]').remove();
    });
});
