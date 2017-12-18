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
<%
com.day.cq.rewriter.linkchecker.LinkCheckerSettings.fromRequest(slingRequest).setIgnoreInternals(true);
%>
<sling2:adaptTo adaptable="${slingRequest}" adaptTo="com.adobe.acs.commons.reports.models.ReportRunner" var="runner" />
<c:set var="reportExecutor" value="${runner.reportExecutor}" scope="request" />
<c:set var="results" value="${reportExecutor.results}" scope="request" />
<sling2:listChildren resource="${sling2:getRelativeResource(resource,'columns')}" var="columns" />
<coral-drawer direction="up" class="report__details">
	<pre>${reportExecutor.details}</pre>
</coral-drawer>
<div>Results ${results.resultsStart} - ${results.resultsEnd}</div>
<div class="report__result-container">
	<table is="coral-table" selectable>
		<colgroup>
			<sling2:listChildren resource="${sling2:getRelativeResource(resource,'columns')}" var="columns" />
			<c:forEach var="col" items="${columns}">
				<col is="coral-table-column" sortable="stortable" data-foundation-layout-table-column-name="${sling2:encode(col.valueMap.heading,'HTML_ATTR')}">
			</c:forEach>
		</colgroup>
        <thead is="coral-table-head" sticky>
            <tr is="coral-table-row">
            	<sling2:listChildren resource="${sling2:getRelativeResource(resource,'columns')}" var="columns" />
				<c:forEach var="col" items="${columns}">
					<th is="coral-table-headercell">
						<sling2:encode value="${col.valueMap.heading}" mode="HTML" />
					</th>
				</c:forEach>
            </tr>
        </thead>
        <tbody is="coral-table-body">
        	<c:forEach var="result" items="${results.results}">
        		<tr is="coral-table-row">
        			<sling2:listChildren resource="${sling2:getRelativeResource(resource,'columns')}" var="columns" />
		        	<c:forEach var="col" items="${columns}">
						<c:set var="result" value="${result}" scope="request" />
						<sling:include path="columns/${col.name}" resourceType="${col.resourceType}" />
					</c:forEach>
				</tr>
			</c:forEach>
		</tbody>
	</table>
</div>
<div class="pagination">
	<c:if test="${results.previousPage != -1}">
		<a href="${resource.path}.results.html?page=0&${report.parameters}" data-page="0" class="coral-Button coral-Button--square pagination__link pagination__prev">
			First
		</a>
		<a href="${resource.path}.results.html?page=${results.previousPage}&${report.parameters}" data-page="${results.previousPage}" class="coral-Button coral-Button--square pagination__link pagination__prev">
			Previous
		</a>
	</c:if>
	<c:if test="${results.nextPage != -1}">
		<a href="${resource.path}.results.html?page=${results.nextPage}&${report.parameters}" data-page="${results.nextPage}" class="coral-Button coral-Button--square pagination__link pagination__next">
			Next
		</a>
	</c:if>
</div>
<%
com.day.cq.rewriter.linkchecker.LinkCheckerSettings.fromRequest(slingRequest).setIgnoreInternals(false);
%>