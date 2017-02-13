<%--
  #%L
  ACS AEM Commons Package - Audit Log Search
  %%
  Copyright (C) 2017 Dan Klco
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
<%@page session="false" %>
<div class="acs-section"> 
	<div ng-controller="MainCtrl" ng-init="app.uri = '${resourcePath}.auditlogsearch.json'; init();">
	    <p>Explore the audit log at any path.</p>
	    <cq:include script="includes/form.jsp"/>
	    <cq:include script="includes/results.jsp"/>
	</div>
</div>