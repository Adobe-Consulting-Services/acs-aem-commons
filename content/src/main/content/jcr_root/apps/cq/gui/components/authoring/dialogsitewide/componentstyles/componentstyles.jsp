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
--%><%@page session="false" %><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page import="com.adobe.granite.ui.components.Value,
                  com.adobe.granite.ui.components.Field,
                  com.adobe.granite.ui.components.AttrBuilder,
                  org.apache.sling.api.resource.ResourceResolver,
                  org.apache.sling.api.resource.Resource,
                  com.day.cq.wcm.api.Page,
                  com.day.cq.wcm.api.PageManager,
                  java.util.Map,
                  com.day.cq.wcm.api.designer.*, com.adobe.granite.ui.components.Config, org.apache.sling.api.resource.ResourceUtil, com.adobe.granite.xss.XSSAPI, java.util.Arrays, java.util.List" %><%

    Config cfg = new Config(resource);
    Value val = new Value(slingRequest, cfg);

    String name = cfg.get("name", String.class);
    String value = val.get(name);
    String rootClass = Field.getRootClass(cfg, value);

    // Build attributes for our hidden field that will be sent to server
    AttrBuilder attrsHidden = new AttrBuilder(request, xssAPI);
    attrsHidden.add("name", name);
    attrsHidden.add("value", value);


    // Try to get the design styles
    String contentPath = (String) slingRequest.getAttribute(Value.CONTENTPATH_ATTRIBUTE);
    if (contentPath == null) {
        %>No content path set.<%
        return;
    }

    Resource contentResource = slingRequest.getResourceResolver().resolve(contentPath);
    if (ResourceUtil.isNonExistingResource(contentResource)) {
        %>Resource not existing.<%
        return;
    }

    // check if resource is a page
    ResourceResolver resolver = slingRequest.getResourceResolver();
    Designer designer = resolver.adaptTo(Designer.class);
    if (designer == null) {
        %>No Designer available.<%
        return;
    }

    PageManager pMgr = resolver.adaptTo(PageManager.class);
    Page page2 = pMgr.getContainingPage(contentResource);
    Design design = designer.getDesign(page2);
    if (design == null) {
        %>No Design available.<%
        //resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return;
    }

    Style style = design.getStyle(contentResource);
    Cell cell = style == null ? null : style.getCell();

    // Render styles
    Map<String, ComponentStyle> styles = design.getComponentStyles(cell);
    %>
    <div class="cq-ComponentStyles">
    <input type="hidden" <%= attrsHidden.build() %>>
    <%
    String[] singleValues = value.split(" ");
    List singleValueList = Arrays.asList(singleValues);

    boolean hasStyles = !styles.isEmpty();
    String stylesHeading =  hasStyles ? i18n.get("Available styles") : i18n.get("No available styles");

    %><h4 class="coral-Heading coral-Heading--4"><%= outVar(xssAPI, i18n, stylesHeading) %></h4><%

    if (hasStyles) {
        %><div class="coral-Well"><%
    }

    for (ComponentStyle s : styles.values()) {

        %><div class="coral-Form-fieldwrapper <%= rootClass %>">
            <label class="coral-Form-fieldlabel"><%= xssAPI.encodeForHTML(s.getTitle()) %></label>
            <span class="coral-Form-field coral-Select" data-init="select">
                <button class="coral-Select-button coral-MinimalButton" type="button">
                    <span class="coral-Select-button-text"></span>
                </button>
                <select name="./type" class="coral-Select-select">
                    <% for (ComponentStyle.Option o : s.getOptions()) {
                        boolean selected = singleValueList.contains(o.getValue());
                    %><option value="<%=  xssAPI.encodeForHTMLAttr(o.getValue()) %>" <%= (selected) ? "selected" : "" %>>
                      <%= xssAPI.encodeForHTML(o.getText()) %>
                    </option>
                    <%
                } %>
                </select>
            </span>
        </div><%
    }
    if (hasStyles) {
        %></div><%
        }
    %>
    </div>
    <%
%>