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
<sling2:adaptTo var="cell" adaptable="${slingRequest}" adaptTo="com.adobe.acs.commons.reports.models.ReportCellValue" />
<td is="coral-table-cell" value="${sling2:encode(val,'HTML_ATTR')}">
	<c:choose>
		<c:when test="${cell.array}">
			<ul>
				<c:forEach var="val" items="${cell.value}">
					<li>
						${val}
					</li>
				</c:forEach>
			</ul>
		</c:when>
		<c:otherwise>
			${cell.value}
		</c:otherwise>
	</c:choose>
</td>