<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false"
        contentType="application/json; charset=UTF-8"
        pageEncoding="utf-8" %><%
 %><%@include file="/apps/acs-commons/datasources/named-image-transforms/named-image-transforms.jsp"%><%
    try {
        // Inherited from named-image-transforms.jsp
        new DataSourceBuilder().writeDataSourceOptions(slingRequest, slingResponse);
    } catch(Exception e) {
        slingResponse.setStatus(500);
    }
%>