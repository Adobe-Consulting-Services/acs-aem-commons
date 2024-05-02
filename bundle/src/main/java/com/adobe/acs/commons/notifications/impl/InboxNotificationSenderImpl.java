/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.notifications.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.notifications.InboxNotification;
import com.adobe.acs.commons.notifications.InboxNotificationSender;
import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskAction;
import com.adobe.granite.taskmanagement.TaskManager;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.adobe.granite.taskmanagement.TaskManagerFactory;

/*
    ACS AEM Commons - AEM Inbox Notification Sender
    Service for sending AEM Inbox Notification
 */
@Component(service = InboxNotificationSender.class)
public class InboxNotificationSenderImpl implements InboxNotificationSender {
    private static final Logger log = LoggerFactory.getLogger(InboxNotificationSenderImpl.class);

    public static final String NOTIFICATION_TASK_TYPE = "Notification";

    @Override
    public InboxNotification buildInboxNotification() {
        return new InboxNotificationImpl();
    }

    @Override
    public void sendInboxNotification(ResourceResolver resourceResolver,
            InboxNotification inboxNotification) throws TaskManagerException {

        log.debug("Sending Inbox Notification [ {} ] to [ {} ]",
                inboxNotification.getTitle(), inboxNotification.getAssignee());

        TaskManager taskManager = resourceResolver.adaptTo(TaskManager.class);
        taskManager.createTask(createTask(taskManager, inboxNotification));
    }

    @Override
    public void sendInboxNotifications(ResourceResolver resourceResolver,
            List<InboxNotification> inboxNotifications)
            throws TaskManagerException {

        for (InboxNotification notificationDetails : inboxNotifications) {
            sendInboxNotification(resourceResolver, notificationDetails);
        }
    }

    private Task createTask(TaskManager taskManager,
            InboxNotification inboxNotification) throws TaskManagerException {

        Task newTask = taskManager.getTaskManagerFactory().newTask(NOTIFICATION_TASK_TYPE);

        newTask.setName(inboxNotification.getTitle());
        newTask.setContentPath(inboxNotification.getContentPath());
        newTask.setDescription(inboxNotification.getMessage());
        newTask.setInstructions(inboxNotification.getInstructions());
        newTask.setCurrentAssignee(inboxNotification.getAssignee());

        String[] notificationActions = inboxNotification.getNotificationActions();

        if (ArrayUtils.isNotEmpty(notificationActions)) {
            List<TaskAction> taskActions = createTaskActionsList(notificationActions, taskManager);
            newTask.setActions(taskActions);
        }

        return newTask;
    }

    private List<TaskAction> createTaskActionsList(
            String[] notificationActions, TaskManager taskManager) {

        TaskManagerFactory taskManagerFactory = taskManager.getTaskManagerFactory();
        List<TaskAction> taskActions = new ArrayList<TaskAction>();

        for (String action : notificationActions) {
            TaskAction newTaskAction = taskManagerFactory.newTaskAction(action);
            taskActions.add(newTaskAction);
        }

        return taskActions;
    }
}