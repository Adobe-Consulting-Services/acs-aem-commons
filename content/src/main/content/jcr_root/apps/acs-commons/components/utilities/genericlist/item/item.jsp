<%@include file="/libs/foundation/global.jsp"%>

<li >
	<%
String title = properties.get("jcr:title", "");
String value = properties.get("value", "");

if (title.equals("")) {
    %><span style="color: red;">Please enter a title</span>
	<%
} else { %>
Title: <%=title%> <br />
Value: <%=value%> <br />
<% } %>
</li>
