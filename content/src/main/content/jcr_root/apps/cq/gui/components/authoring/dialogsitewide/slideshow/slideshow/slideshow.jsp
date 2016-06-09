<%--
  ADOBE CONFIDENTIAL

  Copyright 2013 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
--%><%@page session="false" %><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page import="com.adobe.granite.ui.components.Config,
                  org.apache.sling.api.resource.Resource,
                  java.util.Iterator" %><%

    /**
     * The Slideshow lets the user manage a list of slides.
     * A slide can be added or removed from the list. Every slide have their own <code>title</code>.
     */
  Config cfg = cmp.getConfig();

%><div class="js-Slideshow">
  <%
    for (Iterator<Resource> items = resource.listChildren(); items.hasNext();) {
      %><sling:include resource="<%=items.next() %>"/><%
    }
  %>
</div>