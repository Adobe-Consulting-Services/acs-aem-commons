# Designer - ClientLibs Manager

## Purpose

Designs hold data that typically does not share the same scope;

* Design content; which is "per site"
* Front End Assets; which can be "cross-site"

Designer - ClientLibs Manager provides an interface and common abstraction for decoupling Front-end assets (Clientlibs) with Designs AND Page implementations.

### Use Cases

* Moving resource type implementations without modifying all content sling:resourceType properties
* Overlaying OOTB `/libs` behavior without creating overlay content trees under /apps not specific to your organization/project (ex. `/apps/foundation` or `/apps/cq`)


This Servlet Factory allows "Source resource types" to be mapped to a "Target resource type", effectively providing
an "in code" overlay of the "Source resource type".

## Implementation

* Override of the OOTB CQ Design Page implementation to allow for customization of "Head" and "Body" based ClientLibs.
    * Notes
        * Body Clientlibs only accept JavaScript as CSS should always be loaded in the Head.
        * Leverages the ACS Commons Overlay Servlet to provide an unobtrusive overlay ***(subject to change based on review)***
* DesignHtmlLibraryManager Service
    * Wraps the OOTB HtmlLibraryManager but is driven by the ClientLib configuration from the current design (first bullet point)

## Sling OSGi Configuration

To enable the CQ Design Page overlay, the following sling:osgiConfig must be added to the project.

    /apps/myapp/config.author/com.adobe.acs.util.impl.OverlayServletFactoryImpl-DesignerClientLibsManager.xml


    <?xml version="1.0" encoding="UTF-8"?>
    <jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
        jcr:primaryType="sling:osgiConfig">
        sling.servlet.resourceTypes="wcm/core/components/designer"
        sling.servlet.selectors=""
        sling.servlet.extensions="html"
        sling.servlet.methods="GET"
        prop.target-resource-type="acs-commons/components/utilities/designer"/>

## Example Use

	<%@page session="false" import="com.adobe.acs.commons.designer.*"%><%
        final DesignHtmlLibraryManager dhlm = sling.getService(DesignHtmlLibraryManager.class);
    %>

    <!DOCTYPE html>
    <html>
        <head>
            <title>Demo Page</title>
            ...

            dhlm.writeCssInclude(slingRequest, currentDesign, PageRegion.HEAD, out);
            dhlm.writeJsInclude(slingRequest, currentDesign, PageRegion.HEAD, out);
            dhlm.writeIncludes(slingRequest, currentDesign, PageRegion.HEAD, out);

            <!-- OR, manually pass the list to the OOTB cq:includeClientLib tag; Effectively the same thing -->

            <cq:includeClientLib css="<%= StringUtils.join(dhlm.getCssLibraries(currentDesign, PageRegion.HEAD), ',') %>"/>
            <cq:includeClientLib js="<%= StringUtils.join(dhlm.getJsLibraries(currentDesign, PageRegion.HEAD), ',') %>"/>
            <cq:includeClientLib categories="<%= StringUtils.join(dhlm.getLibraries(currentDesign, PageRegion.HEAD), ',') %>"/>
        </head>
        <body>
            <h1>Demo Page</h1>

            <p>CSS has no business being in the body of a document, and its almost always better to push JS load to the end of the body</p>

            dhlm.writeJsInclude(slingRequest, currentDesign, PageRegion.BODY, out);
            dhlm.writeIncludes(slingRequest, currentDesign, PageRegion.BODY, out);

            <!-- OR, manually pass the list to the OOTB cq:includeClientLib tag; Effectively the same thing -->

            <cq:includeClientLib js="<%= StringUtils.join(dhlm.getJsLibraries(currentDesign, PageRegion.BODY), ',') %>"/>
            <cq:includeClientLib categories="<%= StringUtils.join(dhlm.getLibraries(currentDesign, PageRegion.BODY), ',') %>"/>
        </body>
    </html>

