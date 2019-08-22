<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2015 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%><%
%><%@ page import="com.adobe.acs.commons.wcm.notifications.SystemNotifications,
        java.text.SimpleDateFormat, 
        java.util.Date" %><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%
    
    final String DATE_FORMAT = "EEE, d MMM yyyy h:mm a z";
    SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
    
    SystemNotifications systemNotifications = sling.getService(com.adobe.acs.commons.wcm.notifications.SystemNotifications.class);
    String notificationId = systemNotifications.getNotificationId(pageManager.getContainingPage(resource));
    
    pageContext.setAttribute("title", xssAPI.encodeForHTML(properties.get("jcr:title", String.class)));
    pageContext.setAttribute("style", xssAPI.encodeForHTMLAttr(properties.get("style", "green")));
    pageContext.setAttribute("uid", xssAPI.encodeForHTMLAttr(notificationId));

    Date onTime = properties.get("onTime", Date.class);
    Date offTime = properties.get("offTime", Date.class);
    String onTimeFormatted = null;
    String offTimeFormatted = null;
    
    if (onTime != null) {
        onTimeFormatted = sdf.format(onTime);
    }

    if (offTime != null) {
        offTimeFormatted = sdf.format(offTime);
    }

    String message = properties.get("jcr:description", String.class);
    message = systemNotifications.getMessage(message, onTimeFormatted, offTimeFormatted);
    pageContext.setAttribute("message", message);


    boolean dismissible = properties.get("dismissible", true);
    pageContext.setAttribute("dismissible", dismissible );

%><div class="acsCommons-System-Notification acsCommons-System-Notification--${style}"
       data-dismissible="${dismissible}"
       data-uid="${uid}">
<% if (dismissible) { %>    <a href="#" class="acsCommons-System-Notification-dismiss">Dismiss</a><% } %>
    <div class="acsCommons-System-Notification-title">${title}</div>
    <div class="acsCommons-System-Notification-message">${message}</div>
</div>