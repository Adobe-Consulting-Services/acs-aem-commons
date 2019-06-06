<%@include file="/libs/foundation/global.jsp" %>
<%@page import="org.apache.sling.api.resource.*,
                java.util.*,
                javax.jcr.*,
                com.day.cq.search.*,
                com.day.cq.wcm.api.*,
                com.day.cq.dam.api.*" %>
<%@ page import="com.adobe.acs.commons.synth.children.ChildrenAsPropertyResource" %>
<%@ page import="org.apache.commons.lang3.StringUtils" %>
<%


    Resource real = resourceResolver.getResource("/content");
    ChildrenAsPropertyResource wrapper = new ChildrenAsPropertyResource(real);
    //Resource child = wrapper.create("child-1", "nt:unstructured");
    //ModifiableValueMap mvm = child.adaptTo(ModifiableValueMap.class);
    //mvm.put("prop-1", "some data");
    //mvm.put("prop-2", Calendar.getInstance());
    //wrapper.persist();
    //resourceResolver.commit();


%>Hello