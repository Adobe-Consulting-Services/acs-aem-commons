---
layout: feature
title: Component Placeholders
description: A little polish goes a long way
date: 2014-01-10 23:39:29
thumbnail: /images/placeholders/thumbnail.png
categories: features
initial-release: 1.5.0
tags: new
---

## Purpose

ACS AEM Commons adds some helper CSS classes for creating nice component placeholders for the AEM Classic UI:

### Extra Icons

* `cq-ad-placeholder` - Money, Advertisements
* `cq-audio-placeholder` - Audio
* `cq-ul-placeholder` - List
* `cq-dl-placeholder` - Definitions

### Sizes

* `cq-block-placeholder`: 192px height
* `cq-block-sm-placeholder`: 96px height
* `cq-block-lg-placehodler`: 384px height

All are 100% width.

Note that sizes can be used with the standard placeholder classes as well:

* `cq-gadget-placeholder`
* `cq-chart-placeholder`
* `cq-image-placeholder`
* `cq-video-placeholder`
* `cq-text-placeholder`
* `cq-title-placeholder`
* `cq-file-placeholder`
* `cq-feedlink-placeholder`
* `cq-list-placeholder`
* `cq-carousel-placeholder`
* `cq-reference-placeholder`
* `cq-flash-placeholder`
* `cq-teaser-placeholder`
* `cq-table-placeholder`

## How To Use

### Classic UI only (5.6)

    <wcmmode:edit>
      <img class="cq-dl-placeholder cq-block-placeholder" src="/etc/designs/default/0.gif"/>
    </wcmmode:edit>

### Classic UI &amp; TouchUI (5.6.1+)

    <wcmmode:edit>
      <%=
         Placeholder.getDefaultPlaceholder(slingRequest, component,
          "<img class='cq-dl-placeholder cq-block-placeholder' src='/etc/designs/default/0.gif'/>"
          )%>
       %>
    </wcmmode:edit>


## Sample

{% highlight html %}
<img class="cq-audio-placeholder cq-block-sm-placeholder" src="/etc/designs/default/0.gif"/>
<img class="cq-ad-placeholder cq-block-placeholder" src="/etc/designs/default/0.gif"/>
<img class="cq-carousel-placeholder cq-block-lg-placeholder" src="/etc/designs/default/0.gif"/>
{% endhighlight %}

Yields the following ..

![Sample Placeholderes]({{ site.baseurl }}/images/placeholders/sample.png)


## Notes

**Drag and Drop** can be easily added to these placeholders via the extra CSS class `cq-dd-<drop-target name>` where `drop-target name` matches the `cq:EditConfig` dropTarget node name.

{% highlight html %}
<img class="cq-audio-placeholder cq-block-sm-placeholder cq-dd-audio" src="/etc/designs/default/0.gif"/>
<img class="cq-image-placeholder cq-block-lg-placeholder cq-dd-image" src="/etc/designs/default/0.gif"/>
{% endhighlight %}



