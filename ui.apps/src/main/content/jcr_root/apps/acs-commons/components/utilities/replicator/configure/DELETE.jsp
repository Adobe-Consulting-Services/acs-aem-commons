<%@include file="/libs/foundation/global.jsp" %><%


    String path = slingRequest.getParameter("path");
    Resource res = resourceResolver.getResource(path);
    if(res != null) {
        resourceResolver.delete(res);
        resourceResolver.commit();
        out.println(path);
    }

%>