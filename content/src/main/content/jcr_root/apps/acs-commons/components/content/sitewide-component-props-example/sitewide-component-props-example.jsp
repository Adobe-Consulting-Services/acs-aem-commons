<%@page session="false"%>
<%@include file="/libs/foundation/global.jsp"%>
<%@ taglib prefix="wcm" uri="http://www.adobe.com/consulting/acs-aem-commons/wcm" %>

<wcm:defineObjects />

<h1>Proof of concept</h1>
<h2>Instance Title: ${properties.text}</h2>
<h2>Instance Quantity: ${properties.quantity}</h2>
<br><br>
<h3>Site-wide Title: ${sharedComponentProperties.text}</h3>
<h3>Site-wide Page Ref: ${sharedComponentProperties.referencedPage}</h3>
<br><br>
<h4>Merged Title: ${mergedProperties.text}</h4>
<h4>Merged Quantity: ${mergedProperties.quantity}</h4>
<h4>Merged Page Ref: ${mergedProperties.referencedPage}</h4>

  

