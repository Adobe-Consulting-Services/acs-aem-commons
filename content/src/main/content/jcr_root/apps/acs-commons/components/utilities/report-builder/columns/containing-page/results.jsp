<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2017 Adobe
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
<%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<%@ page import="com.day.cq.wcm.api.*" %>
<%
Resource result = (Resource) request.getAttribute("result");
PageManager pageMgr = resourceResolver.adaptTo(PageManager.class);
pageContext.setAttribute("page", pageManager.getContainingPage(result));
%>
<sling2:getResource path="${result.path}" var="resultRsrc" />
<td is="coral-table-cell" value="${page.path}">        
	<c:if test="${not empty page.path}">
		<a target="_blank" href="#" data-href="/sites.html${page.path}" class="coral-Link">
			<c:out value="${page.pageTitle}" default="${page.title}" />
		</a>
	</c:if>
</td>
