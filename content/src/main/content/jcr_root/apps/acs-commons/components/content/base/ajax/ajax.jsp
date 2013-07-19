<%--
  ==============================================================================

  Base AJAX component

  ==============================================================================

--%><%@ include file="/libs/foundation/global.jsp" %><%
%><%@ page session="false"
           import="com.day.cq.wcm.api.WCMMode,
                   com.day.cq.wcm.api.components.IncludeOptions,
                   org.apache.commons.lang.StringUtils"%><%

    final String DEFAULT_SELECTOR = "ajax";
    final String DEFAULT_EXTENSION = "html";

    final String CN_AJAX_SELECTOR = "ajaxSelectors";
    final String CN_AJAX_EXTENSION = "ajaxExtension";

    final WCMMode mode = WCMMode.fromRequest(slingRequest);
    final ValueMap componentProperties = component.getProperties();

    String ajaxSelectors = StringUtils.stripToEmpty(componentProperties.get(CN_AJAX_SELECTOR, DEFAULT_SELECTOR));
    String ajaxExtension = StringUtils.stripToEmpty(componentProperties.get(CN_AJAX_EXTENSION, DEFAULT_EXTENSION));
    if(StringUtils.isBlank(ajaxSelectors)) { ajaxSelectors = DEFAULT_SELECTOR; }
    if(StringUtils.isBlank(ajaxExtension)) { ajaxExtension = DEFAULT_EXTENSION; }

    final String url = resourceResolver.map(resource.getPath()) + "." + ajaxSelectors + "." + ajaxExtension;

%><% if(WCMMode.PREVIEW.equals(mode) || WCMMode.DISABLED.equals(mode)) { %>
    <div data-ajax-component data-url="<%= url %>" class="acs-ajax-component"></div>
<% } else { %>
    <%-- In Authoring modes, do not bother AJAX'ing in components;
         Instead include them using the usual methods --%>
    <% IncludeOptions.getOptions(request, true).forceSameContext(true); %>
    <sling:include replaceSelectors="<%= ajaxSelectors %>" resource="<%= resource %>"/>
<% } %>