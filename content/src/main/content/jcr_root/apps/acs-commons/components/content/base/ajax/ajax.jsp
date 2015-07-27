<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 Adobe
  %%
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  
       http://www.apache.org/licenses/LICENSE-2.0
  
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
  #L%
  --%>
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
    final String CN_AJAX_LOADING_INDICATOR = "ajaxLoadingIndicator";
    final String CN_AJAX_PASS_QUERY_PARAMS = "ajaxPassQueryParameters";


    final WCMMode mode = WCMMode.fromRequest(slingRequest);
    final ValueMap componentProperties = component.getProperties();

    String ajaxLoadingIndicator =
            xssAPI.encodeForHTMLAttr(StringUtils.stripToEmpty(componentProperties.get(CN_AJAX_LOADING_INDICATOR, "")));
    boolean ajaxLoadingIndicatorEnabled = StringUtils.isNotBlank(ajaxLoadingIndicator);

    String queryParams = StringUtils.stripToNull(slingRequest.getQueryString());
    boolean passQueryParams = componentProperties.get(CN_AJAX_PASS_QUERY_PARAMS, false)
            &&  StringUtils.isNotBlank(queryParams);

    String ajaxSelectors = StringUtils.stripToEmpty(componentProperties.get(CN_AJAX_SELECTOR, DEFAULT_SELECTOR));
    String ajaxExtension = StringUtils.stripToEmpty(componentProperties.get(CN_AJAX_EXTENSION, DEFAULT_EXTENSION));
    if(StringUtils.isBlank(ajaxSelectors)) { ajaxSelectors = DEFAULT_SELECTOR; }
    if(StringUtils.isBlank(ajaxExtension)) { ajaxExtension = DEFAULT_EXTENSION; }

    final String url = resourceResolver.map(slingRequest, resource.getPath()) + "." + ajaxSelectors + "." + ajaxExtension;

%><% if(WCMMode.PREVIEW.equals(mode) || WCMMode.DISABLED.equals(mode)) { %>
    <div data-ajax-component data-url="<%= url %>"
         <%= passQueryParams ? "data-ajax-query-parameters=\"" + xssAPI.encodeForHTMLAttr(queryParams) + "\"" : "" %>
         class="acs-ajax-component">
    	<% if (ajaxLoadingIndicatorEnabled) { %>
    		<div class="<%= ajaxLoadingIndicator %>"></div>
    	<% } %>
    </div>
<% } else { %>
    <%-- In Authoring modes, do not bother AJAX'ing in components;
         Instead include them using the usual methods --%>
    <% IncludeOptions.getOptions(request, true).forceSameContext(true); %>
    <sling:include replaceSelectors="<%= ajaxSelectors %>" resource="<%= resource %>"/>
<% } %>