<%@page session="false"
        import="java.util.Iterator,
                com.adobe.granite.ui.components.ds.DataSource,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap" %><%
%><%@include file="/libs/foundation/global.jsp"%><%
%><%@include file="/apps/acs-commons/components/common/datasources/named-transforms/named-transforms.jsp"%><%
%>[<%

    DataSource datasource = (DataSource) request.getAttribute(DataSource.class.getName());
    Iterator<Resource> iterator = datasource.iterator();
    boolean hasNext = iterator != null && iterator.hasNext();

    while (hasNext) {
        ValueMap map = iterator.next().adaptTo(ValueMap.class);

        %>{"title":"<%= map.get("text", "")%>", "value":"<%= map.get("value", "")%>"}<%

        hasNext = iterator.hasNext();
        if (hasNext) {
            %>,<% // yo, this is the most ugly way I could of think to build a JSON array :)
        }
    }
%>
]
