<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 Adobe
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
<%@include file="/libs/foundation/global.jsp"%>
<%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %>
<%@ taglib prefix="widgets" uri="http://www.adobe.com/consulting/acs-aem-commons/widgets" %>
<%@ taglib prefix="xss" uri="http://www.adobe.com/consulting/acs-aem-commons/xss" %>
<c:set var="images" value="${widgets:getImagesFromImageMultiField(resource, 'images')}"/>
<cq:includeClientLib categories="acs-commons.components"/>
<c:choose>
    <c:when test="${empty images}">
        <wcmmode:edit>
            Add images using component dialog
        </wcmmode:edit>
    </c:when>
    <c:otherwise>
        <%-- first wrapper div is for sizing purposes --%>
        <div style="height: ${xss:getValidInteger(xssAPI, properties.height, 350)}px; width: ${xss:getValidInteger(xssAPI, properties.width, 600)}px;">
            <div class="acs-commons-simple-slideshow">
                <c:forEach var="image" varStatus="status" items="${images}">
                    <img src="${image.src}" class="${status.first ? 'active' : ''}" alt="${image.alt}" height="${xss:getValidInteger(xssAPI, properties.height, 350)}" width="${xss:getValidInteger(xssAPI, properties.width, 600)}"/>
                </c:forEach>
            </div>
        </div>
    </c:otherwise>
</c:choose>