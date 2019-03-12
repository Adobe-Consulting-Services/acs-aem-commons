<%--
  #%L
  ACS AEM Commons Package
  %%
  Copyright (C) 2013 - 2014 Adobe
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
<%@include file="/libs/foundation/global.jsp"%><%
%><%@ page import="java.util.Arrays,java.util.List,org.apache.sling.xss.XSSAPI" %><%
%><%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %><%
%><%@ taglib prefix="xss" uri="http://www.adobe.com/consulting/acs-aem-commons/xss/2.0" %><%
%><%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %><%
%><%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %><%

    String[] tweets = properties.get("tweets", new String[0]);
    int limit = properties.get("limit", 0);
    List<String> tweetList = Arrays.asList(tweets);
    if (limit > 0 && limit < tweetList.size()) {
        tweetList = tweetList.subList(0, limit);
    }

    pageContext.setAttribute("tweets", tweetList);

    XSSAPI slingXssAPI = slingRequest.adaptTo(XSSAPI.class);
    pageContext.setAttribute("slingXssAPI", slingXssAPI);
%>
<c:choose>
    <c:when test="${empty properties.username}">
        <wcm:placeholder>Please provide a Twitter username.</wcm:placeholder>
    </c:when>
    <c:otherwise>
        <c:choose>
            <c:when test="${fn:length(tweets) gt 0}">
                <ul>
                <c:forEach var="tweet" items="${tweets}">
                    <li>${xss:filterHTML(slingXssAPI, tweet)}</li>
                </c:forEach>
                </ul>
            </c:when>
            <c:when test="${wcmmode:isEdit(pageContext)}">
                The Twitter timeline for user: '${xss:encodeForHTML(slingXssAPI, properties.username)}' hasn't been fetched yet.
            </c:when>
        </c:choose>
    </c:otherwise>
</c:choose>


