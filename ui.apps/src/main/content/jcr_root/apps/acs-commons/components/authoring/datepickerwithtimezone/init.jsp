<%--
  ADOBE CONFIDENTIAL

  Copyright 2015 Adobe Systems Incorporated
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
                  java.util.HashMap,
                  org.apache.jackrabbit.util.ISO8601,
                  org.apache.sling.api.wrappers.ValueMapDecorator,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Field" %><%

    Config cfg = cmp.getConfig();

    // Calendar formatting is done by FormData
    String value = cmp.getValue().val(cmp.getExpressionHelper().getString(cfg.get("value", "")));

    ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
    vm.put("value", value);

    request.setAttribute(Field.class.getName(), vm);
%>