<%@include file="/libs/foundation/global.jsp"%><%
%><%@ page session="false"
         import="com.day.cq.commons.Doctype,
                 com.day.cq.wcm.api.components.DropTarget,
                 com.day.cq.wcm.foundation.Image,
                 com.day.cq.wcm.foundation.Placeholder,
                 org.apache.commons.lang.StringUtils,
                 org.apache.sling.xss.XSSAPI" %><%
%><%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %><%
%><%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %><%
%><%

    XSSAPI slingXssAPI = sling.getService(XSSAPI.class);

    Image image = new Image(resource);

    final String transform = properties.get("transform", String.class);
    final String linkURL = properties.get("linkURL", String.class);

    if (image.hasContent()) {

        if (StringUtils.isNotBlank(transform)) {
            final long imageTimestamp = image.getLastModified().getTimeInMillis();
            final long pageTimestamp = currentPage.getLastModified().getTimeInMillis();
            final long timestamp = imageTimestamp > pageTimestamp ? imageTimestamp : pageTimestamp;

            image.setSrc(resource.getPath() + ".transform/" + transform + "/" + timestamp + "/image." + image.getExtension());
        }

        image.setIsInUITouchMode(Placeholder.isAuthoringUIModeTouch(slingRequest));
        image.addCssClass(DropTarget.CSS_CLASS_PREFIX + "image");
        image.loadStyleData(currentStyle);
        image.setSelector(".img"); // use image script
        image.setDoctype(Doctype.fromRequest(request));

        if (StringUtils.isNotBlank(properties.get("alt", String.class))) {
            image.setAlt(properties.get("alt", String.class));
        }

    } else {
        image = null;
    }

    
    linkURL = slingXssAPI.getValidHref(linkURL);
    pageContext.setAttribute("linkURL", linkURL);

    imageSrc = image.getSrc();
    imageSrc = slingXssAPI.getValidHref(imageSrc);
    pageContext.setAttribute("imageSrc", imageSrc);

    imageAlt = image.getAlt();
    imageAlt = slingXssAPI.getValidHref(imageAlt);
    pageContext.setAttribute("imageAlt", imageAlt);


%><c:choose>
    <c:when test="${wcmmode:isEdit(pageContext) && empty image}">
        <wcm:placeholder classNames="cq-image-placeholder cq-block-placeholder" ddType="image"/>
    </c:when>
    <c:when test="${!wcmmode:isEdit(pageContext) && empty image}">
        <%-- Component has not been configured on Publish; Hide the component --%>
    </c:when>
    <c:when test="${not empty linkURL}">
        <a href="${linkURL}"><img
                src="${imageSrc}"
                class="cq-dd-image"
                alt="${imageAlt}"/></a>
    </c:when>
    <c:otherwise>
        <img src="${imageSrc}"
             class="cq-dd-image"
             alt="${imageAlt}"/>
    </c:otherwise>
</c:choose>
