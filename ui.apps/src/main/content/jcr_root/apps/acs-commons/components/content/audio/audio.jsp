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
  --%><%--
  ==============================================================================

  HTML5 audio component

  ==============================================================================

--%><%@ include file="/libs/foundation/global.jsp" %><%
%><%@ page import="com.day.cq.wcm.api.WCMMode,
                   com.day.cq.wcm.api.components.DropTarget"%><%
%><%@taglib prefix="audio" uri="http://www.adobe.com/consulting/acs-aem-commons/audio" %><%
%><%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %><%
%><cq:include script="partials/init.jsp"/><%
%><c:choose>
    <c:when test="${!empty audio_asset}">
        <c:choose>
            <c:when test="${fn:length(renditions) == fn:length(profiles)}">
<audio id="${id}"${attributes}>
    <c:forEach var="entry" items="${renditions}">
        <source src="${request.contextPath}${audio:getHtmlSource(entry.value, entry.key)}" type="${entry.key.htmlType}"/>
    </c:forEach>
        <cq:include script="partials/fallback.jsp"/>
</audio>
<cq:include script="partials/analytics.jsp"/>
            </c:when>
            <c:otherwise>
                <cq:include script="partials/fallback.jsp"/>
            </c:otherwise>
        </c:choose>
    </c:when>
    <c:otherwise>
        <wcm:placeholder classNames="cq-audio-placeholder cq-block-placeholder" ddType="audio" />
    </c:otherwise>
</c:choose>
