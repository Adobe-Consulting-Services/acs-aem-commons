<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2013 Adobe
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
  --%>
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" contentType="text/html" pageEncoding="utf-8"
          import="com.adobe.acs.commons.util.RequireAem" %><%

RequireAem requireAem = sling.getService(RequireAem.class);

if (RequireAem.Distribution.CLOUD_READY.equals(requireAem.getDistribution())) { %>

<link rel="stylesheet" href="https://unpkg.com/@adobe/spectrum-css@2.15.1/dist/standalone/spectrum-light.css"/>
<div class="spectrum-Toast spectrum-Toast--negative" style="display: block; margin: 0 auto; width: 100%;">
    <div class="spectrum-Toast-body">
        <div class="spectrum-Toast-content">
            Bulk Workflow Manager is not compatible with your version of Adobe Experience Manager.
            <br/>
            As a possible alternative, please checkout ACS AEM Commons Manage Controlled Processes (MCP).
        </div>
    </div>
</div>

<%  return;
} %>

<div class="page"
         role="main"
         ng-controller="MainCtrl"
         ng-init="app.uri = '${resourcePath}'; init();">

    <div ng-show="showForm()">
        <%@include file="includes/form.jsp" %>
    </div>

    <div ng-hide="showForm()">
        <%@include file="includes/status.jsp" %>
    </div>
 </div>