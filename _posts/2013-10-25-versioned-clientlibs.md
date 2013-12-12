---
layout: feature
title: Versioned ClientLibs
description: Set TTLs on ClientLib JS/CSS to infinity and beyond!
date: 2013-10-01 23:39:29
thumbnail: /images/versioned-clientlibs/thumbnail.png
categories: features
tags: new
initial-release: 1.2.0
---

## Purpose

Allow CSS and JavaScript served via AEM ClientLibs to be cached client-side with long TTLs.

## How to Use

Add a Sling rewriter configuration node (example below) that includes the `versioned-clientlibs` transformer type to you project. 

The URIs to clientlibs will be rewritten in the format `/path/to/clientlib.last-modified-timestamp.js`

### Note

This re-writer does **NOT** support

* URIs embedded in CSS or JavaScript, including: background-images, web fonts, etc. 
* Relative URIs, e.g. `etc/clientlibs/mysite/styles.css`
* URIs including a scheme, e.g. `http://example.com/etc/clientlibs/mysite/styles.css` and `//example.com/etc/clientlibs/mysite/styles.css`
* URIs to non-AEM HtmlClientLibrary resources, e.g. `/etc/designs/mysite.css`
 
## Rewriter Configuration Node

	/apps/myapp/config/rewriter/versioned-clientlibs.xml

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0"
    jcr:primaryType="sling:Folder"
    contentTypes="[text/html]"
    enabled="{Boolean}true"
    generatorType="htmlparser"
    order="{Long}1"
    serializerType="htmlwriter"
    transformerTypes="[linkchecker,versioned-clientlibs]"/>
{% endhighlight %}        
