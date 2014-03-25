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
