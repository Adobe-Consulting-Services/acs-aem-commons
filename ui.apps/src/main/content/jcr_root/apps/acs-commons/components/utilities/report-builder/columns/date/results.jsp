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
<sling2:getResource path="${result.path}" var="resultRsrc" />
<td is="coral-table-cell" value="${result.valueMap[properties.property].time.time}">        
	<c:choose>
		<c:when test="${not empty properties.format}">
			<fmt:formatDate value="${result.valueMap[properties.property].time}" pattern="${properties.format}" />
		</c:when>
		<c:otherwise>
			<fmt:formatDate value="${result.valueMap[properties.property].time}" dateStyle="medium" timeStyle="medium" />
		</c:otherwise>
	</c:choose>
</td>
