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
<c:set var="columns" value="${properties.columns}"/>
<c:choose>
    <c:when test="${empty columns}">
        <wcm:placeholder>You need to specify the column widths.</wcm:placeholder>
    </c:when>
    <c:otherwise>
        <c:forEach items="${columns}" var="column" varStatus="status">
            <div class="acs-commons-resp-colctrl-col acs-commons-resp-colctrl-col-${column}" >
                  <cq:include path="par${status.count}" resourceType="foundation/components/parsys" />
            </div>
        </c:forEach>
    </c:otherwise>
</c:choose>
