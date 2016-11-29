<%--
  ~ #%L
  ~ ACS AEM Commons Bundle
  ~ %%
  ~ Copyright (C) 2014 Adobe
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
 <%@page import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.resource.ResourceUtil,
                com.day.cq.wcm.webservicesupport.Configuration,
                com.day.cq.wcm.webservicesupport.ConfigurationManager" %>
<%@include file="/libs/foundation/global.jsp" %><%

String[] services = pageProperties.getInherited("cq:cloudserviceconfigs", new String[]{});
ConfigurationManager cfgMgr = resourceResolver.adaptTo(ConfigurationManager.class);
if(cfgMgr != null) {
    String publisherId = null;
    Configuration cfg = cfgMgr.getConfiguration("sharethis", services);
    if(cfg != null) {
        publisherId = cfg.get("publisherId", null);
         if (publisherId != null) {
             publisherId = xssAPI.encodeForJSString(publisherId);
        }
    }
    if(publisherId != null) {
        request.setAttribute("com.adobe.acs.commons.sharethis.publisherId", publisherId);
    %>
<script type="text/javascript">
    var switchTo5x=true;
    (function(){
        var stJsHost = (("https:" == document.location.protocol) ? "https://ws." : "http://w.");
        document.write(unescape("%3Cscript src='" + stJsHost + "sharethis.com/button/buttons.js' type='text/javascript'%3E%3C/script%3E"));
    })();
</script>
<script type="text/javascript">stLight.options({publisher: "<%= publisherId %>", doNotHash: <%= cfg.get("doNotHash", false) %>, doNotCopy: <%= cfg.get("doNotCopy", false) %>, hashAddressBar: <%= cfg.get("hashAddressBar", false) %>});</script>
<%
    }
}
%>