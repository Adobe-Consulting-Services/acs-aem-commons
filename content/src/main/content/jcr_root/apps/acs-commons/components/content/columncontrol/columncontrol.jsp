<%@include file="/libs/foundation/global.jsp" %>
<%@page session="false" %>
<%@ taglib prefix="mpf" uri="http://www.adobe.com/consulting/acs-aem-commons/mpf" %>
<%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %>
<c:set var="columns" value="${mpf:getMultiPanelFieldValues(resource, 'columns')}"/>
<c:choose>
    <c:when test="${empty columns}">
         <wcmmode:edit>You need to specify the columns.</wcmmode:edit>
    </c:when>
    <c:otherwise>
      <c:forEach items="${columns}" var="column" varStatus="status">
          <div class="acs-commons-resp-colctrl-col-${column.width}" >
              <c:choose>
               <c:when test="${empty column.path}">
                   <c:set var="path" value="${status.count}" />
               </c:when>
               <c:otherwise>
                   <c:set var="path" value="${column.path}" />
              </c:otherwise>
              </c:choose>
              <cq:include path="${path}" resourceType="foundation/components/parsys" />
          </div>
      </c:forEach>
     </c:otherwise>
</c:choose>



