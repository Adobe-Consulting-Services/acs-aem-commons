<%@include file="/libs/foundation/global.jsp" %>
<cq:includeClientLib categories="acs-commons.editablegrid.widgets"/>
<h1><c:out value="${currentPage.title }" /></h1>
<script>
var gridcolumns =[];
<c:set var="columns" value="${properties.gridcolumns}"/>
<c:choose>
<c:when test="${empty columns}">
   
</c:when>
<c:otherwise>

    <c:forEach items="${columns}" var="column" varStatus="status" >
    gridcolumns.push('<c:out value="${column}" />');
    </c:forEach>
   
</c:otherwise>
</c:choose>
CQ.Ext.onReady(function(){
  var viewport =   new ACS.CQ.grid.ViewportPanel({
       grid:new ACS.CQ.grid.EditorGridPanel({
          url:"<c:out value="${resource.path}" />.store.json",
          deleteurl:"<c:out value="${resource.path}" />.store.delete.json",
          updateurl:"<c:out value="${resource.path}" />.store.update.json",
          basePath:"<c:out value="${resource.path}" />.store.json"
          <c:choose>
          <c:when test="${empty columns}">
          </c:when>
          <c:otherwise>
          ,gridcolumns:gridcolumns
          </c:otherwise>
          </c:choose>
       })    
   });
   });
</script>
