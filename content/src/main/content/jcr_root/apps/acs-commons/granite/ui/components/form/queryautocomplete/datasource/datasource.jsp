<%@ page import="com.adobe.granite.ui.components.ds.DataSource" %>
<%@ page import="com.adobe.granite.ui.components.ds.SimpleDataSource" %>
<%@ page import="com.adobe.granite.ui.components.ds.ValueMapResource" %>
<%@ page import="com.day.cq.search.PredicateGroup" %>
<%@ page import="com.day.cq.search.Query" %>
<%@ page import="com.day.cq.search.QueryBuilder" %>
<%@ page import="com.day.cq.search.result.SearchResult" %>
<%@ page import="org.apache.commons.collections.IteratorUtils" %>
<%@ page import="org.apache.commons.collections.Transformer" %>
<%@ page import="org.apache.commons.collections.iterators.TransformIterator" %>
<%@ page import="org.apache.sling.api.resource.ResourceResolver" %>
<%@ page import="org.apache.sling.api.wrappers.ValueMapDecorator" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@include file="/libs/foundation/global.jsp"%><%

    final ResourceResolver resolver = resourceResolver;
    final String path = resource.getPath();

    List<Resource> resultList = null;
    DataSource ds;

    if( resource != null ){
        ValueMap widgetProps = resource.getValueMap();

        String[] predicates = widgetProps.get( "predicates", new String[0] );

        if( predicates.length > 0 ){
            Map<String, String> predicateMap = new HashMap<String, String>();
            for(String predicate : predicates){
                String[] parts = predicate.split("=");
                if(parts != null && parts.length >0){
                    predicateMap.put(parts[0],parts[1]);
                }
            }

            QueryBuilder queryBuilder = sling.getService( QueryBuilder.class );
            Session session = resourceResolver.adaptTo( Session.class );
            Query query = queryBuilder.createQuery( PredicateGroup.create(predicateMap), session );

            SearchResult result = query.getResult();

            if( result != null ){
                resultList = IteratorUtils.toList(result.getResources());
            }

        }

        if( resultList != null ){

            ds = new SimpleDataSource(new TransformIterator(resultList.iterator(), new Transformer() {
                public Object transform(Object input) {
                    try {
                        Resource item = (Resource) input;
                        String dataValue = item.getPath();
                        String text = dataValue;

                        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
                        vm.put("text", text);
                        vm.put("value", dataValue);
                        vm.put("canDelete", true);

                        return new ValueMapResource(resolver, path, "nt:unstructured", vm);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }));

            request.setAttribute(DataSource.class.getName(), ds);
        }
    }
%>
