<%@page session="false" import="
                  org.apache.sling.api.resource.Resource, 
                  org.apache.sling.api.resource.ResourceUtil, 
                  org.apache.sling.api.resource.ValueMap,
                  org.apache.sling.api.resource.ResourceResolver,
                  org.apache.sling.api.resource.ResourceMetadata,
                  org.apache.sling.api.wrappers.ValueMapDecorator,
                  org.apache.commons.lang3.StringUtils,
                  javax.jcr.Node,
                  javax.jcr.NodeIterator,
                  javax.jcr.Session,
                  javax.jcr.query.Query,
                  javax.jcr.query.QueryManager,
                  javax.jcr.query.QueryResult,
                  java.util.*,
                  com.adobe.granite.ui.components.ds.DataSource,
                  com.adobe.granite.ui.components.ds.EmptyDataSource,
                  com.adobe.granite.ui.components.ds.SimpleDataSource,
                  com.adobe.granite.ui.components.ds.ValueMapResource"%><%
%><%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %><%
%><cq:defineObjects/><%
  
    // set fallback
    request.setAttribute(DataSource.class.getName(), EmptyDataSource.instance());  
    ResourceResolver resolver = resource.getResourceResolver(); 
    //Create an ArrayList to hold data
    List<Resource> fakeResourceList = new ArrayList<Resource>(); 
    ValueMap vm = null; 	
    String query = properties.get("dropdownquery","");	
    QueryManager queryManager = resolver.adaptTo(Session.class).getWorkspace().getQueryManager();
    Query jcrQuery = queryManager.createQuery(query, javax.jcr.query.Query.JCR_SQL2);
    QueryResult queryResult = jcrQuery.execute();
    NodeIterator nodeIterator = queryResult.getNodes();
    StringBuilder selectOptionsHTML = new StringBuilder();
    List<String> selectQueryColumns = parseQuery(jcrQuery.getStatement());
    List<String> distinctOptionValues=new ArrayList();
        while (nodeIterator.hasNext()) {
           Node nextNode = nodeIterator.nextNode();
           String nextNodePath = nextNode.getPath();           
           Resource componentResource = resourceResolver.resolve(nextNodePath);
           ValueMap valueMap = componentResource.getValueMap();
           Set<Map.Entry<String, Object>> entrySet = valueMap.entrySet();
           Iterator<Map.Entry<String, Object>> entryIterator = entrySet.iterator();
             while (entryIterator.hasNext()) {
                Map.Entry<String, Object> next = entryIterator.next();
                vm = new ValueMapDecorator(new HashMap<String, Object>());   
                String key = next.getKey();
                Object value = next.getValue();
                  if (value instanceof String && (selectQueryColumns.size() == 0 || selectQueryColumns.contains(key.toLowerCase()))) {
                    //Either it has to be a select * query or the query parameter has to contain the current key
                    //We are doing this only string and string array
                     if(distinctOptionValues.contains(value.toString())==false){
                                vm.put("value",value.toString());
                                vm.put("text",value.toString());
                                distinctOptionValues.add(value.toString());
                                fakeResourceList.add(new ValueMapResource(resolver, new ResourceMetadata(), "nt:unstructured", vm));
                            }
                        } 
                    }//End of while parsing the property           
            } 
    //Create a DataSource that is used to populate the drop-down control
    DataSource ds = new SimpleDataSource(fakeResourceList.iterator());
    request.setAttribute(DataSource.class.getName(), ds);
%>
<%! 
    //Mehtod for selecting the column from query
   public List<String> parseQuery(String jcrQueryString) { 
    	List<String> selectQueryColumns=new ArrayList<>();
   		String querySelectParameters = jcrQueryString.toLowerCase();
        querySelectParameters = StringUtils.substringAfter(querySelectParameters, "select");
        querySelectParameters = StringUtils.substringBefore(querySelectParameters, " from");
        querySelectParameters = querySelectParameters.replaceAll("\\[", "").replaceAll("\\]", "");
        querySelectParameters=querySelectParameters.replaceAll("distinct","");
        querySelectParameters = querySelectParameters.trim().toLowerCase();
        querySelectParameters = querySelectParameters.replaceAll("\r","").replaceAll("\n","");

        String[] arr=querySelectParameters.split(",");
            for(int i=0;i<arr.length;i++){
                if(arr[i].indexOf(".")>=0){
                    arr[i]=StringUtils.substringAfter(arr[i],".");
                }
                selectQueryColumns.add(arr[i].toLowerCase().trim());
            }
    return selectQueryColumns;
   } 
%>