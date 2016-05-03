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
package com.adobe.acs.commons.notification.inbox.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.notification.inbox.AEMInboxNotificationDetails;
import com.adobe.acs.commons.notification.inbox.AEMInboxNotificationService;
import com.adobe.granite.taskmanagement.Task;
import com.adobe.granite.taskmanagement.TaskAction;
import com.adobe.granite.taskmanagement.TaskManager;
import com.adobe.granite.taskmanagement.TaskManagerException;
import com.adobe.granite.taskmanagement.TaskManagerFactory;

@Component(
        label = "ACS AEM Commons - AEM Inbox Notification Service",
        description = "Service for sending AEM Inbox Notifications",
        immediate = false,
        metatype = false)
@Service
public class AEMInboxNotificationServiceImpl implements
        AEMInboxNotificationService {

    private static final Logger log = LoggerFactory
            .getLogger(AEMInboxNotificationServiceImpl.class);

    public static final String NOTIFICATION_TASK_TYPE = "Notification";

    @Override
    public AEMInboxNotificationDetails buildNotificationsDetails() {
        AEMInboxNotificationDetails notificationDetails = new AEMInboxNotificationDetailsImpl();

        return notificationDetails;
    }

    @Override
    public void sendAEMInboxNotification(ResourceResolver resourceResolver,
            AEMInboxNotificationDetails notificationDetails)
            throws TaskManagerException {

        TaskManager taskManager = resourceResolver.adaptTo(TaskManager.class);

        Task newTask = createTask(taskManager, notificationDetails);

        log.debug("Sending AEM Inbox Notification to {} with title {}",
                notificationDetails.getAssignee(),
                notificationDetails.getTitle());

        taskManager.createTask(newTask);
    }

    private Task createTask(TaskManager taskManager,
            AEMInboxNotificationDetails notificationDetails)
            throws TaskManagerException {
        
        Task newTask = taskManager.getTaskManagerFactory().newTask(
                NOTIFICATION_TASK_TYPE);

        newTask.setName(notificationDetails.getTitle());
        newTask.setContentPath(notificationDetails.getContentPath());
        newTask.setDescription(notificationDetails.getMessageDetails());
        newTask.setInstructions(notificationDetails.getInstructions());
        newTask.setCurrentAssignee(notificationDetails.getAssignee());

        String[] notificationAction = notificationDetails
                .getNotificationAction();
        if (notificationAction != null && notificationAction.length > 0) {
            List<TaskAction> taskActions = createTaskActionsList(
                    notificationAction, taskManager);

            newTask.setActions(taskActions);
        }

        return newTask;
    }

    private List<TaskAction> createTaskActionsList(String[] notificationAction,
            TaskManager taskManager) {

        TaskManagerFactory taskManagerFactory = taskManager
                .getTaskManagerFactory();
        List<TaskAction> taskActions = new ArrayList<TaskAction>();

        for (String action : notificationAction) {

            TaskAction newTaskAction = taskManagerFactory.newTaskAction(action);
            taskActions.add(newTaskAction);
        }

        return taskActions;
    }

    @Override
    public void sendAEMInboxNotification(ResourceResolver resourceResolver,
            List<AEMInboxNotificationDetails> notificationDetailList)
            throws TaskManagerException {

        for (AEMInboxNotificationDetails notificationDetails : notificationDetailList) {

            sendAEMInboxNotification(resourceResolver, notificationDetails);

        }

    }

}
