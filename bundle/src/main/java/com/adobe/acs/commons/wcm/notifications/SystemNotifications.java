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

package com.adobe.acs.commons.wcm.notifications;

import aQute.bnd.annotation.ProviderType;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;

import java.util.List;

@ProviderType
public interface SystemNotifications {

    /**
     * Gets the activate notifications.
     * @param request The request object
     * @param notifications the notifications folder resource containing the notification pages
     * @return a list of active notification pages (as resources)
     */
    List<Resource> getNotifications(SlingHttpServletRequest request, 
                                    Resource notifications);

    /**
     * Gets the UID identifying this page; UID is based on path and last modified date so will change with modifications and resource moves.
     * @param notificationPage the notification Page
     * @return the uid
     */
    String getNotificationId(Page notificationPage);

    /**
     * Gets the notification message injecting the onTime into {{ onTime }} and offTime into {{ offTime }}.
     * Also converts CRLF into <br/>
     * @param message the raw message; may include HTML but CRLF are converted to <br/>
     * @param onTime the string to be injected into {{ onTime }}
     * @param offTime the string to be injected into {{ offTime }}
     * @return The formatted message
     */
    String getMessage(String message, String onTime, String offTime);
}
