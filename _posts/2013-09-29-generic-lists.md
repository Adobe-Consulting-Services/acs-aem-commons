---
layout: feature
title: Generic Lists
description: Easy creation of simple name/value pair lists.
date: 2013-10-01 23:39:29
thumbnail: /images/generic-lists/thumbnail.png
categories: features
---

Generic Lists are a feature allowing easy creation and management of lists of title/value pairs.

## Content

Generic Lists are represented as CQ Pages under `/etc/acs-commons/lists`, e.g. `/etc/acs-commons/lists/age-ranges`. They are editable by using the Tools screen:

![image](images/generic-lists/in-miscadmin.png)

On a list page, the items in the list are components within a parsys. New items can be created using the Sidekick. Items can be removed using the context menu.

![image](images/generic-lists/editor.png)

## Use in Dialogs

One of the primary purposes of Generic Lists is to populate a selection widget in a component (or page) dialog. To do this, set the `options` configuration property to the list path *plus* `/jcr:content.list.json`. For example:

{% highlight xml %}
    <jcr:root xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
        jcr:primaryType="cq:Widget"
        fieldLabel="Target"
        name="./target"
        options="/etc/acs-commons/lists/age-ranges/_jcr_content.list.json"
        type="select"
        xtype="selection"/>
{% endhighlight %}

This can also be used in multifield scenarios.

In both cases, the JCR property will be set to the *value* of the list item.

## API

You will frequently need to do two things with lists:

1. Get all the items in the list.
2. Lookup the title of a particular value from the list.

To do this, first obtain the `com.day.cq.wcm.api.Page` object for the list page:

{% highlight java %}
    PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
    Page listPage = pageManager.getPage("/etc/acs-commons/lists/targets");
{% endhighlight %}
    
Then adapt the `Page` object to a `com.adobe.acs.fordmedia.lists.GenericList` object:

{% highlight java %}
    GenericList list = listPage.adaptTo(GenericList.class);
{% endhighlight %}

The `GenericList` interface has two methods:

* `getItems()` - returns a `java.util.List` of the items in the list.
* `lookupTitle(String)` - return the title of a particular value from the list.

