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
<sling2:adaptTo var="tags" adaptable="${slingRequest}" adaptTo="com.adobe.acs.commons.reports.models.TagsCellValue" />
<td is="coral-table-cell">
	<ul>
		<c:forEach var="tag" items="${tags.tags}">
			<li>
				<a href="#" data-href="/libs/cq/tagging/gui/content/tags.html${tag.path}" target="_blank">
					${tag.title}
				</a>
			</li>
		</c:forEach>
	</ul>
</td>