<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 - 2022 Adobe
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
  --%>
<%@ include file="/libs/foundation/global.jsp"%>
<%@page import="java.util.Map, 
                java.util.List,
                java.util.ArrayList,
                java.util.Collections,
                java.util.HashMap,
                com.adobe.granite.ui.components.ds.DataSource,
                com.adobe.granite.ui.components.ds.EmptyDataSource,
                com.adobe.granite.ui.components.ds.SimpleDataSource,
                com.adobe.granite.ui.components.ds.ValueMapResource,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.resource.ResourceResolver,
                org.apache.sling.api.resource.ResourceMetadata,
                org.apache.sling.api.wrappers.ValueMapDecorator,
			    com.day.cq.replication.AgentManager, 
                com.day.cq.replication.Agent
"%><cq:defineObjects/>
<%

    request.setAttribute(DataSource.class.getName(), EmptyDataSource.instance());
    ResourceResolver resolver = componentContext.getResource().getResourceResolver();
    List<Resource> fakeResourceList = new ArrayList<Resource>();

    boolean isCacheInvalidate = properties.get("metaData/isCacheInvalidate", false);

    try {
        AgentManager agentManager = sling.getService(AgentManager.class);
        Map<String, Agent> agents = agentManager.getAgents();
        List<String> keys = new ArrayList<String>(agents.keySet());

        Collections.sort(keys);

        for (String key : keys) {
            Agent agent = agents.get(key);

            if (agent.isValid() && agent.isEnabled() && agent.isCacheInvalidator() == isCacheInvalidate) {
                ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());

                vm.put("value", key);
                vm.put("text", agent.getConfiguration().getName());
                fakeResourceList.add(new ValueMapResource(resolver, new ResourceMetadata(), "nt:unstructured", vm));
            }
        }

        // Create a new data source from iterating through our fakedResourceList
        DataSource ds = new SimpleDataSource(fakeResourceList.iterator());
  
        // Add the datasource to our request to expose in the view
        request.setAttribute(DataSource.class.getName(), ds);

    } catch (Exception ex) {
        log.error("Unable to create list of agents.", ex);
        slingResponse.setStatus(500);
    }
%>
