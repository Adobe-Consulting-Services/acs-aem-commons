<%--
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
  --%>
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" import="
                  java.util.List,
                  java.util.ArrayList,
                  java.util.HashMap,
                  org.apache.sling.api.resource.ResourceMetadata,
                  org.apache.sling.api.wrappers.ValueMapDecorator,
                  com.adobe.granite.ui.components.ds.DataSource,
                  com.adobe.granite.ui.components.ds.SimpleDataSource,
                  com.adobe.granite.ui.components.ds.ValueMapResource,
                  com.adobe.acs.commons.contentsync.UpdateStrategy"%><%
%><%

    List<Resource> lst = new ArrayList<Resource>();
	UpdateStrategy[] s = sling.getServices(UpdateStrategy.class, null);
	if(s != null) {
        for (UpdateStrategy stragegy : s) {
            ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());

            vm.put("value", stragegy.getClass().getName());
            vm.put("text", stragegy.getClass().getName());

            lst.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), "nt:unstructured", vm));
        }
	}
    request.setAttribute(DataSource.class.getName(), new SimpleDataSource(lst.iterator()));
%>