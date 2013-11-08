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
  --%>
<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher,
          com.day.cq.replication.Agent,
          com.day.cq.replication.ReplicationResult,
          com.day.cq.replication.ReplicationActionType,
          com.day.cq.replication.ReplicationException,
          java.util.Map"%><%

    /* Services */
    final DispatcherFlusher dispatcherFlusher = sling.getService(DispatcherFlusher.class);

    /* Properties */
    final String[] paths = properties.get("paths", new String[]{});
    final ReplicationActionType replicationActionType = ReplicationActionType.valueOf(properties.get("replicationActionType", "ACTIVATE"));

    String suffix = "";

    try {
        if(paths.length > 0) {
            final Map<Agent, ReplicationResult> results = dispatcherFlusher.flush(resourceResolver, replicationActionType, true, paths);

            for(final Map.Entry<Agent, ReplicationResult> entry : results.entrySet()) {
                final Agent agent = entry.getKey();
                final ReplicationResult result = entry.getValue();

                suffix = "/" + agent.getId() + "/" + (result.isSuccess() && result.getCode() == 200);
            }
        }
    } catch(ReplicationException ex) {
        suffix += "/replication-error";
    }

    response.sendRedirect(currentPage.getPath() + ".html" + suffix);
%>
