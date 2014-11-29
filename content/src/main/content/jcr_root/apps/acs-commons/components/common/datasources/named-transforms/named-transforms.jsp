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

        public int compare(NamedImageTransformer a, NamedImageTransformer b) {
            String nameA = a.getTransformName().toLowerCase();
            String nameB = b.getTransformName().toLowerCase();

            return nameA.compareTo(nameB);
        }
    });

    // Create list of ValueMapResource's
    for (NamedImageTransformer transform : namedImageTransforms) {
        final String transformName = transform.getTransformName();
        final Map map = new HashMap();

        map.put("text", transformName);
        map.put("value", transformName);

        resourceList.add(new ValueMapResource(resourceResolver,
                new ResourceMetadata(),
                "",
                new ValueMapDecorator(map)));
    }

    DataSource ds = EmptyDataSource.instance();

    if (resourceList.size() > 0){
        // create a new datasource object
        ds = new SimpleDataSource(resourceList.iterator());
    }

    request.setAttribute(DataSource.class.getName(), ds);
%>
