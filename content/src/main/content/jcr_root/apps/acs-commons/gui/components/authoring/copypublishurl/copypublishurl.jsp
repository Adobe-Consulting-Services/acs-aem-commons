<%@page import="com.day.cq.i18n.I18n,
					com.day.cq.commons.Externalizer,org.apache.sling.api.SlingHttpServletRequest"%>
<%@taglib prefix="cq" uri="http://www.day.com/taglibs/cq/1.0" %><%%>

<cq:defineObjects />

<%
    Externalizer externalizer = resourceResolver.adaptTo(Externalizer.class);
    String url = externalizer.externalLink(resourceResolver, Externalizer.PUBLISH, request.getScheme(), ((SlingHttpServletRequest)request).getRequestPathInfo().getSuffix());

%>
<%= url %>
