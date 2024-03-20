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
%><%@ page import="java.util.Iterator,
                   java.util.TreeMap,
                   org.apache.sling.api.adapter.AdapterManager,
                   com.adobe.acs.commons.twitter.TwitterClient,
                   com.day.cq.wcm.webservicesupport.Configuration,
                   com.day.cq.wcm.webservicesupport.ConfigurationManager" %>

<body>
    <h1>Twitter Rate Limits</h1>
<div id="data">
<%
ConfigurationManager configurationManager = sling.getService(ConfigurationManager.class);
AdapterManager adapterManager = sling.getService(AdapterManager.class);
Iterator<Configuration> configs = configurationManager.getConfigurations("/etc/cloudservices/twitterconnect");
while (configs.hasNext()) {
    Configuration serviceConfig = configs.next();
    TwitterClient client = adapterManager.getAdapter(serviceConfig, TwitterClient.class);
    if (client != null) {
        pageContext.setAttribute("serviceConfig", serviceConfig);
        pageContext.setAttribute("stati", new TreeMap(client.getTwitter().getRateLimitStatus()));
%>
<cq:text value="${serviceConfig.title} (${serviceConfig.path})" escapeXml="true" tagName="h3"/>
<p>
    <ul>
        <c:forEach var="entry" items="${stati}">
            <li><b>${entry.key}</b> - ${entry.value.remaining} of ${entry.value.limit} remaining. Reset in ${entry.value.secondsUntilReset} seconds.</li>
        </c:forEach>
    </ul>
</p>
<%
    }
}
%>
</div>
</body>