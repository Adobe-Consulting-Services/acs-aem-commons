# WCM Helpers


## WCMHelper

### Purpose

Provide a simple abstraction for creating CQ-specific markup that drive component-based authoring elements like drop-targets and placeholder icons.

### Example

The mode common usecase of the WCMHelper is to control the execution of a component JSP based on it configured status.

    <%@ include file="/libs/foundation/global.jsp" %><%
    %><%@ page import="com.adobe.acs.commons.wcm.WCMHelper,
                       com.adobe.acs.commons.wcm.WCMEditType"%><%

        // Initialize component
        String title = properties.get("title", "");
        String description = properties.get("description", "");

        boolean hasTitle = title.length > 5;
        boolean hasDescription = description.length > 10;

    %><% if(WCMHelper.isNotConfigured(slingRequest, slingResponse, WCMEditType.TITLE, hasTitle, hasDescription) {
        // If (hasTitle && hasDescription) == false, then print out the Title placeholder image and stop further
        // execution of this component. This will also intelligently build out drop-targets based on the components cq:editConfigs
        return;
    } %>
    <%-- Else all conditions are true so display the component --%>
    <h2><cq:text property="title"/></h2>
    <p><cq:text property="description"/></p>



