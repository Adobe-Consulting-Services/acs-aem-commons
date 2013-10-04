---
layout: feature
title: Client Libs Manager
description: Associate Client Libs with Designs
date: 2013-10-01 23:39:29
thumbnail: /images/designer-clientlibsmanager/thumbnail.png
categories: features
---

## Purpose

Provide an author-able means for defining, creating and managing custom Error pages per content tree/site.

## Expected Behavior

### Edit/Design mode

#### 40x Handling
Author is displayed the corresponding Error page

#### 50x Handling
Normal/OOTB 500x/Exception error handling behavior is respected. JSP exceptions are displayed inline w the JSP.

### Preview mode

#### 40x Handling
Author is displayed the corresponding Error page

#### 50x Handling
A custom "Error page" is displayed that includes the Request Progress and Stack Trace.

### Disabled (Publish) mode

The corresponding Error page is displayed.

## Instructions

1. Create the proxy overlays for Sling errorhandler scripts (404.jsp and default.jsp) which include the acs-commons counterparts.

 * /apps/sling/servlet/errorhandler/404.jsp

	    <%@page session="false"%><%
 	    %><%@include file="/apps/acs-commons/components/utilities/errorpagehandler/404.jsp" %>

 * /apps/sling/servlet/errorhandler/default.jsp

	    <%@page session="false"%><%
 	    %><%@include file="/apps/acs-commons/components/utilities/errorpagehandler/default.jsp" %>

2. In your base page implementation, add the following `cq:Widget` to the Page Properties dialog

    &lt;errorpages
        jcr:primaryType="cq:Widget"
        path="./apps/acs-commons/components/utilities/errorpagehandler/dialog/errorpages"
        xtype="cqinclude"/>

    - OR create a your own custom pathfield widget -

    &lt;errorpages
        jcr:primaryType="cq:Widget"
        fieldLabel="Error Pages"
        fieldDescription="Error pages for this content tree"
        name="./errorPages"
        xtype="pathfield"/>

3. Create a CQ Page that will act as the default Error page, and also contain all custom variations of error pages.
Each error page's "name" (Node name) should correspond to the HTTP Response Status code it should respond to.
    * 500: Internal Server Error
    * 404: Not Found
    * 403: Forbidden
Typically only 404 and 500 are needed with everything else using the fallback (default error page) as the messaging around these tends to be less useful to Web site visitors.
A common pattern is to create this at the site's root under a node named "errors"
    * Ex. /content/geometrixx/en/us/errors
4. Create any error-specific pages under this default error page created in Step 2.
Note, it is critical that the page NAMES (node names) follow status codes. The Page Titles can be anything.
    * Ex. /content/geometrixx/en/us/errors/404
    * Ex. /content/geometrixx/en/us/errors/500
5. Edit the Page Properties of the site's root node, and in the new "Error Pages" dialog input (Step 1) select the default error page (Step 2).
    * Ex. ./errorPages => /content/geometrixx/en/us/errors
6. Further customizations can be made via the OSGi Configuration for the `ACS AEM Commons - Error Page Handler` Configuration, including a "System wide" fallback error page.

*** Note: *** At this time the full Sling exception-name look-up scheme is not supported. Implementing a `500` error page is sufficient.


