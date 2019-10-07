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
%><%@ page import="org.apache.sling.api.resource.Resource,
        java.util.List,
        com.adobe.acs.commons.wcm.notifications.SystemNotifications" %><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %><%
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %><%
%><%@ taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %><%

    SystemNotifications systemNotifications = sling.getService(SystemNotifications.class);
    List<Resource> notifications = systemNotifications.getNotifications(slingRequest, resource.getParent());

    if(notifications.size() < 1) {
        // No Content Response
        response.setStatus(204);
        return;
    }

    pageContext.setAttribute("notifications", notifications);

%><cq:includeClientLib css="acs-commons.system-notifications.notification"/>
<div id="acsCommons-System-Notifications">
    <c:forEach var="notification" items="${notifications}">
        <cq:include path="${notification.path}/jcr:content"
                    resourceType="${component.resourceType}/notification"/>
    </c:forEach>
</div>