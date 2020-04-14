<%@include file="/libs/foundation/global.jsp" %>
<%@page session="false"%>
<%@page import="com.day.cq.wcm.api.PageManager,
        com.day.cq.wcm.api.Page,
        java.util.Locale,
        com.adobe.acs.commons.genericlists.GenericList" %>
<%
    String selectedTz = (String)request.getAttribute("selectedTz");
    String nameTz = (String)request.getAttribute("nameTz");

    Locale locale = request.getLocale();
    pageManager = resourceResolver.adaptTo(PageManager.class);
    Page listPage = pageManager.getPage("/etc/acs-commons/lists/datepicker-timezones");

    GenericList list = listPage.adaptTo(GenericList.class);
%>
<coral-autocomplete class="datepickertz" name="<%=nameTz%>tz" placeholder="Choose Timezone" style="width:100%">
    <%for (GenericList.Item item : list.getItems()) { %>
    <coral-autocomplete-item value="<%=item.getValue()%>" <%out.print(item.getValue().equals(selectedTz) ? "selected" : "");%>>
        <%=item.getTitle(locale)%>
    </coral-autocomplete-item>
    <%}%>
</coral-autocomplete>