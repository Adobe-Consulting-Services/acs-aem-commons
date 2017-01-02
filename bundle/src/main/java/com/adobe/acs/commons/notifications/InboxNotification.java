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

import aQute.bnd.annotation.ConsumerType;

import java.io.Serializable;

/**
 * Represents a Inbox Notification.
 *
 * This is a sub-set of attributes of the generic AEM Task object.
 */
@ConsumerType
public abstract class InboxNotification implements Serializable {

    /**
     * @return the notification title.
     */
    public abstract String getTitle();

    /**
     * Sets the notification title.
     * @param title the title
     */
    public abstract void setTitle(String title);

    /**
     * @return the notifications associated content path.
     */
    public abstract String getContentPath();

    /**
     * Sets the content path.
     * @param contentPath the content path
     */
    public abstract void setContentPath(String contentPath);

    /**
     * @return the principal name of the notification recipient
     */
    public abstract String getAssignee();

    /**
     * Sets the assignee.
     * @param assignee the principal name of the notification recipient
     */
    public abstract void setAssignee(String assignee);

    /**
     * @return the notification message.
     */
    public abstract String getMessage();

    /**
     * Sets the message.
     * @param message the message.
     */
    public abstract void setMessage(String message);

    /**
     * Gets the notification's actions.
     * @return the notification's actions.
     */
    public abstract String[] getNotificationActions();

    /**
     * Sets the notification's actions.
     * @param notificationActions the notification's actions.
     */
    public abstract void setNotificationActions(String... notificationActions);

    /**
     * @return the notification's instructions.
     */
    public abstract String getInstructions();

    /**
     * Sets the notification's instructions.
     * @param instructions the instructions.
     */
    public abstract void setInstructions(String instructions);
}