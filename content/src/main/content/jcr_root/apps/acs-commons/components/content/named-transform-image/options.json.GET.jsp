<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false"
        contentType="application/json; charset=UTF-8"
        pageEncoding="utf-8"%><%
 %><%@include file="/apps/acs-commons/components/common/datasources/named-transforms/named-transforms.jsp"%><%
    try {
        // Inherited from named-transforms.jsp
        dataSourceBuilder.writeDataSourceOptions(slingRequest, slingResponse);
    } catch(Exception e) {
        slingResponse.setStatus(500);
    }
%>