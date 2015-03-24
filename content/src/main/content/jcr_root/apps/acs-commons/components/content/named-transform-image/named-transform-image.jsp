<%@include file="/libs/foundation/global.jsp"%><%
%><%@ page session="false"
         import="com.day.cq.commons.Doctype,
                 com.day.cq.wcm.api.components.DropTarget,
                 com.day.cq.wcm.foundation.Image,
                 com.day.cq.wcm.foundation.Placeholder,
                 org.apache.commons.lang.StringUtils,
                 java.util.Calendar" %><%
%><%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %><%
%><%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %><%

    Image image = new Image(resource);
    final String transform = properties.get("transform", "");

    if (StringUtils.isBlank(transform) || !image.hasContent())  {
        image = null;
    } else {
        final Calendar calendar = image.getLastModified();
        final long timestamp = calendar.getTimeInMillis();

        image.setIsInUITouchMode(Placeholder.isAuthoringUIModeTouch(slingRequest));

        //set src url that is handled by the named transform image servlet
        image.setSrc(resource.getPath() + ".transform/" + transform + "/" + timestamp + "/image.png");

        //drop target css class = dd prefix + name of the drop target in the edit config
        image.addCssClass(DropTarget.CSS_CLASS_PREFIX + "image");
        image.loadStyleData(currentStyle);
        image.setSelector(".img"); // use image script
        image.setDoctype(Doctype.fromRequest(request));

        // add design information if not default (i.e. for reference paras)
        if (!currentDesign.equals(resourceDesign)) {
            image.setSuffix(currentDesign.getId());
        }
    }

    pageContext.setAttribute("image", image);

%><c:choose>
    <c:when test="${wcmmode:isEdit(pageContext) && empty image}">
        <wcm:placeholder classNames="cq-image-placeholder cq-block-placeholder" ddType="image" />
    </c:when>
    <c:when test="${!wcmmode:isEdit(pageContext) && empty image}">
        <%-- Component has not been configured on Publish; Hide the component --%>
    </c:when>
    <c:otherwise>
        <img src="${image.src}" alt="${image.alt}"/>
    </c:otherwise>
</c:choose>








