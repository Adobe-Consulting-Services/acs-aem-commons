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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page import="org.apache.sling.api.resource.Resource,
                  org.apache.sling.api.resource.ValueMap,
                  org.apache.sling.api.resource.ResourceUtil,
                  com.day.cq.wcm.webservicesupport.Configuration,
                  com.day.cq.wcm.webservicesupport.ConfigurationManager" %><%

String[] services = pageProperties.getInherited("cq:cloudserviceconfigs", new String[]{});
ConfigurationManager cfgMgr = resourceResolver.adaptTo(ConfigurationManager.class);
if (cfgMgr != null) {
    String kitID = null;
    Configuration cfg = cfgMgr.getConfiguration("typekit", services);
    if (cfg != null) {
        kitID = cfg.get("kitID", null);
    }

    if (kitID != null) {
    %>
<script type="text/javascript" src="//use.typekit.com/<%= kitID %>.js"></script>
<script type="text/javascript">try{Typekit.load();}catch(e){}</script><%
    }
}
%>