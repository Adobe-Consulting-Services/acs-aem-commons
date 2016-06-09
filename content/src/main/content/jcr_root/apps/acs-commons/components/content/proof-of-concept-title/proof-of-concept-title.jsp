<%@page session="false"%>
<%--
  Copyright 1997-2008 Day Management AG
  Barfuesserplatz 6, 4001 Basel, Switzerland
  All Rights Reserved.

  This software is the confidential and proprietary information of
  Day Management AG, ("Confidential Information"). You shall not
  disclose such Confidential Information and shall use it only in
  accordance with the terms of the license agreement you entered into
  with Day.

  ==============================================================================

  Title component.

  Draws a title either store on the resource or from the page

--%><%@include file="/libs/foundation/global.jsp"%>
<%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %>
<wcm:defineObjects /><%
%>
  <h1>Proof of concept</h1>
  <h2>Instance: ${properties.text}<h1>
  <h2>Sitewide: ${properties.textSitewide}</h2>
  <h2>prop1: ${prop1}</h2>

