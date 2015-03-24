<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false"
        import="com.adobe.acs.commons.images.NamedImageTransformer,
                com.adobe.acs.commons.wcm.datasources.DataSourceBuilder,
                com.adobe.acs.commons.wcm.datasources.DataSourceOption,
                java.util.ArrayList,

                java.util.List" %><%

    final DataSourceBuilder dataSourceBuilder = sling.getService(DataSourceBuilder.class);
    final NamedImageTransformer[] namedImageTransforms = sling.getServices(NamedImageTransformer.class, null);
    final List<DataSourceOption> options = new ArrayList<DataSourceOption>();

/*
    Arrays.sort(namedImageTransforms, new Comparator<NamedImageTransformer>() {
        public int compare(NamedImageTransformer a, NamedImageTransformer b) {
            String nameA = a.getTransformName().toLowerCase();
            String nameB = b.getTransformName().toLowerCase();

            return nameA.compareTo(nameB);
        }
    });
             */
    for (final NamedImageTransformer transform : namedImageTransforms) {
        options.add(new DataSourceOption(transform.getTransformName(),
                                         transform.getTransformName()));
    }

    dataSourceBuilder.addDataSource(slingRequest, options);
%>
