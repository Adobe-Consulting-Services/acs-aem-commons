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
%><%@page import="com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ComponentHelper.Options,
                  com.adobe.granite.ui.components.Tag,
                  com.adobe.granite.ui.components.Value" %><%

    /**
     * The SizeField lets the user enter the width and height (for example for an image).
     * It demands child nodes named <code>width</code> and <code>height</code> to represent the width and height respectively.
     * Usually they are having resource type of <code>granite/ui/components/foundation/form/numberfield</code>.
     * They also support <code>unit</code> property to indicate what unit to be printed.
     *
     * @component
     * @name SizeField
     * @location /libs/cq/gui/components/authoring/dialog/sizefield
     *
     * @property {String} [id] id attr
     * @property {String} [rel] class attr (this is to indicate the semantic relationship of the element)
     * @property {String} [class] class attr
     * @property {String} [title] title attr
     * @property {String} [fieldLabel] the label of the widget
     * @property {String} [width/unit=px] the unit to be printed for width
     * @property {String} [height/unit=px] the unit to be printed for height
     * @property {String} [&lt;other&gt;] data-&lt;other&gt; attr
     *
     * @example
     * <caption>Content Structure</caption>
     * + mysizefield
     *   - jcr:primaryType = "nt:unstructured"
     *   - sling:resourceType = "cq/gui/components/authoring/dialog/sizefield"
     *   - fieldLabel = "Size"
     *   + width
     *     - jcr:primaryType = "nt:unstructured"
     *     - sling:resourceType = "granite/ui/components/foundation/form/numberfield"
     *     - unit = "%"
     *   + height
     *     - jcr:primaryType = "nt:unstructured"
     *     - sling:resourceType = "granite/ui/components/foundation/form/numberfield"
     *     - unit = "%"
     */
     
    Config cfg = cmp.getConfig();
    Value val = new Value(slingRequest, cfg);
    
    Resource width = resource.getChild("width");
    Resource height = resource.getChild("height");
    
    String fieldLabel = cfg.get("fieldLabel", String.class);
    
    Tag tag = cmp.consumeTag();
    
    AttrBuilder attrs = tag.getAttrs();

    attrs.add("id", cfg.get("id", String.class));
    attrs.addClass("coral-Form-field cq-Sizefield");
    attrs.addClass(cfg.get("class", String.class));
    attrs.addRel(cfg.get("rel", String.class));
    attrs.add("title", i18n.getVar(cfg.get("title", String.class)));
    
    attrs.addOthers(cfg.getProperties(), "id", "class", "rel", "title", "fieldLabel");

    if (cmp.getOptions().rootField() && (fieldLabel != null)) {
        AttrBuilder fieldWrapperAttrs = new AttrBuilder(request, xssAPI);
        fieldWrapperAttrs.addClass("coral-Form-fieldwrapper");

    %><div <%= fieldWrapperAttrs.build() %>><%

        attrs.addClass("coral-Form-field");

        if (fieldLabel != null) {
            %><label class="coral-Form-fieldlabel"><%= outVar(xssAPI, i18n, fieldLabel) %></label><%
        }
    }

    %><div <%= attrs.build() %>><%
        AttrBuilder widthAttrs = new AttrBuilder(request, xssAPI);
        widthAttrs.addClass("cq-Sizefield-width");
        cmp.include(width, new Options().rootField(false).tag(new Tag(widthAttrs)));

        AttrBuilder heightAttrs = new AttrBuilder(request, xssAPI);
        heightAttrs.addClass("cq-Sizefield-height");
        cmp.include(height, new Options().rootField(false).tag(new Tag(heightAttrs)));
    %></div><%

    if (cmp.getOptions().rootField() && (fieldLabel != null)) {
        %></div><%
    }
    %>