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
  ~   http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~ #L%
  --%><%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false"
          import="
    javax.jcr.Session,
    javax.jcr.Node,
	java.util.*,
	java.io.PrintWriter,
    com.day.cq.replication.Replicator,
    com.day.cq.replication.ReplicationOptions,
    com.day.cq.replication.ReplicationActionType" %><pre><%

    String path = request.getParameter("path");

    Replicator replicator = sling.getService(Replicator.class);
    ReplicationOptions opts = new ReplicationOptions();
    opts.setSuppressVersions(true);
    replicator.replicate(resourceResolver.adaptTo(Session.class), ReplicationActionType.ACTIVATE, path, null);
	out.println("Replicating: " + path);

%></pre>
