<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2022 Adobe
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
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:choose>
    <c:when test="${fn:startsWith(properties.property, '/')}">
        <c:set var="property" value="${fn:substringAfter(properties.property, '/')}"/>
    </c:when>
    <c:otherwise>
        <c:set var="property" value="${properties.property}"/>
    </c:otherwise>
</c:choose>

