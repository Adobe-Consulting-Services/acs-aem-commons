<%--
  ==============================================================================

  Base Form component

  Handles internal forwards for form based components

  ==============================================================================

--%><%@ include file="/libs/foundation/global.jsp" %><%
    final String path = currentPage.getPath() + ".html";
%><cq:include path="<%= path  %>" resourceType="cq/Page"/>