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
<sling2:adaptTo adaptable="${result}" var="refModel" adaptTo="com.adobe.acs.commons.reports.models.ReferencesModel" />
<td is="coral-table-cell" value="${fn:length(refModel.references)}">
	<strong>Count: ${fn:length(refModel.references)}</strong>
	<coral-drawer>
		<ul>
			<c:forEach var="reference" items="${refModel.references}">
				<li>
					Type: ${reference.type} - <a title="${reference.target.path}" href="#" data-href="/crx/de/index.jsp#${reference.target.path}" target="_blank">Reference</a>					
				</li>
			</c:forEach>
		</ul>
	</coral-drawer>
</td>
