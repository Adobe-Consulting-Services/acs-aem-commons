/*
 * #%L
 * ACS AEM Commons Bundle
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
 */
package com.adobe.acs.commons.notifications;

import java.util.List;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.granite.taskmanagement.TaskManagerException;


/**
 * OSGi Service that is used to send AEM Notifications notifications.
 */
@ProviderType
public interface InboxNotificationSender {

    /**
     * Sends a AEM inbox Notification.
     *
     * @param resourceResolver the resource resolver used to send the notification.
     * @param inboxNotification the notification to send.
     * @throws TaskManagerException
     */
    void sendInboxNotification(ResourceResolver resourceResolver,
            InboxNotification inboxNotification) throws TaskManagerException;

    /**
     * Sends multiple AEM inbox notifications.
     * @param resourceResolver the resource resolver used to send the notification.
     * @param inboxNotifications the notifications to send.
     * @throws TaskManagerException
     */
    void sendInboxNotifications(ResourceResolver resourceResolver,
            List<InboxNotification> inboxNotifications)
            throws TaskManagerException;

    /**
     * Builds an InboxNotifcation object that can be populate prior to sending.
     * @return a blank InboxNotification object.
     */
    InboxNotification buildInboxNotification();
}
