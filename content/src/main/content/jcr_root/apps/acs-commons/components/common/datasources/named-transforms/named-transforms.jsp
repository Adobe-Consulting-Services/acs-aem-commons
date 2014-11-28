<%@page session="false"
        import="java.util.ArrayList,
                java.util.Arrays,
                java.util.HashMap,
                java.util.Map,
                java.util.Comparator,
                com.adobe.acs.commons.images.NamedImageTransformer,
                com.adobe.granite.ui.components.ds.DataSource,
                com.adobe.granite.ui.components.ds.EmptyDataSource,
                com.adobe.granite.ui.components.ds.SimpleDataSource,
                com.adobe.granite.ui.components.ds.ValueMapResource,
                org.apache.sling.api.wrappers.ValueMapDecorator,
                org.apache.sling.api.resource.ResourceMetadata,
                org.apache.sling.api.wrappers.ValueMapDecorator,
                org.apache.sling.api.resource.Resource" %><%
%><%@include file="/libs/foundation/global.jsp"%><%

    ArrayList<Resource> resourceList = new ArrayList<Resource>();

    // read list of image transforms
    NamedImageTransformer[] namedImageTransforms = sling.getServices(NamedImageTransformer.class, null);

    // sort alphabetically by name
    Arrays.sort(namedImageTransforms, new Comparator<NamedImageTransformer>() {

        @Override
        public int compare(NamedImageTransformer left, NamedImageTransformer right) {
            String nameA = left.getTransformName().toLowerCase();
            String nameB = left.getTransformName().toLowerCase();

            return nameA.compareTo(nameB);
        }
    });

    // Create list of ValueMapResource's
    for (NamedImageTransformer transform : namedImageTransforms) {
        String transformName = transform.getTransformName();
        Map map = new HashMap();
        map.put("text", transformName);
        map.put("value", transformName);

        resourceList.add(new ValueMapResource(resource.getResourceResolver(), new ResourceMetadata(), "",
                new ValueMapDecorator(map)));
    }

    DataSource ds;
    // if no matching nodes where found
    if (resourceList.size() == 0){
        // return empty datasource
        ds = EmptyDataSource.instance();
    } else {
        // create a new datasource object
        ds = new SimpleDataSource(resourceList.iterator());
    }

    request.setAttribute(DataSource.class.getName(), ds);
%>
