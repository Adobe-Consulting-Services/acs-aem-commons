package com.adobe.acs.commons.notification.inbox;

import java.io.Serializable;

public interface AEMInboxNotificationDetails extends Serializable {
    
    public String getTitle();

    public void setTitle(String title);

    public String getContentPath();

    public void setContentPath(String contentPath);

    public String getAssignee();

    public void setAssignee(String assignee);

    public String getMessageDetails();

    public void setMessageDetails(String messageDetails);

    public String[] getNotificationAction();

    public void setNotificationAction(String[] notificationAction);

    public String getInstructions();

    public void setInstructions(String instructions);
}
