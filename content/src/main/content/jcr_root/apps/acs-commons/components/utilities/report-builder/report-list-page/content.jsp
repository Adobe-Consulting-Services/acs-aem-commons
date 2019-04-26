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
  --%><%
%><%@include file="/libs/foundation/global.jsp" %>
<%@taglib prefix="sling2" uri="http://sling.apache.org/taglibs/sling" %>
<div ng-controller="MainCtrl" ng-init="init();">
	
	<div class="coral-Well">
		<form action="/var/acs-commons/reports" method="post" class="coral-Form--aligned" id="fn-acsCommons-add" ng-submit="createReport($event, 'fn-acsCommons-add')">
			<input type="hidden" name="jcr:primaryType" value="cq:Page" />
			<input type="hidden" name="jcr:content/sling:resourceType" value="acs-commons/components/utilities/report-builder/report-page" />
			<input type="hidden" name="jcr:content/jcr:created" />
			<input type="hidden" name="jcr:content/jcr:createdBy" />
			<input type="hidden" name="jcr:content/cq:designPath" value="/etc/designs/acs-commons" />
			<div class="coral-Form-fieldwrapper">
				<label class="coral-Form-fieldlabel" id="title">Title</label>
				<input is="coral-textfield" class="coral-Form-field" placeholder="Enter your title" name="jcr:content/jcr:title" labelledby=title" required />
			</div>
			<div class="coral-Form-fieldwrapper" >
				<button is="coral-button" icon="add" iconsize="S">
					Add Report
				</button>
			</div>
		</form>
	</div>
	<br/><hr/><br/>
	<table is="coral-table" class="table-example">
		<colgroup>
			<col is="coral-table-column" sortable>
			<col is="coral-table-column">
			<col is="coral-table-column" fixedwidth>
			<col is="coral-table-column" fixedwidth>
		</colgroup>
		<thead is="coral-table-head">
			<tr is="coral-table-row">
			  <th is="coral-table-headercell">Title</th>
			  <th is="coral-table-headercell">Description</th>
			  <th is="coral-table-headercell">Edit</th>
			  <th is="coral-table-headercell">Delete</th>
			</tr>
		</thead>
		<tbody is="coral-table-body">
			<sling2:findResources query="SELECT * FROM [nt:base] AS s WHERE ISDESCENDANTNODE([/var/acs-commons/reports]) AND [jcr:primaryType]='cq:Page'" language="JCR-SQL2" var="reports" />
			<c:forEach var="report" items="${reports}">
				<tr is="coral-table-row">
					<sling2:getResource base="${report}" path="jcr:content" var="reportContent" />
					<td is="coral-table-cell">
						<a class="coral-Link" href="${report.path}.html?wcmmode=disabled">
							${sling2:encode(reportContent.valueMap['jcr:title'],'HTML')}
						</a>	
					</td>
					<td is="coral-table-cell">
						${sling2:encode(reportContent.valueMap['jcr:description'],'HTML')}
					</td>
					<td is="coral-table-cell">
						<button is="coral-button" icon="edit" iconsize="S" data-href="/editor.html${report.path}.html">
						</button>
					</td>
					<td is="coral-table-cell">
						<form action="${report.path}" method="post" class="coral-Form--aligned" id="fn-acsCommons-remove_${report.name}" ng-submit="postValues($event,'fn-acsCommons-remove_${report.name}')">
							<input type="hidden"  name=":operation" value="delete" />
							<div class="coral-Form-fieldwrapper">
								<button is="coral-button" icon="delete" iconsize="S"></button>
							</div>
						</form>
					</td>
				</tr>
			</c:forEach> 
		</tbody>
	</table>
</div>