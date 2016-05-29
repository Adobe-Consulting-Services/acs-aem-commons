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
  --%><%--
  ==============================================================================

  Draggable List Component.

  A component that lets users reorder list items generated from any datasource.

  The ACS Commons Query Autocomplete Datasource can be used with this component as well.

   /**
   * The predicate map. Optional, only if using the ACS Commons Query Autocomplete datasource.
   */
  - predicates (String[]) = type=cq:Page,path=/content/geometrixx,property=jcr:content/sling:resourceType,property.value=geometrixx/components/homepage

   /**
   * The display property name. Optional, only if using the ACS Commons Query Autocomplete datasource.
   */
   -displayProperty (String) = jcr:content/jcr:title.

   /**
   * The display property name.
   */
   -sling:resourceType (String) = acs-commons/granite/ui/components/draggablelist.

  ==============================================================================

--%>
<%@include file="/libs/granite/ui/global.jsp" %>
<%@page session="false"
        import="com.adobe.granite.ui.components.AttrBuilder,
                com.adobe.granite.ui.components.Config,
                com.adobe.granite.ui.components.Tag,
                org.apache.commons.collections.IteratorUtils,
                java.util.*" %>
<%
    Config cfg = cmp.getConfig();

    Iterator<Resource> itemDataSourceIterator = cmp.getItemDataSource().iterator();

    String[] jcrValues = cmp.getValue().get(cfg.get("name", String.class), new String[0]);


    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    attrs.add("id", cfg.get("id", String.class));
    attrs.addClass(cfg.get("class", String.class));
    attrs.addRel(cfg.get("rel", String.class));
    attrs.add("title", i18n.getVar(cfg.get("title", String.class)));
    attrs.addOther("collision", "none");

    attrs.addClass("coral-Draggablelist-Addon");

    attrs.addOthers(cfg.getProperties(), "id", "class", "rel", "title", "name", "multiple", "disabled", "required", "renderReadOnly", "fieldLabel", "fieldDescription", "emptyText", "ignoreData", "translateOptions", "ordered");
%>
<div <%= attrs.build() %>>
    <label class="coral-Form-fieldlabel"><%= xssAPI.encodeForHTMLAttr(cfg.get("fieldLabel", String.class)) %>
    </label>

    <%
        AttrBuilder ulAttrs = new AttrBuilder(request, xssAPI);
        ulAttrs.addClass("acs-commons-draggablelist-draggable coral-Draggablelist-Addon-ul");
    %>
    <ul <%= ulAttrs.build() %>>
        <%
            List<String> valueList = Arrays.asList(jcrValues);

            List<Resource> itemDataSourceList = IteratorUtils.toList(itemDataSourceIterator);

            Map<String, String> finalMap = new LinkedHashMap();

            for (String jcrValue : valueList) {
                for (Resource item : itemDataSourceList) {
                    Config optionCfg = new Config(item);
                    String value = cmp.getExpressionHelper().getString(optionCfg.get("value", String.class));
                    String text = optionCfg.get("text", "");
                    if (jcrValue.equals(value)) {
                        finalMap.put(value, text);
                    }
                }
            }
            for (Resource item : itemDataSourceList) {
                Config optionCfg = new Config(item);
                String value = cmp.getExpressionHelper().getString(optionCfg.get("value", String.class));
                String text = optionCfg.get("text", "");
                if (!valueList.contains(value)) {
                    finalMap.put(value, text);
                }
            }
            for (String key : finalMap.keySet()) {
                AttrBuilder inputAttrs = new AttrBuilder(request, xssAPI);
                inputAttrs.add("name", cfg.get("name", String.class));
                inputAttrs.add("type", "hidden");
                inputAttrs.addDisabled(cfg.get("disabled", false));
                inputAttrs.add("value", key);
        %>
        <li><%=finalMap.get(key) %>
        </li>
        <%
            }

        %>

    </ul>


</div>
