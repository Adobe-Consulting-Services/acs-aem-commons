<%@page session="false"%><%@page import="org.apache.sling.api.resource.Resource,
                org.apache.sling.api.resource.ValueMap,
                org.apache.sling.api.resource.ResourceUtil,
                com.day.cq.wcm.webservicesupport.Configuration,
                com.day.cq.wcm.webservicesupport.ConfigurationManager" %>
<%@include file="/libs/foundation/global.jsp" %><%

String[] services = pageProperties.getInherited("cq:cloudserviceconfigs", new String[]{});
ConfigurationManager cfgMgr = (ConfigurationManager)sling.getService(ConfigurationManager.class);
if(cfgMgr != null) {
    String header = null;
    Configuration cfg = cfgMgr.getConfiguration("dtm", services);
    if(cfg != null) {
        header = cfg.get("header", null);
    }

    if(header != null) {
    %>
    <script type="text/javascript" src="<%=header%>">
    </script><%
    }
}
%>
