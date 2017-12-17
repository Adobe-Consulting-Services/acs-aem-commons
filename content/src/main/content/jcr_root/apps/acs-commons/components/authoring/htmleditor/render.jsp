<%@include file="/libs/granite/ui/global.jsp" %>
<%%>
<%@page session="false"
        import="com.adobe.granite.ui.components.AttrBuilder,
                com.adobe.granite.ui.components.Config,
                com.adobe.granite.ui.components.Field,
                com.adobe.granite.ui.components.Tag" %>
<%--###
AEM HTML Editor Widget
========

.. granite:servercomponent:: /apps/acs-commons/components/authoring/htmleditor
 :supertype: /libs/granite/ui/components/coral/foundation/form/field
 
 A HTML editor component.
 
 It extends :granite:servercomponent:`Field </libs/granite/ui/components/coral/foundation/form/field>` component.

 It has the following content structure:

 .. gnd:gnd::

    [granite:FormHtmlEditor] > granite:FormField
    
    /**
     * The name that identifies the field when submitting the form.
     */
    - name (String)

    /**
    * Number of rows to show (rest of code will be overflown)
    */
   - rows (integer) = 10
    
    
###--%>
<%

  Config cfg = cmp.getConfig();
  ValueMap vm = (ValueMap) request.getAttribute(Field.class.getName());
  // get attributes
  Tag tag = cmp.consumeTag();
  AttrBuilder attrs = tag.getAttrs();
  cmp.populateCommonAttrs(attrs);
  // get rows
  int rows = cfg.get("rows", 10) + 4;
  // get feild name
  attrs.add("name", cfg.get("name", String.class));
  // get feild value
  String value = vm.get("value", String.class);
  // add value to attributes
  attrs.add("value", value);
  
%>
<input type="hidden" id="acs-hidden-html-editor" <%= attrs.build() %> ></input>
<div data-language="markup" id="acs-html-editor-wrapper" style=" height: <%= rows %>em; border: #d0d0d0 solid 1px;"></div>

<%--### lazy load the clientlib, this will be cached by browser (unless caching is disabled ###--%>
<ui:includeClientLib categories="acs-commons.html.editor.widget"/>
