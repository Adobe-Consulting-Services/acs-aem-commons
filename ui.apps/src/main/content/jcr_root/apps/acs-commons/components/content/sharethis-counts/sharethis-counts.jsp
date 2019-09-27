<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2014 Adobe
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
<%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %><%
%><%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %>
<c:set var="services" value="${properties.services}"/>
<c:set var="publisherId" value="${requestScope['com.adobe.acs.commons.sharethis.publisherId']}"/>
<c:choose>
    <c:when test="${empty services}">
        <wcm:placeholder>Select one or more services from the dialog.</wcm:placeholder>
    </c:when>
    <c:when test="${empty publisherId}">
        <wcmmode:edit>This component depends upon the ShareThis Cloud Service.</wcmmode:edit>
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${properties.style == 'vertical'}">
                <c:set var="classPostfix">vcount</c:set>
            </c:when>
            <c:otherwise>
                <c:set var="classPostfix">hcount</c:set>
            </c:otherwise>
        </c:choose>
        <c:forEach items="${services}" var="service">
            <span class="st_${service}_${classPostfix}"></span>
        </c:forEach>
    </c:otherwise>
</c:choose>
