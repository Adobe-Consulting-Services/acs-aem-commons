<%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false"
          import="com.adobe.granite.ui.components.Config,
                  com.adobe.granite.ui.components.rendercondition.RenderCondition,
                  com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition,
                  com.adobe.granite.ui.components.ExpressionHelper,
                  com.adobe.granite.ui.components.ExpressionResolver,
                  org.apache.sling.api.resource.ResourceResolver,
                  org.apache.sling.api.resource.Resource,
                  com.day.cq.replication.ReplicationStatus"%>
<%
    ResourceResolver resolver = resourceResolver;
    ExpressionHelper expressionHelper = new ExpressionHelper(sling.getService(ExpressionResolver.class), pageContext);
    boolean vote = false;
    if (resolver != null) {
        /*
         * Get the component helper and "resourcePath"
         * of the current granite:rendercondition resource. Use the
         * ExpressionHelper to evaluate the JSP Expression Language (EL)
         * expression (e.g. ${param.resourcePath} or ${requestPathInfo.suffix}).
         */
        Config cfg = new Config(resource);
        String resourcePath = expressionHelper.getString(cfg.get("resourcePath", String.class));
        if (resourcePath != null) {
            Resource res = resolver.getResource(resourcePath);
            if (res != null) {
                ReplicationStatus status = res.adaptTo(ReplicationStatus.class);
                if (status.isActivated()){
                    vote = true;
                }
            }
        }
    }  /* Display or hide the widget */
         request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(vote));

%>