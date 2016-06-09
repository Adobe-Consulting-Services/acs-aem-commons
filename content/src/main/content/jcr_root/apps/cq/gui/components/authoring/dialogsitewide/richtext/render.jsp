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
          import="com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.Field,
                  com.adobe.granite.ui.components.Tag" %><%

    Config cfg = cmp.getConfig();
    ValueMap vm = (ValueMap) request.getAttribute(Field.class.getName());

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    attrs.add("id", cfg.get("id", String.class));
    attrs.addClass(cfg.get("class", String.class));
    attrs.addRel(cfg.get("rel", String.class));
    attrs.add("title", i18n.getVar(cfg.get("title", String.class)));

    attrs.add("type", "text");
    attrs.add("name", cfg.get("name", String.class));
    attrs.add("placeholder", i18n.getVar(cfg.get("emptyText", String.class)));
    attrs.addDisabled(cfg.get("disabled", false));
    attrs.add("value", vm.get("value", String.class));

    if (cfg.get("required", false)) {
        attrs.add("aria-required", true);
    }

    attrs.addClass("coral-Textfield");

    attrs.addOthers(cfg.getProperties(), "id", "class", "rel", "title", "type", "name", "value", "emptyText", "disabled", "required", "fieldLabel", "fieldDescription", "renderReadOnly", "ignoreData");

%><div class="richtext-container">
    <input type="hidden" <%= attrs.build() %>>
    <input type="hidden" class="coral-RichText-isRichTextFlag" name="<%=cfg.get("richTextFlagPropertyName", "./textIsRich")%>" value="true">
    <div class="coral-RichText-editable coral-Form-field coral-Textfield coral-Textfield--multiline coral-RichText" data-config-path="<%=resource.getPath()%>.infinity.json"
         data-use-fixed-inline-toolbar="<%=cfg.get("useFixedInlineToolbar", Boolean.class)%>"></div>
</div>