<%@include file="/libs/foundation/global.jsp" %><%
%><%@page session="false" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"
		 import="com.adobe.acs.commons.components.longformtext.LongFormTextComponent"%><%
%><%@taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %><%
%><%@taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %><%

    /* Services */
    final LongFormTextComponent longFormTextComponent = sling.getService(LongFormTextComponent.class);

    final String[] textParagraphs =
            longFormTextComponent.getTextParagraphs(properties.get("text", ""));

    int index = 0;

    pageContext.setAttribute("paragraphElement",
            xssAPI.encodeForHTMLAttr(component.getProperties().get("paragraphElement", String.class)));
    pageContext.setAttribute("paragraphCSS",
            xssAPI.encodeForHTMLAttr(component.getProperties().get("paragraphCSS", String.class)));

    pageContext.setAttribute("inlineParPrefix", LongFormTextComponent.LONG_FORM_TEXT_PAR);
    pageContext.setAttribute("inlinePars", resource.getChildren());
    pageContext.setAttribute("textParagraphs", textParagraphs);

    pageContext.setAttribute("text", properties.get("text", String.class));

%><wcmmode:edit>
    <%-- Updating underlying state on GET request, but ONLY on AEM Author (Edit Mode).
         Typically this is not done, but required to clean up removed paragraph resources --%>
    <% longFormTextComponent.mergeParagraphSystems(resource, textParagraphs.length); %>
</wcmmode:edit>
<c:choose>
    <c:when test="${!wcmmode:isEdit(pageContext) && empty text}">
        <%-- No text on Publish? Display nothing! --%>
    </c:when>
    <c:when test="${wcmmode:isEdit(pageContext) && empty text}">
        <wcm:placeholder classNames="cq-textlines-placeholder" />
    </c:when>
    <c:otherwise>
        <c:forEach var="text" items="${textParagraphs}" varStatus="paragraph">
            <c:set var="index" value="<%= ++index %>"/>
            <c:set var="includeParsys" value="<%= longFormTextComponent.hasContents(resource, index) %>"/>

            <c:choose>
                <c:when test="${!empty paragraphElement && !empty paragraphCSS}">
                    <${paragraphElement} class="${paragraphCSS}">${text}</${paragraphElement}>
                </c:when>
                <c:when test="${!empty paragraphElement}">
                    <${paragraphElement}>${text}</${paragraphElement}>
                </c:when>
                <c:otherwise>
                    ${text}
                </c:otherwise>
            </c:choose>

            <c:if test="${wcmmode:isEdit(pageContext) || includeParsys}">
                <cq:include path="${inlineParPrefix}${index}"
                            resourceType="${component.resourceType}/long-form-text-parsys"/>
            </c:if>
        </c:forEach>
    </c:otherwise>
</c:choose>