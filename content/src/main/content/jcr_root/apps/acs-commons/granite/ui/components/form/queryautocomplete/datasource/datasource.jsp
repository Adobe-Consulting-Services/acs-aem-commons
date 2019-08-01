<%@ page import="com.adobe.granite.ui.components.ds.DataSource" %>
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

  Query autocomplete datasoruce.

  A datasource that takes a list of predicates and a display property to build out the autocomplete list.
  The path of the resource is saved in the JCR and the display property is used for better UX.

  /**
   * The predicate map.
   */
  - predicates (String[]) = type=cq:Page,path=/content/geometrixx,property=jcr:content/sling:resourceType,property.value=geometrixx/components/homepage

   /**
   * The display property name.
   */
   -displayProperty (String) = jcr:content/jcr:title.

  ==============================================================================

--%><%@ page import="com.adobe.granite.ui.components.ds.SimpleDataSource" %>
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
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%@include file="/libs/foundation/global.jsp"%><%

    final ResourceResolver resolver = resourceResolver;
    final String path = resource.getPath();

    List<Resource> resultList = null;
    DataSource ds;

    if( resource != null ){
        ValueMap widgetProps = resource.getValueMap();

        final String displayProperty = widgetProps.get( "displayProperty", new String() );

        String[] predicates = widgetProps.get( "predicates", new String[0] );

        if( predicates.length > 0 ){
            Map<String, String> predicateMap = new HashMap<String, String>();
            for(String predicate : predicates){                
                String[] parts = predicate.split("=");
                if(parts != null && parts.length > 1){
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
                        ValueMap valueMap = item.getValueMap();

                        String value = item.getPath();
                        String text = value;

                        if( StringUtils.isNotEmpty( displayProperty ) && valueMap.containsKey( displayProperty )){
                            text = valueMap.get( displayProperty, value );
                        }

                        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());
                        vm.put("text", text);
                        vm.put("value", value);
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
