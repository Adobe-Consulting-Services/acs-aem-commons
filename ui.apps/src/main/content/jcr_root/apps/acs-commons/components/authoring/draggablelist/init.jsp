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
        import="java.util.HashMap,
                org.apache.sling.api.wrappers.ValueMapDecorator,
                com.adobe.granite.ui.components.Config,
                com.adobe.granite.ui.components.Field" %>
<%
    Config cfg = cmp.getConfig();

    String name = cfg.get("name", String.class);
    String[] value = cmp.getValue().get(name, new String[0]);
    ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
    vm.put("value", value);

    request.setAttribute(Field.class.getName(), vm);
%>