---
layout: feature
title: XSS JSP Functions
description: JSP Taglib for XSS Protection
date: 2013-12-11 23:39:29
thumbnail: /images/xssfunctions/thumbnail.png
categories: features
initial-release: 1.3.0
tags: new
---


## Purpose

Provide simple JSP EL functions for XSS protection using the [XSSAPI](http://dev.day.com/docs/en/cq/current/javadoc/com/adobe/granite/xss/XSSAPI.html) service provided by AEM.

## Usage

First, add the taglib declaration:

    <%@ taglib prefix="xss" uri="http://www.adobe.com/consulting/acs-aem-commons/xss" %>

### Functions

* `xss:encodeForHTML(XSSAPI, String)`
* `xss:encodeForHTMLAttr(XSSAPI, String)`
* `xss:encodeForJSString(XSSAPI, String)`
* `xss:filterHTML(XSSAPI, String)`
* `xss:getValidDimension(XSSAPI, String, String)`
* `xss:getValidHref(XSSAPI, String)`
* `xss:getValidInteger(XSSAPI, String, int)`
* `xss:getValidJSToken(XSSAPI, String, String)`


See JavaDoc of [XSSAPI](http://dev.day.com/docs/en/cq/current/javadoc/com/adobe/granite/xss/XSSAPI.html) for more details. Also see the [XSS Cheat Sheet](https://dev.day.com/content/docs/en/cq/current/developing/securitychecklist/_jcr_content/par/download/file.res/xss_cheat_sheet.pdf).

### Example

    <%@include file="/libs/foundation/global.jsp"%><%
    %><%@ taglib prefix="xss" uri="http://www.adobe.com/consulting/acs-aem-commons/xss" %>
    ${xss:encodeForHTMLAttr(xssAPI, 'hi"')}
