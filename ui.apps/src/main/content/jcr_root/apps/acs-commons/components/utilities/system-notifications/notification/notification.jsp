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

    String style = properties.get("style", "green");
    if ("green".equals(style)) {
        style = "success";
    } else if ("red".equals(style)) {
        style = "error";
    } else if ("yellow".equals(style)) {
        style = "warning";
    } else if ("blue".equals(style)) {
        style = "info";
    }

    pageContext.setAttribute("style", xssAPI.encodeForHTMLAttr(style));
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
    pageContext.setAttribute("dismissible", dismissible);

    pageContext.setAttribute("dismissibleLabel",
        xssAPI.encodeForHTML(properties.get("dismissibleLabel", "Close")));


%><coral-alert
  size="L"
  variant="${style}"
  class="acsCommons-System-Notification"
  data-dismissible="${dismissible}"
  data-fn-acs-commons-system-notification-uid="${uid}">
  <coral-alert-header>${title}</coral-alert-header>
  <coral-alert-content>
    ${message}
    <% if (dismissible) { %>
        <div style="text-align:right">
          <button data-fn-acs-commons-system-notification-dismiss="${uid}" is="coral-button" variant="minimal" >${dismissibleLabel}</button>
        </div>
    <% } %>
  </coral-alert-content>
</coral-alert>