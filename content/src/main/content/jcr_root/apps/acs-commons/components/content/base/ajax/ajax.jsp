<%--
  ==============================================================================

  Base AJAX component

  ==============================================================================

--%><%@ include file="/libs/foundation/global.jsp" %><%
%><%@ page session="false"
           import="com.day.cq.wcm.api.WCMMode,
                   com.day.cq.wcm.api.components.IncludeOptions"%><%

    final WCMMode mode = WCMMode.fromRequest(slingRequest);
    // Use .ajax.nocache to allow matching on either ajax.jsp or ajax/nocache.jsp
    // depending on the importance of the un-cacheability of the content
    final String url = resourceResolver.map(resource.getPath()) + ".ajax.nocache.html";

%><% if(WCMMode.PREVIEW.equals(mode) || WCMMode.DISABLED.equals(mode)) { %>
    <cq:includeClientLib categories="acs-commons.components"/>
    <div data-ajax-component data-url="<%= url %>" class="acs-ajax-component"></div>
<% } else { %>
    <%-- In Authoring modes, do not bother AJAX'ing in components;
         Instead include them using the usual methods --%>
    <% IncludeOptions.getOptions(request, true).forceSameContext(true); %>
    <sling:include replaceSelectors="ajax.nocache" resource="<%= resource %>"/>
<% } %>