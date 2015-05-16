<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false"
        import="com.adobe.acs.commons.images.NamedImageTransformer,
                com.adobe.granite.ui.components.ds.DataSource,
                com.adobe.granite.ui.components.ds.EmptyDataSource,
                com.adobe.granite.ui.components.ds.SimpleDataSource,
                com.adobe.granite.ui.components.ds.ValueMapResource,
                org.apache.commons.lang.StringUtils,
                org.apache.sling.api.SlingHttpServletRequest,
                org.apache.sling.api.SlingHttpServletResponse,
                org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ResourceMetadata,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.wrappers.ValueMapDecorator,
                org.apache.sling.commons.json.JSONArray,
                org.apache.sling.commons.json.JSONException,
                org.apache.sling.commons.json.JSONObject,
                java.io.IOException,
                java.util.ArrayList,
                java.util.HashMap,
                java.util.Iterator,
                java.util.List,
                java.util.Map" %><%

    final NamedImageTransformer[] namedImageTransforms = sling.getServices(NamedImageTransformer.class, null);

    final DataSourceBuilder dataSourceBuilder = new DataSourceBuilder();
    final List<DataSourceOption> options = new ArrayList<DataSourceOption>();

    for (final NamedImageTransformer transform : namedImageTransforms) {
        options.add(new DataSourceOption(StringUtils.capitalize(
                                    StringUtils.replace(transform.getTransformName(), "-", " ")),
                                     transform.getTransformName()));
    }

    dataSourceBuilder.addDataSource(slingRequest, options);

%><%!

/** Inlined into JSP to facilitate AEM 5.6.1 Support **/

/** To be moved into Bundle as proper API in ACS AEM Commons 2.0.0 release **/

private class DataSourceOption {

    private String text;

    private String value;

    public DataSourceOption(final String text, final String value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public void setText(final String text) {
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
    }
}

%><%!

/** Inlined into JSP to facilitate AEM 5.6.1 Support **/

/** To be moved into Bundle as proper API in ACS AEM Commons 2.0.0 release **/

private class DataSourceBuilder {

    public void addDataSource(final SlingHttpServletRequest slingRequest, final List<DataSourceOption> options) {

        final ArrayList<Resource> resourceList = new ArrayList<Resource>();

        DataSource dataSource = null;

        for (final DataSourceOption option : options) {

            final Map map = new HashMap();

            map.put("text", option.getText());
            map.put("value", option.getValue());

            resourceList.add(new ValueMapResource(slingRequest.getResourceResolver(),
                    new ResourceMetadata(),
                    "",
                    new ValueMapDecorator(map)));
        }

        if (resourceList.size() > 0){
            dataSource = new SimpleDataSource(resourceList.iterator());
        } else {
            dataSource = EmptyDataSource.instance();
        }

        slingRequest.setAttribute(DataSource.class.getName(), dataSource);
    }

     public void writeDataSourceOptions(final SlingHttpServletRequest slingRequest,
                                       final SlingHttpServletResponse slingResponse) throws JSONException, IOException {
        final DataSource datasource = (DataSource) slingRequest.getAttribute(DataSource.class.getName());
        final JSONArray jsonArray = new JSONArray();

        if (datasource != null) {
            final Iterator<Resource> iterator = datasource.iterator();

            if (iterator != null) {
                while (iterator.hasNext()) {
                    final Resource dataResource = iterator.next();

                    if (dataResource != null) {
                        final ValueMap dataProps = dataResource.adaptTo(ValueMap.class);

                        if (dataProps != null) {
                            final JSONObject json = new JSONObject();

                            json.put("text", dataProps.get("text", ""));
                            json.put("value", dataProps.get("value", ""));

                            jsonArray.put(json);
                        }
                    }
                }
            }
        }

        slingResponse.getWriter().write(jsonArray.toString());
    }
}
%>
