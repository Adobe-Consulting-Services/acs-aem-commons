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
<%@page session="false" import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.resource.ResourceUtil,
                com.day.cq.wcm.webservicesupport.Configuration,
                com.day.cq.wcm.webservicesupport.ConfigurationManager" %>
<%@include file="/libs/foundation/global.jsp" %><%

String[] services = pageProperties.getInherited("cq:cloudserviceconfigs", new String[]{});
ConfigurationManager cfgMgr = resourceResolver.adaptTo(ConfigurationManager.class);
if(cfgMgr != null) {
    String footerCode = null;
    boolean debugMode = false;
    Configuration cfg = cfgMgr.getConfiguration("dtm", services);
    if(cfg != null) {
        footerCode = cfg.get("footerCode", null);
        debugMode = cfg.get("debugMode", false);
    }

    if(footerCode != null) {
    %>
    <script type="text/javascript">
    <%= footerCode %>
    <% if (debugMode) { %>_satellite.setDebug(true);<% } %>
    </script><%
    }
}
%>
