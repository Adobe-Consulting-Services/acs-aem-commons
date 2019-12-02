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
--%><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false"
          import="java.util.Calendar,
                  org.apache.jackrabbit.util.ISO8601,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Tag" %><%

    // @deprecated read-only mode is deprecated

    Config cfg = cmp.getConfig();

	Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    String fieldLabel = cfg.get("fieldLabel", String.class);
    String displayFormat = i18n.getVar(cfg.get("displayedFormat", String.class));

    String value;

    // We have to fetch using Calendar.class as ValueMap doesn't guarantee a particular date format during conversion to string.
    Calendar calendarValue = cmp.getValue().get(cfg.get("name", String.class), Calendar.class);
    if (calendarValue != null) {
        value = ISO8601.format(calendarValue);
    } else {
        value = cmp.getValue().val(cmp.getExpressionHelper().getString(cfg.get("value", "")));
    }

    AttrBuilder spanAttrs = new AttrBuilder(request, xssAPI);
    spanAttrs.add("data-datepicker-format", displayFormat);

    if (cmp.getOptions().rootField()) {
        attrs.addClass("coral-Form-fieldwrapper");
        attrs.addClass(cfg.get("wrapperClass", String.class));

        %><span <%= attrs.build() %>><%
	        if (fieldLabel != null) {
	            %><label class="coral-Form-fieldlabel"><%= outVar(xssAPI, i18n, fieldLabel) %></label><%
	        }

	        %><span <%= spanAttrs.build() %>><%= xssAPI.encodeForHTML(value) %></span
        ></span><%
    } else {
        %><span <%= spanAttrs.build() %>><%= xssAPI.encodeForHTML(value) %></span><%
    }
%>