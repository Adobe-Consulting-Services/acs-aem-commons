<%
%><%@include file="/libs/granite/ui/global.jsp" %><%
%><%@page session="false"
          import="com.adobe.granite.ui.components.rendercondition.RenderCondition,
           com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition,
           com.adobe.granite.ui.components.ExpressionHelper,
           org.apache.sling.api.resource.ResourceResolver,
           com.adobe.granite.ui.components.ExpressionResolver,
           org.apache.sling.api.resource.Resource,
           com.adobe.granite.ui.components.Config,
           org.apache.sling.featureflags.Features"%>
<%
    ResourceResolver resolver = resourceResolver;
    Features features = sling.getService(org.apache.sling.featureflags.Features.class);
    ExpressionHelper expressionHelper = new ExpressionHelper(sling.getService(ExpressionResolver.class), pageContext);

    boolean vote = false;
    if (resolver != null) {

         Config cfg = new Config(resource);
         String featureFlag = expressionHelper.getString(cfg.get("featureFlag", String.class));

         if (features.isEnabled(featureFlag)) {
              vote = true;
         } else {
              vote = false;
         }
     }

      /* Display or hide the widget */
      request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(vote));
%>

