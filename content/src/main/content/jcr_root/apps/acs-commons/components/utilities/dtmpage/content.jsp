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
<%@page contentType="text/html"
            pageEncoding="utf-8"%><%
%><%@include file="/libs/foundation/global.jsp"%><div>

<div>
    <h3>Dynamic Tag Management</h3>
    <h4>Note - this integration is deprecated. Please use the standard integration instead.</h4>
    <ul>
        <li><div class="li-bullet"><strong>Header Script URL: </strong><br><%= xssAPI.encodeForHTML(properties.get("headerUrl", "")).replaceAll("\\&\\#xa;","<br>") %></div></li>
        <li><div class="li-bullet"><strong>JavaScript Footer Snippet: </strong><br><%= xssAPI.encodeForHTML(properties.get("footerCode", "")).replaceAll("\\&\\#xa;","<br>") %></div></li>
        <li><div class="li-bullet"><strong>Debug Mode: </strong><br><%= properties.get("debugMode", false) %></div></li>
    </ul>
</div>

