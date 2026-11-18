<%@include file="/libs/foundation/global.jsp"%><%
%><%@page session="false" import="
        com.adobe.granite.ui.components.PagingIterator,
        com.adobe.granite.ui.components.ds.DataSource,
                 org.apache.sling.api.wrappers.ValueMapDecorator,
         com.adobe.granite.ui.components.ds.AbstractDataSource,
        java.util.*,
        org.apache.sling.api.resource.*
            " %><%

    Resource hostsResource = resourceResolver.getResource("/etc/replication/agents.author");
	Resource suffixResource = slingRequest.getRequestPathInfo().getSuffixResource();
    List<Resource> agents = new ArrayList<>();
    if(hostsResource != null) for(Resource item : hostsResource.getChildren()){
        Resource jcrContent = item.getChild("jcr:content");
        if(jcrContent == null) continue;
        ValueMap props = jcrContent.getValueMap();
        ValueMap vm = new ValueMapDecorator(new HashMap<String, Object>(props));
        if(!props.get("enabled", false) || !"content-replicator".equals(props.get("provisioner"))) {
            continue;
        }
        vm.put("value", item.getName());
        vm.put("text", jcrContent.getValueMap().get("jcr:title", item.getName()));
        if(suffixResource != null && suffixResource.getPath().equals(item.getPath())) {
        	vm.put("selected", true);
        }

        agents.add(new ResourceWrapper(item){
           public String getResourceType() {
             return "acs-commons/components/utilities/replicator/agententry";
           }
           public ValueMap getValueMap() {
            return vm;
           }
        });
    }
    DataSource ds = new AbstractDataSource() {
        public Iterator<Resource> iterator() {
            return agents.iterator();
        }
    };

    request.setAttribute(DataSource.class.getName(), ds);
%>
