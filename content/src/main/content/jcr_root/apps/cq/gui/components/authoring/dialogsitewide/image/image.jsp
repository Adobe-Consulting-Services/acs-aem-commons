<%--
  ADOBE CONFIDENTIAL

  Copyright 2012 Adobe Systems Incorporated
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
--%><%--

  This is a simple image component just for demonstration in the new authoring editor.
  Will be replaced with a more advanced version.

--%><%@page session="false" %><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page import="com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Value,
                  com.adobe.granite.ui.components.Field,
                  com.adobe.granite.ui.components.AttrBuilder" %><%

    Config cfg = new Config(resource);
    Value val = new Value(slingRequest, cfg);

    AttrBuilder attrs = new AttrBuilder(request, xssAPI);

    String fieldLabel = cfg.get("fieldLabel", String.class);

    String name = cfg.get("name", String.class);
    String value = val.get(name);

    attrs.add("id", cfg.get("id", String.class));
    attrs.addClass("image-full-width");
    attrs.addClass(cfg.get("class", String.class));
    attrs.addRel(cfg.get("rel", String.class));
    attrs.add("title", i18n.getVar(cfg.get("title", String.class)));
    attrs.add("src", value);

    attrs.addOthers(cfg.getProperties(), "id", "class", "rel", "title", "name", "value", "fieldLabel");

    String rootClass = Field.getRootClass(cfg, value);

    if (fieldLabel != null) {
        %><label class="<%= rootClass %>"><span><%= outVar(xssAPI, i18n, fieldLabel) %></span><%
        rootClass = "";
    }

	%><img <%= attrs.build() %> /><%

    if (fieldLabel != null) {
        %></label><%
    }
%>