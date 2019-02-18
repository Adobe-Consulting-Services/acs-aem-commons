<%@include file="/libs/foundation/global.jsp"%>
<%@page session="false"
        import="java.lang.IllegalArgumentException,
          com.adobe.acs.commons.rendercondition.GraniteRenderConditionEvaluator,
          com.adobe.granite.ui.components.rendercondition.RenderCondition,
          com.adobe.granite.ui.components.rendercondition.SimpleRenderCondition"%>
<%--###

Template
======

.. granite:servercomponent:: /apps/acs-commons/granite/ui/components/renderconditions/service
   :rendercondition:

   A condition that renders granite widgets based on a regestered service

   It has the following content structure:

   .. gnd:gnd::

      [granite:RenderConditionsService]

      /**
       * The condition.name property of an OSGI service that implements com.adobe.acs.commons.rendercondition.GraniteRenderCondition
       */
      - condition.name (String)

      /**
       * The default fallback value, if anything fails (class loading, exceptions, or anything else).
       * true: shows the granite widget. false: hides it.
       */
      - default (Boolean:)
###--%>
<sling:defineObjects/>
<%
  // all logic is in GraniteRenderConditionEvaluator class.
  boolean vote = GraniteRenderConditionEvaluator.evaluate(slingRequest, request, sling, pageContext);
  request.setAttribute(RenderCondition.class.getName(), new SimpleRenderCondition(vote));
%>