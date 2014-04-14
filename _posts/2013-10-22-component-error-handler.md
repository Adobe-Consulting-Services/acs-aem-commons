---
layout: feature
title: Component Error Handler
description: Don't let erring Components ruin your day
date: 2013-10-23 23:39:29
thumbnail: /images/component-error-handler/thumbnail.png
categories: features
initial-release: 1.2.0
---

## Purpose

Gracefully handle erring components with custom views. Edit, Preview and Publish modes can all have custom HTML snippets to display when a Component throws an error.

## How to Use

Create a new `sling:OsgiConfig` to define how each view should be handled. To leverage the ACS AEM Commons OOTB views use the `sling:OsgiConfig` node defined below. This configuration will be used globally across all sites and pages on the AEM instance. 

    /apps/myapp/config/com.adobe.acs.commons.wcm.impl.ComponentErrorHandlerImpl

{% highlight xml %}
<?xml version="1.0" encoding="UTF-8"?>
<jcr:root xmlns:sling="http://sling.apache.org/jcr/sling/1.0" xmlns:cq="http://www.day.com/jcr/cq/1.0"
    xmlns:jcr="http://www.jcp.org/jcr/1.0" xmlns:nt="http://www.jcp.org/jcr/nt/1.0"
    jcr:primaryType="sling:OsgiConfig"
    prop.edit.enabled="{Boolean}true"
    prop.edit.html="/apps/acs-commons/components/utilities/component-error-handler/edit.html"
    prop.preview.enabled="{Boolean}false"
    prop.preview.html="/apps/acs-commons/components/utilities/component-error-handler/preview.html"
    prop.publish.enabled="{Boolean}false"
    prop.publish.html="/dev/null"
	/>
{% endhighlight %}  

You can also create HTML snippets pointed to by the `prop.*.html` OSGi Config properties to fully custom experience. 

* Different views can be configured to point to the same HTML file
* CSS can be added inline to the HTML files to provide a particular aesthetic
* To hide erring component set the path to "/dev/null" or ""

## Suppression (Since 1.5.0)

As of version 1.5.0, there are two mechanisms for suppressing the component error handler:

* A list of resource types can be set using the `suppress-resource-types` OSGi property.
* The request attribute `com.adobe.acs.commons.wcm.component-error-handler.suppress` can be set to the boolean `true`.
	* Request attribute suppression prevents component error handling in two ways
		1. Errors occurring within the context of the include which sets the suppression request attribute will be supressess (allowing a component to suppress itself).
		2. Errors occuring in any include after the suppression request attribute is set, UNTIL the suppression request attribute it removed/set to false, will be suppressed.
