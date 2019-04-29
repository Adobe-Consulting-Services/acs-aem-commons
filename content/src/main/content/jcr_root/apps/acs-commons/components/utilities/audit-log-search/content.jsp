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
<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" import="com.adobe.acs.commons.util.QueryHelper, javax.jcr.query.Query"%><%

	QueryHelper queryHelper = sling.getService(QueryHelper.class);
	pageContext.setAttribute("missingIndex", queryHelper.isTraversal(resourceResolver, Query.JCR_SQL2, "SELECT * FROM [cq:AuditEvent]"));

%><div class="acs-section">
	<div ng-controller="MainCtrl" ng-init="app.uri = '${resourcePath}.auditlogsearch.json'; init();">
		<c:if test="${missingIndex}">
			<div class="coral-Alert coral-Alert--notice index-warning">
				<button type="button" class="coral-MinimalButton coral-Alert-closeButton" title="Close" data-dismiss="alert">
					<i class="coral-Icon coral-Icon--sizeXS coral-Icon--close coral-MinimalButton-icon"></i>
				</button>
				<i class="coral-Alert-typeIcon coral-Icon coral-Icon--sizeS coral-Icon--alert"></i>
				<strong class="coral-Alert-title">Index Missing</strong>
				<div class="coral-Alert-message">
					<p>
					No index found for the type <code>cq:AuditEvent</code>, this will result in very slow performance or failure to search audit events.
					</p>
					<p>
					For more information on creating the necessary index, please see the <a target="_blank" href="https://adobe-consulting-services.github.io/acs-aem-commons/features/audit-log-search/index.html">ACS AEM Commons Audit Log Search feature doc page</a>.
					</p>
				</div>
			</div>
		</c:if>
	    <p>Explore the audit log at any path.</p>
	    <cq:include script="includes/form.jsp"/>
	    <cq:include script="includes/results.jsp"/>
	</div>
</div>
