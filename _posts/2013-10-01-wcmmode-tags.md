---
layout: feature
title: WCMMode Tags & Functions
description: JSP Taglib for working with WCM Mode
date: 2013-10-01 23:39:29
thumbnail: /images/default/thumbnail.png
categories: features
initial-release: 1.0.0
---


## Purpose

Provide simple JSP custom tags and EL functions for determining the [WCMMode](http://dev.day.com/docs/en/cq/current/javadoc/com/day/cq/wcm/api/WCMMode.html).

## Usage

First, add the taglib declaration:

    <%@ taglib prefix="wcmmode" uri="http://www.adobe.com/consulting/acs-aem-commons/wcmmode" %>

### Tags

JSP tags available are:

* `<wcmmode:edit>`
* `<wcmmode:design>`
* `<wcmmode:preview>`
* `<wcmmode:disabled>`

In each case, the body of the tag will be evaluated if the current WCMMode matches the tag name, e.g.

{% highlight jsp %}
<wcmmode:edit>
This will be output in EDIT mode.
</wcmmode:edit>
{% endhighlight %}

Each tag also takes a `not` parameter which can be used to invert the logic, e.g.

{% highlight jsp %}
<wcmmode:edit not="true">
This will be output in everything except EDIT mode.
</wcmmode:edit>
{% endhighlight %}

### Functions

EL Functions available are:

* `wcmmode:isEdit(pageContext)`
* `wcmmode:isDesign(pageContext)`
* `wcmmode:isPreview(pageContext)`
* `wcmmode:isDisabled(pageContext)`

These would typically be used in a `<c:if>` tag, e.g.

{% highlight jsp %}
<c:if test="${blank properties['title'] && wcmmode:isEdit(pageContext)}">
    You probably want to populate the title in the dialog.
</c:if>
{% endhighlight %}