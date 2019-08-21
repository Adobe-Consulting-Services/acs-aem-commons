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
<td is="coral-table-cell">
	<c:choose>
		<c:when test="${properties.editor == 'custom'}">
			<c:set var="editor" value="${properties.customEditor}" />
		</c:when>
		<c:otherwise>
			<c:set var="editor" value="${properties.editor}" />
		</c:otherwise>
	</c:choose>
	<c:choose>
		<c:when test="${properties.useResourceType}">
			<c:set var="path" value="${fn:replace(fn:substring(result.path, 6, fn:length(result.path)),':','%3A')}" />
		</c:when>
		<c:otherwise>
			<c:set var="path" value="${fn:replace(result.path,':','%3A')}" />
		</c:otherwise>
	</c:choose>
	<a target="_blank" data-href="${editor}${path}" class="coral-Button coral-Button--square">
		<i class="coral-Icon coral-Icon--gear coral-Icon--sizeS"></i>
	</a>
</td>

