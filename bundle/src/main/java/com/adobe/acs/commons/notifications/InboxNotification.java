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

import java.io.Serializable;

public interface InboxNotification extends Serializable {

    public String getTitle();

    public void setTitle(String title);

    public String getContentPath();

    public void setContentPath(String contentPath);

    public String getAssignee();

    public void setAssignee(String assignee);

    public String getMessage();

    public void setMessage(String message);

    public String[] getNotificationActions();

    public void setNotificationActions(String... notificationActions);

    public String getInstructions();

    public void setInstructions(String instructions);
}
