<%@page session="false"%>
<%@include file="/libs/foundation/global.jsp"%>
<%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %>

<wcm:defineObjects />

<h1>Proof of concept</h1>
<h2>Instance Title: ${properties.titleText}</h2>
<h2>Instance Quantity: ${properties.quantity}</h2>
<br><br>
<h3>Shared Title: ${sharedProperties.titleText}</h3>
<h3>Shared Page Ref: ${sharedProperties.referencedPage}</h3>
<br><br>
<h4>Global Title: ${globalProperties.titleText}</h4>
<br><br>
<h5>Merged Title: ${mergedProperties.titleText}</h5>
<h5>Merged Quantity: ${mergedProperties.quantity}</h5>
<h5>Merged Page Ref: ${mergedProperties.referencedPage}</h5>
