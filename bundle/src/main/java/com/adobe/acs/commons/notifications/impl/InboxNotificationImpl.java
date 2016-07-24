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
package com.adobe.acs.commons.notifications.impl;

import com.adobe.acs.commons.notifications.InboxNotification;
import org.apache.commons.lang.ArrayUtils;

import java.util.Arrays;

public class InboxNotificationImpl extends InboxNotification {

    private static final long serialVersionUID = -5976192100927192675L;

    private String title;

    private String contentPath;

    private String assignee;

    private String message;

    private String[] notificationActions;

    private String instructions;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentPath() {
        return contentPath;
    }

    public void setContentPath(String contentPath) {
        this.contentPath = contentPath;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String[] getNotificationActions() {
        if (notificationActions == null) {
            return ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            return Arrays.copyOf(notificationActions, notificationActions.length);
        }
    }

    public void setNotificationActions(String... notificationActions) {
        if (notificationActions == null) {
            this.notificationActions = ArrayUtils.EMPTY_STRING_ARRAY;
        } else {
            this.notificationActions = Arrays.copyOf(notificationActions, notificationActions.length);
        }
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

}
