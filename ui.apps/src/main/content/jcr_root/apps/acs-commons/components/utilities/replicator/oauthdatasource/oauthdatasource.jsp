<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" import="
        com.adobe.granite.ui.components.ds.DataSource,
        org.apache.sling.api.wrappers.ValueMapDecorator,
        com.adobe.granite.ui.components.ds.ValueMapResource,
        com.adobe.granite.ui.components.ds.SimpleDataSource,
        org.osgi.service.cm.Configuration,
        org.osgi.service.cm.ConfigurationAdmin,
        java.util.*,
        org.apache.sling.api.resource.*
            " %><%

    ConfigurationAdmin configurationAdmin = sling.getService(ConfigurationAdmin.class);
    String configurationPid = "com.adobe.granite.auth.oauth.accesstoken.provider";
    Configuration[] configurations = configurationAdmin.listConfigurations("(service.factoryPid="+configurationPid+")");


    List<Resource> lst = new ArrayList<Resource>();
    if(configurations != null) for (Configuration cfg : configurations) {
        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>());

        Dictionary<String, Object> props = cfg.getProcessedProperties(null);
        String name = (String)props.get("name");
        vm.put("value", name);
        vm.put("text", name);

        lst.add(new ValueMapResource(resourceResolver, new ResourceMetadata(), "nt:unstructured", vm));
    }
    request.setAttribute(DataSource.class.getName(), new SimpleDataSource(lst.iterator()));
%>
