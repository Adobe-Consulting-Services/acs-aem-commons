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
                  org.apache.sling.api.resource.ResourceMetadata,
                  org.apache.sling.api.wrappers.ValueMapDecorator,
                  java.util.List,
                  java.util.HashMap,
                  java.util.ArrayList,
                  com.adobe.granite.ui.components.ds.DataSource,
                  com.adobe.granite.ui.components.ds.SimpleDataSource,
                  com.adobe.granite.ui.components.ds.ValueMapResource,
				  com.adobe.acs.commons.contentsync.ConfigurationUtils

            " %><%

    Resource hostsResource = resourceResolver.getResource(ConfigurationUtils.HOSTS_PATH);
	Resource suffixResource = slingRequest.getRequestPathInfo().getSuffixResource();

    List<Resource> lst = new ArrayList<Resource>();
	if(hostsResource != null) {
        for (Resource item : hostsResource.getChildren()) {
            ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());

            String name = item.getValueMap().get("name", item.getValueMap().get("host", ""));
            vm.put("value", item.getPath());
            vm.put("text", name);
            if(suffixResource != null && suffixResource.getPath().equals(item.getPath())) {
                vm.put("selected", true);
            }

            lst.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), "nt:unstructured", vm));
        }
	}
    request.setAttribute(DataSource.class.getName(), new SimpleDataSource(lst.iterator()));
%>