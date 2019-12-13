<%--
  ADOBE CONFIDENTIAL

  Copyright 2015 Adobe Systems Incorporated
  All Rights Reserved.

  NOTICE:  All information contained herein is, and remains
  the property of Adobe Systems Incorporated and its suppliers,
  if any.  The intellectual and technical concepts contained
  herein are proprietary to Adobe Systems Incorporated and its
  suppliers and may be covered by U.S. and Foreign Patents,
  patents in process, and are protected by trade secret or copyright law.
  Dissemination of this information or reproduction of this material
  is strictly forbidden unless prior written permission is obtained
  from Adobe Systems Incorporated.
--%><%
%><%@include file="/libs/granite/ui/global.jsp"%><%
%><%@page session="false"%><%
%><%@page import="java.util.ArrayList,
          java.util.Calendar,
          java.util.Collection,
          java.util.List,
          javax.jcr.security.Privilege,
          javax.jcr.RepositoryException,
          javax.jcr.Session,
          javax.jcr.security.AccessControlManager,
          org.apache.commons.lang.StringUtils,
          org.apache.jackrabbit.util.Text,
          org.apache.sling.api.resource.ResourceResolver,
          com.adobe.granite.ui.components.AttrBuilder,
          com.adobe.granite.ui.components.Tag,
          com.day.cq.wcm.api.Page,
          com.day.cq.wcm.api.PageManager" %><%

AccessControlManager acm = null;
try {
acm = resourceResolver.adaptTo(Session.class).getAccessControlManager();
} catch (RepositoryException e) {
log.error("Unable to get access manager", e);
}

Page cqPage = resource.adaptTo(Page.class);

String title;
String thumbnailURL = null;
boolean isFolder = false;

String actionRels = StringUtils.join(getActionRels(resource, cqPage, acm), " ");

if (cqPage != null) {
title = cqPage.getTitle() == null ? cqPage.getName() : cqPage.getTitle();
thumbnailURL = getThumbnailUrl(cqPage, 48, 48);
} else {
ValueMap vm = resource.getValueMap();
title = vm.get("jcr:content/jcr:title", vm.get("jcr:title", resource.getName()));
    
isFolder = true;
}

Tag tag = cmp.consumeTag();
AttrBuilder attrs = tag.getAttrs();

attrs.addBoolean("hidden", true);
attrs.addClass("foundation-collection-meta");
attrs.add("data-foundation-collection-meta-title", title + "!");
attrs.add("data-foundation-collection-meta-folder", isFolder);
attrs.add("data-foundation-collection-meta-rel", actionRels);

AttrBuilder imgAttrs = new AttrBuilder(request, xssAPI);
imgAttrs.addClass("foundation-collection-meta-thumbnail");
imgAttrs.addHref("src", thumbnailURL);

%><div <%= attrs %>>
    <img <%= imgAttrs %>>
</div><%!

private String getThumbnailUrl(Page page, int width, int height) {
    String ck = "";
    
    ValueMap metadata = page.getProperties("image/file/jcr:content");
    if (metadata != null) {
        Calendar cal = metadata.get("jcr:lastModified", Calendar.class);
        if (cal != null) {
            ck = "" + (cal.getTimeInMillis() / 1000);
        }
    }

    return Text.escapePath(page.getPath()) + ".thumb." + width + "." + height + ".png?ck=" + ck;
}

private List<String> getActionRels(Resource resource, Page page, AccessControlManager acm) {
    List<String> actionRels = new ArrayList<String>();
    // actions rels [data-foundation-collection-meta-rel], are not respected if they are empty.
    actionRels.add("dummy");
    
    if (hasPermission(acm, resource, Privilege.JCR_ADD_CHILD_NODES)) {
        actionRels.add("cq-siteadmin-admin-pastepage");
        actionRels.add("cq-siteadmin-admin-createlist");        
        if (page == null) {
            actionRels.add("cq-siteadmin-admin-createfolder");
        }
    }
    
    return actionRels;
}

private boolean hasPermission(AccessControlManager acm, String path, String privilege) {
    if (acm != null) {
        try {
            Privilege p = acm.privilegeFromName(privilege);
            return acm.hasPrivileges(path, new Privilege[]{p});
        } catch (RepositoryException ignore) {
        }
    }
    return false;
}

private boolean hasPermission(AccessControlManager acm, Resource resource, String privilege) {
    return hasPermission(acm, resource.getPath(), privilege);
}
%>