---
layout: feature
title: Delegating Servlet
description: Clean-up those one-off overlays
date: 2013-10-01 23:39:29
thumbnail: /images/delegating-servlet/thumbnail.png
categories: features
---

## Purpose

Avoid polluting the `/apps` namespace for one-off overlays, and to enable deployment of multiple overlay options while enabling the system to select the implementation to use. 


## Use Cases

* Moving resource type implementations without modifying all content sling:resourceType properties
* Overlaying OOTB `/libs` behavior without creating overlay content trees under /apps not specific to your organization/project (ex. `/apps/foundation` or `/apps/cq`)


This Servlet Factory allows "Source resource types" to be mapped to a "Target resource type", effectively providing
an "in code" overlay of the "Source resource type".

## How to Use

To create a delegation mapping, create a new sling:osgiConfig 

    /apps/myapp/config.author/com.adobe.acs.util.impl.DelegatingServletFactoryImpl-SomeFriendlyName.xml

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="sling:osgiConfig"
    sling.servlet.resourceTypes="resource/type/to/delegate"
    sling.servlet.selectors="optionally.scope.with.selectors"
    sling.servlet.extensions="optionally.scope.with.extension.usually.html"
    sling.servlet.methods="optionally.scope.w.method.usually.GET"
    prop.target-resource-type="resource/type/to/delegate/to"/>
{% endhighlight %}        

## Example

The ACS AEM Commons Designer - ClientLibs implementation uses the Delegatin Servlet to delegate requests from resource type `wcm/core/components/designer` to `acs-commons/components/utilities/designer`

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0" xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="sling:osgiConfig"
    sling.servlet.resourceTypes="wcm/core/components/designer"
    sling.servlet.selectors=""
    sling.servlet.extensions="html"
    sling.servlet.methods="GET"
    prop.target-resource-type="acs-commons/components/utilities/designer"/>
{% endhighlight %}        