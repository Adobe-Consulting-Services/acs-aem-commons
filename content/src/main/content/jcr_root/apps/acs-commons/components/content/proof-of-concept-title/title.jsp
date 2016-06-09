<%@page session="false"%><%--
  Copyright 1997-2008 Day Management AG
  Barfuesserplatz 6, 4001 Basel, Switzerland
  All Rights Reserved.

  This software is the confidential and proprietary information of
  Day Management AG, ("Confidential Information"). You shall not
  disclose such Confidential Information and shall use it only in
  accordance with the terms of the license agreement you entered into
  with Day.

  ==============================================================================

  Title component.

  Draws a title either store on the resource or from the page

--%><%@include file="/libs/foundation/global.jsp"%><%
%><%@ page import="java.util.Iterator,
        org.apache.commons.lang3.StringEscapeUtils,
        com.day.cq.commons.Doctype,
        com.day.cq.commons.DiffInfo,
        com.day.cq.commons.DiffService,
        org.apache.sling.api.resource.Resource,
        org.apache.sling.api.resource.ResourceUtil" %><%

    // first calculate the correct title - look for our sources if not set in paragraph
    String title = properties.get(NameConstants.PN_TITLE, String.class);
    if (title == null || title.equals("")) {
        title = resourcePage.getPageTitle();
    }
    if (title == null || title.equals("")) {
        title = resourcePage.getTitle();
    }
    if (title == null || title.equals("")) {
        title = resourcePage.getName();
    }

    // escape title
    title = xssAPI.filterHTML(title);

    // check if we need to compute a diff
    String vLabel = request.getParameter(DiffService.REQUEST_PARAM_DIFF_TO);
    String diffOutput = null;
    DiffInfo diffInfo = resource.adaptTo(DiffInfo.class);
    if (diffInfo != null) {
        DiffService diffService = sling.getService(DiffService.class);
        ValueMap map = ResourceUtil.getValueMap(diffInfo.getContent());
        String diffText = map.get(NameConstants.PN_TITLE, "");
        // if the paragraph has no own title, we use the current page title(!)
        if (diffText == null || diffText.equals("")) {
            diffText = title;
        } else {
            diffText = xssAPI.filterHTML(diffText);
        }
        diffOutput = diffInfo.getDiffOutput(diffService, title, diffText, false);
        if (title.equals(diffOutput)) {
            diffOutput = null;
        }
    } else if (vLabel != null) {
        // handle the case when title component is not inside parsys
        Resource vRes = DiffInfo.getVersionedResource(resource, vLabel);
        if (vRes == null && resource.getParent() != null) {
            vRes = DiffInfo.getVersionedResource(resource.getParent(), vLabel);
        }        
        if (vRes != null) {
            //check if versioned resource has an overriding direct title component
            for (Iterator<Resource> iter = resourceResolver.listChildren(vRes); iter.hasNext();) {
                Resource res = iter.next();
                if (res.isResourceType("foundation/components/title")) {
                    vRes = res;
                    break;
                }
            }
            DiffService diffService = sling.getService(DiffService.class);
            ValueMap map = ResourceUtil.getValueMap(vRes);
            String diffText = map.get(NameConstants.PN_TITLE, "");
            diffOutput = diffService.diff(title, diffText, false);
        }
    }
    String defType = currentStyle.get("defaultType", "large");

    // use image title if type is "small" but not if diff should be displayed
    if (properties.get("type", defType).equals("small") && diffOutput == null) {
        String suffix = currentDesign.equals(resourceDesign) ? "" : "/" + currentDesign.getId();
        // add mod timestamp to avoid client-side caching of updated images
        long tstamp = properties.get("jcr:lastModified", properties.get("jcr:created", System.currentTimeMillis()));
        suffix += "/" + tstamp + ".png";
        String xs = Doctype.isXHTML(request) ? "/" : "";
        %><img src="<%= xssAPI.getValidHref(resource.getPath()+".title.png"+suffix) %>" alt="<%= xssAPI.encodeForHTMLAttr(title) %>"<%=xs%>><%

    // large title
    } else if (diffOutput == null) {
        %><h1>POC: <%= title %></h1><%

    // we need to display the diff output
    } else {
        // don't escape diff output
        %><h1>POC: <%= diffOutput %></h1><%

    }

%>
