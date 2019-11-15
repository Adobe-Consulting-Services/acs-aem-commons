<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 - 2015 Adobe
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

<%@page
    import="java.util.Map, java.util.List, java.util.ArrayList, java.util.Collections, org.apache.commons.lang3.StringUtils,
			org.apache.sling.commons.json.JSONObject, org.apache.sling.commons.json.JSONArray,
			com.day.cq.replication.AgentManager, com.day.cq.replication.Agent"%>

<%
    response.setContentType("application/json");

    boolean isCacheInvalidate = properties.get("metaData/isCacheInvalidate", false);

    JSONArray options = new JSONArray();
    try {
        AgentManager agentManager = sling.getService(AgentManager.class);
        Map<String, Agent> agents = agentManager.getAgents();
        List<String> keys = new ArrayList<String>(agents.keySet());

        Collections.sort(keys);

        for (String key : keys) {
            Agent agent = agents.get(key);

            if (agent.isValid() && agent.isEnabled() && agent.isCacheInvalidator() == isCacheInvalidate) {
                JSONObject entry = new JSONObject();
                entry.put("value", key);
                entry.put("text", agent.getConfiguration().getName());
                options.put(entry);
            }
        }
    } catch (Exception ex) {
        log.error("Unable to create list of agents.", ex);
    }
%>
<%=options.toString()%>

