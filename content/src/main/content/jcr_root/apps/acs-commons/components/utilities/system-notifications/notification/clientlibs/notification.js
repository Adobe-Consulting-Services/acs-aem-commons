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
    $.get('/etc/acs-commons/notifications/_jcr_content.list.html', function(data) {
        $('body').append(data);
    });
    
    $('body').on('click', '.acsCommons-System-Notification-dismiss', function() {
        var $notification = $(this).closest('.acsCommons-System-Notification'),
            uid = $notification.data('uid'),
            dismissible = $notification.data('dismissible'),
            uids;
        
        $notification.hide();

        if (dismissible) {
            // Track dismissal
            uids = getCookieValue('acs-commons-system-notifications');
            if (uids) {
                // UIDs have been tracked
                if (uids.indexOf(uid) === -1) {
                    // This notification has not been dismissed before, mark as dismissed
                    uids = uids + "," + uid;
                }
            } else {
                // Nothing has been dismissed, mark this notification as dismissed
                uids = uid;
            }

            setSessionCookie('acs-commons-system-notifications', uids);
        }
    });
    
    function setSessionCookie(name, value) {
        document.cookie = name + '=' + value + '; expires=Tue, 01 Jan 2999 12:00:00 UTC; path=/;';
    }

    function getCookieValue(name) {
        var cookies = document.cookie.split(';'),
            i,
            cookie;
        name = name + '=';
        
        for (i = 0; i < cookies.length; i++) {
            cookie = cookies[i];
            while (cookie.charAt(0) === ' ') {
                cookie = cookie.substring(1, cookie.length);
                if (cookie.indexOf(name) === 0) {
                    return cookie.substring(name.length, cookie.length);
                }
            }
        }
        
        return null;
    } 
});