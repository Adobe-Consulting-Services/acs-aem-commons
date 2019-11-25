<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2015 Adobe
  ~ %%
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~      http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%><%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false"
          import="java.text.Collator,
                  java.util.Collections,
                  java.util.Comparator,
                  java.util.Iterator,
                  java.util.List,
                  javax.servlet.jsp.JspWriter,
                  org.apache.commons.collections.IteratorUtils,
                  com.adobe.granite.ui.components.AttrBuilder,
                  com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.ComponentHelper,
                  com.adobe.granite.ui.components.Tag" %><%--###
Select
======

.. granite:servercomponent:: /apps/acs-commons/components/authoring/graphiciconselect
   :supertype: /libs/granite/ui/components/foundation/form/select
   
   Graphic Icon Select is a component to represent a concept of selection of icon options.

   It extends :granite:servercomponent:`Select </libs/granite/ui/components/foundation/form/select>` component.

   It has the following content structure:

   .. gnd:gnd::

      [granite:FormSelect]
      
      /**
       * The id attribute.
       */
      - id (String)

      /**
       * The class attribute. This is used to indicate the semantic relationship of the component similar to ``rel`` attribute.
       */
      - rel (String)

      /**
       * The class attribute.
       */
      - class (String)

      /**
       * The title attribute.
       */
      - title (String) i18n
      
      /**
       * The name that identifies the field when submitting the form.
       */
      - name (String)
      
      /**
       * The value of the field.
       */
      - value (String) multiple
      
      /**
       * The initial text to display when nothing is selected.
       */
      - emptyText (String) i18n
      
      /**
       * Indicates if the field is in disabled state.
       */
      - disabled (Boolean)
      
      /**
       * Indicates if the user is able to select multiple selections.
       */
      - multiple (Boolean)
      
      /**
       * ``true`` to translate the options, ``false`` otherwise.
       */
      - translateOptions (Boolean) = true
      
      /**
       * ``true`` to sort the options based on the text, ``false`` otherwise.
       *
       * It is assumed that the options don't contain option group.
       */
      - ordered (Boolean) = false
      
      /**
       * The options of this component can be specified by this child resource, or by ``datasource`` child resource.
       */
      + items
      
      /**
       * The options of this component can be specified by this child resource, or by ``items`` child resource.
       */
      + datasource
      
   Each option has the following structure:
   
   .. gnd:gnd::

      [granite:FormSelectItem]
      
      /**
       * The id attribute.
       */
      - id (String)

      /**
       * The class attribute. This is used to indicate the semantic relationship of the component similar to ``rel`` attribute.
       */
      - rel (String)

      /**
       * The class attribute.
       */
      - class (String)
      
      /**
       * The title attribute.
       */
      - title (String) i18n
      
      /**
       * The value of the option.
       */
      - value (StringEL)
      
      /**
       * Indicates if the option is in disabled state.
       */
      - disabled (Boolean)
      
      /**
       * ``true`` to pre-select this option, ``false`` otherwise.
       */
      - selected (Boolean)
      
      /**
       * The text of the option.
       */
      - text (String) i18n
      
   Example::
   
      + myselect
        - sling:resourceType = "acs-commons/components/authoring/graphiciconselect"
        - emptyText = "Select"
        - name = "myselect"
        + items
          + option1
            - text = "op1"
          + option2
            - text = "op2"
###--%><%

    Config cfg = cmp.getConfig();

    Iterator<Resource> itemIterator = cmp.getItemDataSource().iterator();
    
    if (cfg.get("ordered", false)) {
        List<Resource> items = (List<Resource>) IteratorUtils.toList(itemIterator);
        final Collator langCollator = Collator.getInstance(request.getLocale());
        
        Collections.sort(items, new Comparator<Resource>() {
            public int compare(Resource o1, Resource o2) {
                return langCollator.compare(getOptionText(o1, cmp), getOptionText(o2, cmp));
            }
        });
        
        itemIterator = items.iterator();
    }

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();
    cmp.populateCommonAttrs(attrs);

    attrs.add("id", cfg.get("id", String.class));
    attrs.addClass(cfg.get("class", String.class));
    attrs.addClass("coral-Select");
    attrs.addRel(cfg.get("rel", String.class));
    attrs.add("title", i18n.getVar(cfg.get("title", String.class)));
    attrs.addOther("collision", "none");

    attrs.add("data-init", "graphiciconselect");
    
    attrs.addOthers(cfg.getProperties(), "id", "class", "rel", "title", "name", "multiple", "disabled", "required", "renderReadOnly", "fieldLabel", "fieldDescription", "emptyText", "ignoreData", "translateOptions", "ordered");


    AttrBuilder selectAttrs = new AttrBuilder(request, xssAPI);
    selectAttrs.add("name", cfg.get("name", String.class));
    selectAttrs.addMultiple(cfg.get("multiple", false));
    selectAttrs.addDisabled(cfg.get("disabled", false));

    AttrBuilder selectListAttrs = new AttrBuilder(request, xssAPI);
    selectListAttrs.addOther("collision-adjustment", cfg.get("collisionAdjustment", String.class));

%><span <%= attrs.build() %>>
    <select <%= selectAttrs.build() %>><%
        for (Iterator<Resource> items = itemIterator; items.hasNext();) {
            printOption(out, items.next(), cmp);
        }
    %></select>
</span><%!

    private void printOption(JspWriter out, Resource option, ComponentHelper cmp) throws Exception {
        I18n i18n = cmp.getI18n();
        XSSAPI xss = cmp.getXss();
    
        Config optionCfg = new Config(option);
        String value = cmp.getExpressionHelper().getString(optionCfg.get("value", String.class));

        AttrBuilder opAttrs = new AttrBuilder(null, cmp.getXss());

        opAttrs.add("id", optionCfg.get("id", String.class));
        opAttrs.addClass(optionCfg.get("class", String.class));
        opAttrs.addRel(optionCfg.get("rel", String.class));
        opAttrs.add("title", i18n.getVar(optionCfg.get("title", String.class)));
        opAttrs.add("value", value);

        opAttrs.addDisabled(optionCfg.get("disabled", false));
        opAttrs.addOthers(optionCfg.getProperties(), "id", "class", "rel", "title", "value", "text", "disabled", "selected", "group");

        // if the item is an optgroup, render the <optgroup> and all its containing items
        if (optionCfg.get("group", false)) {
            opAttrs.add("label", i18n.getVar(optionCfg.get("text", String.class)));

            out.println("<optgroup " + opAttrs.build() + ">");
            for (Iterator<Resource> options = option.listChildren(); options.hasNext();) {
                printOption(out, options.next(), cmp);
            }
            out.println("</optgroup>");
        } else {
            // otherwise, render the <option>
            opAttrs.addSelected(cmp.getValue().isSelected(value, optionCfg.get("selected", false)));
            out.println("<option " + opAttrs.build() + ">" + xss.encodeForHTML(getOptionText(option, cmp)) + "</option>");
        }
    }

    private String getOptionText(Resource option, ComponentHelper cmp) {
        Config optionCfg = new Config(option);
        String text = optionCfg.get("text", "");
        
        if (cmp.getConfig().get("translateOptions", true)) {
            text = cmp.getI18n().getVar(text);
        }
        
        return text;
    }
%>
