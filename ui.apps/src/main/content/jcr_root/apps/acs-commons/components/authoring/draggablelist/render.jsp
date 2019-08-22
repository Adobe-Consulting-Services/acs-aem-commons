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

--%><%@include file="/libs/granite/ui/global.jsp" %>
<%@page session="false"
        import="com.adobe.granite.ui.components.AttrBuilder,
                com.adobe.granite.ui.components.Config,
                com.adobe.granite.ui.components.Tag,
                org.apache.commons.collections.IteratorUtils,
                java.util.*" %>
<%

    Config cfg = cmp.getConfig();

    Iterator<Resource> itemDataSourceIterator = cmp.getItemDataSource().iterator();

    String name = cfg.get("name", String.class);
    String[] jcrValues = cmp.getValue().get(name, new String[0]);
    boolean disabled = cfg.get("disabled", false);

    Tag tag = cmp.consumeTag();
    AttrBuilder attrs = tag.getAttrs();

    attrs.add("id", cfg.get("id", String.class));
    attrs.addClass(cfg.get("class", String.class));
    attrs.addRel(cfg.get("rel", String.class));
    attrs.add("title", i18n.getVar(cfg.get("title", String.class)));
    attrs.addOther("collision", "none");
    if (disabled) {
        attrs.add("data-disabled", disabled);
    }

    attrs.addClass("coral-Addon-Draggablelist");

    attrs.addOthers(cfg.getProperties(), "id", "class", "rel", "title", "name", "multiple", "disabled", "required", "renderReadOnly", "fieldLabel", "fieldDescription", "emptyText", "ignoreData", "translateOptions", "ordered");


    AttrBuilder deleteAttrs = new AttrBuilder(request, xssAPI);
    deleteAttrs.add("type", "hidden");
    deleteAttrs.addDisabled(disabled);
    deleteAttrs.add("name", name + "@Delete");

    AttrBuilder typeHintAttrs = new AttrBuilder(request, xssAPI);
    typeHintAttrs.add("type", "hidden");
    typeHintAttrs.addDisabled(disabled);
    typeHintAttrs.add("name", name + "@TypeHint");
    typeHintAttrs.add("value", "String[]");

%>
<div <%= attrs.build() %>>
    <input <%= deleteAttrs.build() %>>
    <input <%= typeHintAttrs.build() %>>

    <div class="acs-commons-draggablelist-dropzone">
        <%
            AttrBuilder ulAttrs = new AttrBuilder(request, xssAPI);
            ulAttrs.addClass("acs-commons-draggablelist-draggable coral-Addon-Draggablelist-ul");
            ulAttrs.add("data-init", "draggablelist");
            ulAttrs.add("data-allow", "reorder drop");
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
            <li draggable="true"><input <%=inputAttrs.build() %>/><%=finalMap.get(key) %>
                <i class="coral-Icon coral-Icon--apps list-move-icon"></i>
            </li>
            <%
                }

            %>

        </ul>
    </div>

</div>
