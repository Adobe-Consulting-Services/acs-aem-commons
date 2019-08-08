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
<div class="acs-section">
	<div ng-controller="MainCtrl" ng-init="app.uri = '${resourcePath}.results.html'; init();">
		<form ng-submit="run()" id="report--form">
			<sling2:listChildren resource="${sling2:getRelativeResource(resource,'parameters')}" var="parameters" />
			<c:forEach var="par" items="${parameters}">
				<div class="form-row">
					<cq:include path="parameters/${par.name}" resourceType="${par.valueMap.resourceType}" />
				</div>
			</c:forEach>
			<div class="form-row">
				<coral-checkbox value="-1" name="page">
					All Results
				</coral-checkbox>
			</div>
			<div class="form-row">
				<div class="form-left-cell">&nbsp;</div>
				<button class="coral-Button coral-Button--primary">Execute Report</button>
				<a class="coral-Button coral-Button--secondary" ng-click="download('${resource.path}.report.csv')">Download Report</a>
			</div>
		</form>
	</div>
</div>